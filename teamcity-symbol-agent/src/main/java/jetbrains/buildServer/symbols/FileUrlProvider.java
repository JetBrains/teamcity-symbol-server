

package jetbrains.buildServer.symbols;

import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.StringUtil;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.File;

/**
 * @author Evgeniy.Koshkin
 */
public class FileUrlProvider {

  private static final Logger LOG = Logger.getLogger(FileUrlProvider.class);

  private final String myUrlPrefix;
  private final long myBuildId;
  private String mySourcesRootDirectoryCanonicalPath;

  public FileUrlProvider(String serverUrl, long buildId, File sourcesRootDirectory) {
    myUrlPrefix = StringUtil.trimEnd(serverUrl, "/"); //cut last '/';
    myBuildId = buildId;
    mySourcesRootDirectoryCanonicalPath = FileUtil.getCanonicalFile(sourcesRootDirectory).getPath();
  }

  public String getBuildPath() {
    return String.format("builds/id-%d/sources/files", myBuildId);
  }

  public String getBasePath() {
    return myUrlPrefix;
  }

  @Nullable
  public String getFileUrl(File file) {
    final String canonicalFilePath = FileUtil.getCanonicalFile(file).getPath();
    if (!StringUtil.startsWithIgnoreCase(canonicalFilePath, mySourcesRootDirectoryCanonicalPath)) {
      LOG.debug(String.format("Failed to construct URL for file %s. It locates outside of source root directory %s.",
        canonicalFilePath, mySourcesRootDirectoryCanonicalPath));
      return null;
    }
    return canonicalFilePath.substring(mySourcesRootDirectoryCanonicalPath.length() + 1)
      .replace(File.separator, "/");
  }
}
