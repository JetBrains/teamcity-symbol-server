package jetbrains.buildServer.symbols;

import java.io.File;
import java.io.IOException;

/**
 * @author Evgeniy.Koshkin
 */
public class FileUrlProvider {
  private static final String GUEST_AUTH_APP_SOURCES = "guestAuth/app/sources";

  private final String myServerUrl;
  private final long myBuildId;
  private final File mySourcesRootDirectory;

  public FileUrlProvider(String serverUrl, long buildId, File sourcesRootDirectory) {
    myServerUrl = serverUrl;
    myBuildId = buildId;
    mySourcesRootDirectory = sourcesRootDirectory;
  }

  public String getHttpAlias() {
    return String.format("%s/%s/builds/id-%d/sources/files", myServerUrl, GUEST_AUTH_APP_SOURCES, myBuildId);
  }

  public String getFileUrl(String path) throws IOException {
    String sourcesRootDirectoryPath = mySourcesRootDirectory.getCanonicalPath();
    return path.substring(sourcesRootDirectoryPath.length() + 1).replace(File.separator, "/");
  }
}
