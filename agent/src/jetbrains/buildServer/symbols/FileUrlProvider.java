package jetbrains.buildServer.symbols;

import java.io.File;
import java.io.IOException;

/**
 * @author Evgeniy.Koshkin
 */
public class FileUrlProvider {
  private final String myUrlPrefix;
  private final long myBuildId;
  private final File mySourcesRootDirectory;

  public FileUrlProvider(String serverUrl, long buildId, File sourcesRootDirectory) {
    myUrlPrefix = serverUrl;
    myBuildId = buildId;
    mySourcesRootDirectory = sourcesRootDirectory;
  }

  public String getHttpAlias() {
    return String.format("%s/builds/id-%d/sources/files", myUrlPrefix, myBuildId);
  }

  public String getFileUrl(String path) throws IOException {
    String sourcesRootDirectoryPath = mySourcesRootDirectory.getCanonicalPath();
    return path.substring(sourcesRootDirectoryPath.length() + 1).replace(File.separator, "/");
  }
}
