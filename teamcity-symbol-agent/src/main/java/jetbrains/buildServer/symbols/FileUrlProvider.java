/*
 * Copyright 2000-2023 JetBrains s.r.o.
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
