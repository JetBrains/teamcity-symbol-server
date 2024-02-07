

package jetbrains.buildServer.symbols;

import java.util.List;
import java.util.concurrent.TimeoutException;
import jetbrains.buildServer.serverSide.metadata.BuildMetadataEntry;

public interface MetadataSource {
  List<BuildMetadataEntry> getEntriesByBuildId(Long buildId) throws InterruptedException, TimeoutException;
  Long getBuildIdByEntryKey(String key) throws InterruptedException, TimeoutException;
}
