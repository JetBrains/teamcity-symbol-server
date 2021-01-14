/*
 * Copyright 2000-2021 JetBrains s.r.o.
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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import jetbrains.buildServer.agent.BuildProgressLogger;

public class SourceLinkStreamBuilder {

  private final FileUrlProvider myUrlProvider;
  private final BuildProgressLogger myProgressLogger;
  private final Gson gson = new GsonBuilder().create();

  public SourceLinkStreamBuilder(final FileUrlProvider urlProvider, BuildProgressLogger progressLogger) {
    myUrlProvider = urlProvider;
    myProgressLogger = progressLogger;
  }

  public int dumpStreamToFile(File targetFile, Collection<File> sourceFiles) throws IOException {
    int processedFilesCount = 0;
    final FileWriter fileWriter = new FileWriter(targetFile.getPath(), true);

    final String baseUrl = myUrlProvider.getBasePath() + "/" + myUrlProvider.getBuildPath() + "/";
    try {
      final Map<String, String> sourceMap = new HashMap<String, String>();
      for(File sourceFile : sourceFiles){
        String url = null;
        try{
          url = baseUrl + myUrlProvider.getFileUrl(sourceFile);
        } catch (Exception ex){
          myProgressLogger.warning("Failed to calculate url for source file " + sourceFile);
          myProgressLogger.exception(ex);
        }
        if(url == null) continue;
        processedFilesCount++;

        sourceMap.put(sourceFile.getPath(), url);
      }
      fileWriter.write(gson.toJson(new SourcesDescriptor(sourceMap)));
    }
    finally {
      fileWriter.close();
    }
    return processedFilesCount;
  }

  private class SourcesDescriptor {
    private final Map<String, String> documents;

    private SourcesDescriptor(final Map<String, String> documents) {
      this.documents = documents;
    }
  }
}
