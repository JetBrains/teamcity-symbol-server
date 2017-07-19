package jetbrains.buildServer.symbols;

import jetbrains.buildServer.serverSide.metadata.BuildMetadataEntry;
import jetbrains.buildServer.serverSide.metadata.MetadataStorage;
import jetbrains.buildServer.serverSide.metadata.MetadataStorageWriter;
import jetbrains.buildServer.util.Action;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

class MetadataStorageMock implements MetadataStorage {

  private List<BuildMetadataEntry> myEntries = new ArrayList<>();

  public void addEntry(final long buildId, final String filePath, final String fileSignature) {
    myEntries.add(new BuildMetadataEntry() {
      public long getBuildId() {
        return buildId;
      }

      @NotNull
      public String getKey() {
        return fileSignature;
      }

      @NotNull
      public Map<String, String> getMetadata() {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put(BuildSymbolsIndexProvider.ARTIFACT_PATH_KEY, filePath);
        return map;
      }
    });
  }

  @NotNull
  public Iterator<BuildMetadataEntry> getAllEntries(@NotNull String s) {
    return myEntries.iterator();
  }

  public int getNumberOfEntries(@NotNull String s) {
    return 0;
  }

  @NotNull
  public Iterator<BuildMetadataEntry> getEntriesByKey(@NotNull String s, @NotNull String s2) {
    return myEntries.iterator();
  }

  @NotNull
  public Iterator<BuildMetadataEntry> getBuildEntry(long l, @NotNull String s) {
    return myEntries.iterator();
  }

  public boolean updateCache(long l, boolean b, @NotNull String s, @NotNull Action<MetadataStorageWriter> action) {
    return false;
  }

  @Override
  public boolean removeBuildEntries(final long buildId, @NotNull String providerId) {
    return myEntries.removeIf(buildMetadataEntry -> buildMetadataEntry.getBuildId() == buildId);
  }

  @Override
  public void addBuildEntry(long l, @NotNull String s, @NotNull String s1, @NotNull Map<String, String> map, boolean b) {
    myEntries.add(new BuildMetadataEntry() {
      @Override
      public long getBuildId() {
        return l;
      }

      @NotNull
      @Override
      public String getKey() {
        return s1;
      }

      @NotNull
      @Override
      public Map<String, String> getMetadata() {
        return map;
      }
    });
  }

  @NotNull
  public Iterator<BuildMetadataEntry> findEntriesWithValue(@NotNull String s, @NotNull String s1, Collection<String> collection) {
    return myEntries.iterator();
  }

  @NotNull
  @Override
  public Iterator<BuildMetadataEntry> findEntriesWithValue(@NotNull String s, @NotNull String s1, @Nullable Collection<String> collection, boolean b) {
    return myEntries.iterator();
  }

  @NotNull
  @Override
  public Iterator<BuildMetadataEntry> findEntriesWithKeyValuePairs(@NotNull String s, @NotNull Map<String, String> map, boolean b) {
    return myEntries.iterator();
  }
}