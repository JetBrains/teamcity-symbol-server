package jetbrains.buildServer.symbols;

import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.artifacts.BuildArtifact;
import jetbrains.buildServer.serverSide.artifacts.BuildArtifacts;
import jetbrains.buildServer.serverSide.artifacts.BuildArtifactsViewMode;
import jetbrains.buildServer.serverSide.impl.LogUtil;
import jetbrains.buildServer.serverSide.metadata.BuildMetadataProvider;
import jetbrains.buildServer.serverSide.metadata.MetadataStorageWriter;
import org.apache.log4j.Logger;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Evgeniy.Koshkin
 */
public class BuildSymbolsIndexProvider implements BuildMetadataProvider {

  public static final String PROVIDER_ID = "symbols-index-provider";
  public static final String SIGNATURE_KEY = "sign";
  public static final String FILE_NAME_KEY = "file-name";
  public static final String ARTIFACT_PATH_KEY = "artifact-path";

  private static final Logger LOG = Logger.getLogger(BuildSymbolsIndexProvider.class);
  private final SymbolsCache mySymbolsCache;

  public BuildSymbolsIndexProvider(@NotNull final SymbolsCache symbolsCache) {
    mySymbolsCache = symbolsCache;
  }

  @NotNull
  public static String getMetadataKey(@NotNull String signature, @NotNull String fileName){
    return signature + ":" + fileName;
  }

  @NotNull
  public String getProviderId() {
    return PROVIDER_ID;
  }

  public void generateMedatadata(@NotNull SBuild sBuild, @NotNull MetadataStorageWriter metadataStorageWriter) {
    int numSymbolFiles = 0;
    final BuildArtifact symbols = sBuild.getArtifacts(BuildArtifactsViewMode.VIEW_HIDDEN_ONLY).getArtifact(".teamcity/symbols");
    final long buildId = sBuild.getBuildId();
    if(symbols != null){
      for (BuildArtifact symbolSignaturesSource : symbols.getChildren()){
        if (!symbolSignaturesSource.getName().startsWith(SymbolsConstants.SYMBOL_SIGNATURES_FILE_NAME_PREFIX) &&
                !symbolSignaturesSource.getName().startsWith(SymbolsConstants.BINARY_SIGNATURES_FILE_NAME_PREFIX))
          continue;

        Set<PdbSignatureIndexEntry> indexEntries = Collections.emptySet();
        try {
          indexEntries = PdbSignatureIndexUtil.read(symbolSignaturesSource.getInputStream(), false);
        } catch (IOException e) {
          LOG.debug("Failed to read symbols index data from artifact " + symbolSignaturesSource.getRelativePath(), e);
        } catch (JDOMException e) {
          LOG.debug("Failed to read symbols index data from artifact " + symbolSignaturesSource.getRelativePath(), e);
        }

        LOG.debug(String.format("Build with id %d provides %d symbol file signatures.", buildId, indexEntries.size()));

        for (final PdbSignatureIndexEntry indexEntry : indexEntries) {
          final String signature = indexEntry.getGuid();
          final String fileName = indexEntry.getFileName();
          String artifactPath = indexEntry.getArtifactPath();
          if(artifactPath == null){
            LOG.debug(String.format("Artifact path is not provided for artifact %s, locating it by name in build %s artifacts.", fileName, LogUtil.describe(sBuild)));
            artifactPath = locateArtifact(sBuild, fileName);
            if(artifactPath != null){
              LOG.debug(String.format("Located artifact by name %s, path - %s. Build - %s", fileName, artifactPath, LogUtil.describe(sBuild)));
            }
          }

          LOG.info(String.format(
            "Indexing symbol file %s with signature %s of build %s", fileName, signature, LogUtil.describe(sBuild)
          ));
          final HashMap<String, String> data = new HashMap<>();
          data.put(SIGNATURE_KEY, signature);
          data.put(FILE_NAME_KEY, fileName);
          data.put(ARTIFACT_PATH_KEY, artifactPath);

          final String metadataKey = getMetadataKey(signature, fileName);
          metadataStorageWriter.addParameters(metadataKey, data);
          mySymbolsCache.removeEntry(metadataKey);
        }
        ++numSymbolFiles;
      }
    }
    if(numSymbolFiles == 0) {
      LOG.debug("Build with id " + buildId + " doesn't provide symbols index data.");
    }
  }

  @Nullable
  private String locateArtifact(@NotNull SBuild build, final @NotNull String artifactName) {
    final AtomicReference<String> locatedArtifactPath = new AtomicReference<String>(null);
    build.getArtifacts(BuildArtifactsViewMode.VIEW_ALL_WITH_ARCHIVES_CONTENT).iterateArtifacts(artifact -> {
      if(artifact.getName().equals(artifactName)){
        locatedArtifactPath.set(artifact.getRelativePath());
        return BuildArtifacts.BuildArtifactsProcessor.Continuation.BREAK;
      }
      else return BuildArtifacts.BuildArtifactsProcessor.Continuation.CONTINUE;
    });
    return locatedArtifactPath.get();
  }
}
