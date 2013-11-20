package jetbrains.buildServer.symbols;

import jetbrains.buildServer.serverSide.metadata.BuildMetadataEntry;
import jetbrains.buildServer.serverSide.metadata.MetadataStorage;
import org.jetbrains.annotations.NotNull;

import java.util.*;

class MetadataStorageMock implements MetadataStorage {

  private List<BuildMetadataEntry> myEntries = new ArrayList<BuildMetadataEntry>();

  public void addEntry(final long buildId, final String fileName, final String fileSignature) {
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
        map.put(BuildSymbolsIndexProvider.ARTIFACT_PATH_KEY, "foo");
        map.put(BuildSymbolsIndexProvider.FILE_NAME_KEY, fileName);
        return map;
      }
    });
  }

  @NotNull
  public Iterator<BuildMetadataEntry> getAllEntries(@NotNull String s) {
    return myEntries.iterator();
  }

  @NotNull
  public Iterator<BuildMetadataEntry> getEntriesByKey(@NotNull String s, @NotNull String s2) {
    return myEntries.iterator();
  }

  @NotNull
  public Iterator<BuildMetadataEntry> getBuildEntry(long l, @NotNull String s) {
    return myEntries.iterator();
  }
}