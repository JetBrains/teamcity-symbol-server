package jetbrains.buildServer.symbols;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;

/**
 * @author Evgeniy.Koshkin
 */
public class FileUrlProvider {

  private static final Logger LOG = Logger.getLogger(FileUrlProvider.class);

  private final String myUrlPrefix;
  private final long myBuildId;
  private String mySourcesRootDirectoryCanonicalPath;

  public FileUrlProvider(String serverUrl, long buildId, File sourcesRootDirectory) throws IOException {
    myUrlPrefix = serverUrl;
    myBuildId = buildId;
    mySourcesRootDirectoryCanonicalPath = sourcesRootDirectory.getCanonicalPath();
  }

  public String getBuildPath() {
    return String.format("builds/id-%d/sources/files", myBuildId);
  }

  public String getBasePath() {
    return myUrlPrefix;
  }

  @Nullable
  public String getFileUrl(File file) throws IOException {
    final File canonicalFile = file.getCanonicalFile();
    final String canonicalFilePath = canonicalFile.getPath();
    if(!canonicalFilePath.startsWith(mySourcesRootDirectoryCanonicalPath)){
      LOG.debug(String.format("Failed to construct URL for file %s. It locates outside of source root directory %s.", canonicalFile, mySourcesRootDirectoryCanonicalPath));
      return null;
    }
    return canonicalFilePath.substring(mySourcesRootDirectoryCanonicalPath.length() + 1).replace(File.separator, "/");
  }
}
