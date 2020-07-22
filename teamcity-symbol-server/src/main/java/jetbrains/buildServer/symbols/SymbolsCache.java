/*
 * Copyright 2000-2020 JetBrains s.r.o.
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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalNotification;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import jetbrains.buildServer.serverSide.BuildServerAdapter;
import jetbrains.buildServer.serverSide.BuildServerListener;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.TeamCityProperties;
import jetbrains.buildServer.serverSide.metadata.BuildMetadataEntry;
import jetbrains.buildServer.util.EventDispatcher;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class SymbolsCache {

  private static final Logger LOG = Logger.getLogger(SymbolsCache.class);

  /**
   * Contains the map of cached requests to symbol server metadata storage.
   *
   * The key is a buildId.
   * The value is a build metadata entry map or empty value if build was not found.
   */
  private final Cache<Long, Optional<Map<String, BuildMetadataEntry>>> myCachedBuilds;

  private final ConcurrentMap<String, Long> myKeyToBuildIdMap = new ConcurrentHashMap<>();
  private final Cache<String, Boolean> myMissedSymbols;

  public SymbolsCache(@NotNull final EventDispatcher<BuildServerListener> events) {
    final int missedSymbolsCacheSize = TeamCityProperties.getInteger(SymbolsConstants.SYMBOLS_SERVER_MISS_CACHE_ENTRIES_SIZE, 2048);
    final int missedSymbolsExpirationTimeSec = TeamCityProperties.getInteger(SymbolsConstants.SYMBOLS_SERVER_MISS_CACHE_EXPIRATION_TIME_SEC, 60 * 60 * 3);

    myMissedSymbols = CacheBuilder
      .newBuilder()
      .maximumSize(missedSymbolsCacheSize)
      .expireAfterAccess(missedSymbolsExpirationTimeSec, TimeUnit.SECONDS)
      .build();

    final int cacheSize = TeamCityProperties.getInteger(SymbolsConstants.SYMBOLS_SERVER_CACHE_ENTRIES_SIZE, 32);
    final int expirationTimeSec = TeamCityProperties.getInteger(SymbolsConstants.SYMBOLS_SERVER_CACHE_EXPIRATION_TIME_SEC, 60 * 60);

    myCachedBuilds = CacheBuilder
      .newBuilder()
      .maximumSize(cacheSize)
      .expireAfterAccess(expirationTimeSec, TimeUnit.SECONDS)
      .removalListener((RemovalNotification<Long, Optional<Map<String, BuildMetadataEntry>>> notification) -> {
        LOG.debug("Removing cache entry. BuildId: " + notification.getValue());

        Optional<Map<String, BuildMetadataEntry>> notificationValue = notification.getValue();
        if (notificationValue == null || !notificationValue.isPresent()) {
          return;
        }

        for (String entryKey: notificationValue.get().keySet()) {
          LOG.debug("Removing entryKey: " + entryKey);
          myKeyToBuildIdMap.remove(entryKey);
        }

        LOG.debug("All build-related entries was removed from cache. BuildId: " + notification.getValue());
      })
      .build();

    events.addListener(new BuildServerAdapter() {
      @Override
      public void buildArtifactsChanged(@NotNull SBuild build) {
        myCachedBuilds.invalidate(build.getBuildId());
      }
    });
  }

  public BuildMetadataEntry getEntry(@NotNull final String key,
                                     @NotNull final MetadataSource metadataSource) {
    Boolean missedSymbol = myMissedSymbols.getIfPresent(key);
    if (missedSymbol != null && missedSymbol) {
      LOG.debug("Symbol server does not host the symbol. Missed symbols cache contains key: " + key);
      return null;
    }

    try {
      Long buildId = myKeyToBuildIdMap.get(key);
      if (buildId == null) {
        LOG.debug("Searching buildId by key. Key: " + key);
        buildId = metadataSource.getBuildIdByEntryKey(key);
        if (buildId == null) {
          LOG.debug("Could not found buildId by key. Key: " + key);
          myMissedSymbols.put(key, true);
          return null;
        } else {
          myKeyToBuildIdMap.put(key, buildId);
        }
      }

      LOG.debug("Key was found in keyToBuildMap. Key: " + key + ", BuildId: " + buildId);

      final AtomicBoolean shouldUpdateMap = new AtomicBoolean(false);
      final Long lambdaBuildId = buildId;
      final Optional<Map<String, BuildMetadataEntry>> buildEntries = myCachedBuilds.get(buildId, () -> {
        final List<BuildMetadataEntry> entries = metadataSource.getEntriesByBuildId(lambdaBuildId);
        if (entries.isEmpty()) {
          return Optional.empty();
        }
        shouldUpdateMap.set(true);
        final HashMap<String, BuildMetadataEntry> result = new HashMap<String, BuildMetadataEntry>();
        for (BuildMetadataEntry entry: entries) {
          result.put(entry.getKey(), entry);
        }
        return Optional.of(result);
      });

      if (buildEntries.isPresent()) {
        LOG.debug("Build entries was found in cache. BuildId: " + buildId);
        final Map<String, BuildMetadataEntry> buildEntriesMap = buildEntries.get();
        if (shouldUpdateMap.get()) {
          for (String entryKey: buildEntriesMap.keySet()) {
            myKeyToBuildIdMap.put(entryKey, lambdaBuildId);
            myMissedSymbols.invalidate(entryKey);
          }
        }
        final BuildMetadataEntry metadata = buildEntriesMap.get(key);
        if (metadata != null) {
          if (LOG.isDebugEnabled()) {
            LOG.debug("Key was found in cache. Key: " + key + ", Value: " + metadata);
          }
          return metadata;
        }
      }

      LOG.debug("Key was found in keyToBuildIdMap but there was no such build in cache. Removing key from keyToBuildIdMap. Key: " + key + ", BuildId: " + buildId);
      myKeyToBuildIdMap.remove(key);
    } catch (ExecutionException | InterruptedException | TimeoutException e) {
      LOG.error("Exception occured during metadata loading", e);
    }
    return null;
  }

  public void invalidate(long buildId) {
    LOG.debug("Removing symbols cache for build with BuildId: " + buildId);
    myCachedBuilds.invalidate(buildId);
  }
}

