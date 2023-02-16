/*
 * Copyright 2000-2023 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jetbrains.buildServer.symbols;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.artifacts.BuildArtifact;
import jetbrains.buildServer.serverSide.artifacts.BuildArtifacts;
import jetbrains.buildServer.serverSide.artifacts.BuildArtifactsViewMode;
import jetbrains.buildServer.serverSide.impl.LogUtil;
import jetbrains.buildServer.serverSide.metadata.BuildMetadataProvider;
import jetbrains.buildServer.serverSide.metadata.MetadataStorageWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
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

  private static final Logger LOG = Logger.getInstance(BuildSymbolsIndexProvider.class.getName());
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
    final BuildArtifact symbols = sBuild.getArtifacts(BuildArtifactsViewMode.VIEW_HIDDEN_ONLY).getArtifact(".teamcity/symbols");
    final long buildId = sBuild.getBuildId();
    final Set<String> processedSymbols = new HashSet<>();
    if(symbols != null){
      for (BuildArtifact symbolSignaturesSource : symbols.getChildren()){
        if (!symbolSignaturesSource.getName().startsWith(SymbolsConstants.SYMBOL_SIGNATURES_FILE_NAME_PREFIX) &&
            !symbolSignaturesSource.getName().startsWith(SymbolsConstants.BINARY_SIGNATURES_FILE_NAME_PREFIX))
          continue;

        final Set<PdbSignatureIndexEntry> indexEntries;
        try {
          indexEntries = PdbSignatureIndexUtil.read(symbolSignaturesSource.getInputStream(), false);
        } catch (Exception e) {
          LOG.warnAndDebugDetails(String.format(
            "Failed to read symbols index file %s of build %s",
            symbolSignaturesSource.getRelativePath(), LogUtil.describe(sBuild)), e);
          continue;
        }

        LOG.debug(String.format("Build with id %d provides %d symbol file signatures.", buildId, indexEntries.size()));

        for (final PdbSignatureIndexEntry indexEntry : indexEntries) {
          final String signature = indexEntry.getGuid();
          final String fileName = indexEntry.getFileName();
          final String metadataKey = getMetadataKey(signature, fileName);

          if (processedSymbols.contains(metadataKey)) continue;

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

          metadataStorageWriter.addParameters(metadataKey, data);
          mySymbolsCache.invalidate(buildId);
          processedSymbols.add(metadataKey);
        }
      }
    }
    if (processedSymbols.isEmpty()) {
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
