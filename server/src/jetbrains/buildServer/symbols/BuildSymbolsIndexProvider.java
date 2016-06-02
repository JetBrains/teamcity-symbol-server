package jetbrains.buildServer.symbols;

import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.artifacts.BuildArtifact;
import jetbrains.buildServer.serverSide.artifacts.BuildArtifacts;
import jetbrains.buildServer.serverSide.artifacts.BuildArtifactsViewMode;
import jetbrains.buildServer.serverSide.impl.LogUtil;
import jetbrains.buildServer.serverSide.metadata.BuildMetadataProvider;
import jetbrains.buildServer.serverSide.metadata.MetadataStorageWriter;
import jetbrains.buildServer.util.CollectionsUtil;
import jetbrains.buildServer.util.filters.Filter;
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
  public static final String FILE_NAME_KEY = "file-name";
  public static final String ARTIFACT_PATH_KEY = "artifact-path";

  private static final Logger LOG = Logger.getLogger(BuildSymbolsIndexProvider.class);

  @NotNull
  public String getProviderId() {
    return PROVIDER_ID;
  }

  public void generateMedatadata(@NotNull SBuild sBuild, @NotNull MetadataStorageWriter metadataStorageWriter) {
    final long buildId = sBuild.getBuildId();

    final BuildArtifact symbols = sBuild.getArtifacts(BuildArtifactsViewMode.VIEW_HIDDEN_ONLY).getArtifact(".teamcity/symbols");
    final BuildArtifact symbolSignaturesSource = symbols == null ? null : CollectionsUtil.findFirst(symbols.getChildren(), new Filter<BuildArtifact>() {
      public boolean accept(@NotNull BuildArtifact data) {
        return data.getName().startsWith(SymbolsConstants.SYMBOL_SIGNATURES_FILE_NAME_PREFIX);
      }
    });
    if(symbolSignaturesSource == null) {
      LOG.debug("Build with id " + buildId + " doesn't provide symbols index data.");
      return;
    }

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
      final HashMap<String, String> data = new HashMap<String, String>();
      data.put(FILE_NAME_KEY, fileName);
      data.put(ARTIFACT_PATH_KEY, artifactPath);
      metadataStorageWriter.addParameters(signature, data);
      LOG.debug("Stored symbol file signature " + signature + " for file name " + fileName + " build id " + buildId);
    }
  }

  @Nullable
  private String locateArtifact(@NotNull SBuild build, final @NotNull String artifactName) {
    final AtomicReference<String> locatedArtifactPath = new AtomicReference<String>(null);
    build.getArtifacts(BuildArtifactsViewMode.VIEW_DEFAULT_WITH_ARCHIVES_CONTENT).iterateArtifacts(new BuildArtifacts.BuildArtifactsProcessor() {
      @NotNull
      public Continuation processBuildArtifact(@NotNull BuildArtifact artifact) {
        if(artifact.getName().equals(artifactName)){
          locatedArtifactPath.set(artifact.getRelativePath());
          return Continuation.BREAK;
        }
        else return Continuation.CONTINUE;
      }
    });
    return locatedArtifactPath.get();
  }
}
