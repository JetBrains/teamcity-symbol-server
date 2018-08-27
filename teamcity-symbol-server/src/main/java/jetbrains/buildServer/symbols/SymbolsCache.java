package jetbrains.buildServer.symbols;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import jetbrains.buildServer.serverSide.BuildServerAdapter;
import jetbrains.buildServer.serverSide.BuildServerListener;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.TeamCityProperties;
import jetbrains.buildServer.serverSide.metadata.BuildMetadataEntry;
import jetbrains.buildServer.util.EventDispatcher;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class SymbolsCache {

  private static final Logger LOG = Logger.getLogger(SymbolsCache.class);

  /**
   * Contains the map of cached requests to symbol server metadata storage.
   *
   * The key is a composite PDB entry key, see BuildSymbolsIndexProvider#getMetadataKey.
   * The value is a build metadata entry or empty value if entry was not found.
   */
  private final Cache<String, Optional<BuildMetadataEntry>> myCachedRequests;

  /**
   * Contains the map of cached composite keys by build id used during cleanup on event
   * BuildServerListener#buildArtifactsChanged(jetbrains.buildServer.serverSide.SBuild).
   *
   * The key is a build id.
   * The value is a set of cached composite keys.
   */
  private final ConcurrentMap<Long, Collection<String>> myCachedKeysByBuildId = new ConcurrentHashMap<>();

  public SymbolsCache(@NotNull final EventDispatcher<BuildServerListener> events) {
    final int cacheSize = TeamCityProperties.getInteger(SymbolsConstants.SYMBOLS_SERVER_CACHE_ENTRIES_SIZE, 256);
    final int expirationTimeSec = TeamCityProperties.getInteger(SymbolsConstants.SYMBOLS_SERVER_CACHE_EXPIRATION_TIME_SEC, 60 * 30);
    myCachedRequests = Caffeine.newBuilder()
      .maximumSize(cacheSize)
      .executor(Runnable::run)
      .expireAfterAccess(expirationTimeSec, TimeUnit.SECONDS)
      .removalListener((String key, Optional<BuildMetadataEntry> entry, RemovalCause cause) -> {
        if (entry == null || !entry.isPresent()) {
          return;
        }
        final BuildMetadataEntry buildMetadataEntry = entry.get();
        final long buildId = buildMetadataEntry.getBuildId();
        final Collection<String> keys = myCachedKeysByBuildId.get(buildId);
        if (keys == null) {
          return;
        }
        keys.remove(key);
        if (keys.isEmpty()) {
          myCachedKeysByBuildId.remove(buildId);
        }
      })
      .build();

    events.addListener(new BuildServerAdapter() {
      @Override
      public void buildArtifactsChanged(@NotNull SBuild build) {
        final Collection<String> keys = myCachedKeysByBuildId.remove(build.getBuildId());
        if (keys != null) {
          myCachedRequests.invalidateAll(keys);
        }
      }
    });
  }

  public BuildMetadataEntry getEntry(@NotNull final String key,
                                     @NotNull final Function<String, BuildMetadataEntry> function) {
    final Optional<BuildMetadataEntry> metadataEntry = myCachedRequests.get(key, s -> {
      LOG.debug("Creating symbols cache for entry with key: " + s);

      // Calculate build metadata entry
      final BuildMetadataEntry entry = function.apply(s);
      if (entry == null) {
        return Optional.empty();
      }

      // Cache affected composite key for this build
      final Collection<String> keys = myCachedKeysByBuildId.computeIfAbsent(
        entry.getBuildId(), buildId -> ConcurrentHashMap.newKeySet()
      );
      keys.add(s);

      return Optional.of(entry);
    });

    if (metadataEntry != null && metadataEntry.isPresent()) {
      return metadataEntry.get();
    }

    return null;
  }

  public void removeEntry(@NotNull final String key) {
    LOG.debug("Removing symbols cache for entries with key: " + key);
    myCachedRequests.invalidate(key);
  }
}
