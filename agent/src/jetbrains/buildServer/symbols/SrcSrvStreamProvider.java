/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;

/**
 * @author Evgeniy.Koshkin
 */
public class SrcSrvStreamProvider {

  private static final String myRestApiUrl = "http://localhost:8111/bs/guestAuth/app/rest";
  private long myBuildId;
  private File mySourcesRootDirectory;

  public SrcSrvStreamProvider(final long buildId, final File sourcesRootDirectory) {
    myBuildId = buildId;
    mySourcesRootDirectory = sourcesRootDirectory;
  }

  public void dumpStreamToFile(File targetFile, Collection<File> sourceFiles) throws IOException {
    final FileWriter fileWriter = new FileWriter(targetFile.getPath(), true);
    try {
      fileWriter.write("SRCSRV: ini ------------------------------------------------\r\n");
      fileWriter.write(String.format("VERSION=%d\n", 1));
      fileWriter.write("SRCSRV: variables ------------------------------------------\r\n");
      fileWriter.write("SRCSRVTRG=%http_extract_target%\n");
      fileWriter.write("SRCSRVCMD=");
      fileWriter.write(String.format("HTTP_EXTRACT_TARGET=%s/builds/id:%d/sources/files", myRestApiUrl, myBuildId) + "/%var2%\r\n");
      fileWriter.write("SRCSRV: source files ------------------------------------------\r\n");
      String sourcesRootDirectoryPath = mySourcesRootDirectory.getCanonicalPath();
      for(File sourceFile : sourceFiles){
        final String sourceFileCanonical = sourceFile.getCanonicalPath();
        fileWriter.write(String.format("%s*%s\r\n", sourceFileCanonical, sourceFileCanonical.substring(sourcesRootDirectoryPath.length() + 1).replace(File.separator, "/")));
      }

      fileWriter.write("SRCSRV: end ------------------------------------------------");
    }
    finally {
      fileWriter.close();
    }
  }
}
