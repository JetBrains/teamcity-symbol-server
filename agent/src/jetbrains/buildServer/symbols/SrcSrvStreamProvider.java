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
import java.net.URI;
import java.util.Collection;
import java.util.Date;

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
      fileWriter.write("SRCSRV: ini ------------------------------------------------");
      fileWriter.write(String.format("VERSION=%d", 1));
      fileWriter.write(String.format("INDEXVERSION=%d", 1));
      fileWriter.write("VERCTRL=http");
      fileWriter.write(String.format("DATETIME=%s", (new Date()).toString()));

      fileWriter.write("SRCSRV: variables ------------------------------------------");
      fileWriter.write("SRCSRVVERCTRL=http");
      fileWriter.write(String.format("REST_API_URL=%s", myRestApiUrl));
      fileWriter.write(String.format("BUILD_LOCATOR=id:%d", myBuildId));
      fileWriter.write("HTTP_EXTRACT_TARGET=%REST_API_URL%/%BUILD_LOCATOR%/sources/files/%var2%");
      fileWriter.write("SRCSRVTRG=%HTTP_EXTRACT_TARGET%");
      fileWriter.write("SRCSRVCMD=");

      final URI checkoutDirUri = mySourcesRootDirectory.toURI();
      fileWriter.write("SRCSRV: source files ------------------------------------------");
      for(File sourceFile : sourceFiles){
        final File sourceFileAbsolute = sourceFile.getAbsoluteFile();
        fileWriter.write(String.format("%s*%s", sourceFileAbsolute.getPath(), checkoutDirUri.relativize(sourceFileAbsolute.toURI()).getPath()));
      }

      fileWriter.write("SRCSRV: end ------------------------------------------------");
    }
    finally {
      fileWriter.close();
    }
  }
}
