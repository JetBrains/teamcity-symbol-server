package jetbrains.buildServer.symbols;

import jetbrains.buildServer.util.FileUtil;
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

  public String getHttpAlias() {
    return String.format("%s/builds/id-%d/sources/files", myUrlPrefix, myBuildId);
  }

  @Nullable
  public String getFileUrl(File file) {
    final File canonicalFile = FileUtil.getCanonicalFile(file);
    final String canonicalFilePath = canonicalFile.getPath();
    if(!canonicalFilePath.startsWith(mySourcesRootDirectoryCanonicalPath)){
      LOG.debug(String.format("Failed to construct URL for file %s. It locates outside of source root directory %s.", canonicalFile, mySourcesRootDirectoryCanonicalPath));
      return null;
    }
    return canonicalFilePath.substring(mySourcesRootDirectoryCanonicalPath.length() + 1).replace(File.separator, "/");
  }
}
