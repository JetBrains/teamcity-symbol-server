package jetbrains.buildServer.symbols;

import jetbrains.buildServer.agent.BuildProgressLogger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;

/**
 * @author Evgeniy.Koshkin
 */
public class SrcSrvStreamBuilder {

  private final FileUrlProvider myUrlProvider;
  private final BuildProgressLogger myProgressLogger;

  public SrcSrvStreamBuilder(final FileUrlProvider urlProvider, BuildProgressLogger progressLogger) {
    myUrlProvider = urlProvider;
    myProgressLogger = progressLogger;
  }

  public int dumpStreamToFile(File targetFile, Collection<File> sourceFiles) throws IOException {
    int processedFilesCount = 0;
    final FileWriter fileWriter = new FileWriter(targetFile.getPath(), true);

    try {
      fileWriter.write("SRCSRV: ini ------------------------------------------------\r\n");
      fileWriter.write("VERSION=3\r\n");
      fileWriter.write("INDEXVERSION=2\r\n");
      fileWriter.write("VERCTRL=http\r\n");
      fileWriter.write("SRCSRV: variables ------------------------------------------\r\n");
      fileWriter.write("SRCSRVVERCTRL=http\r\n");
      fileWriter.write(String.format("HTTP_ALIAS=%s\r\n", myUrlProvider.getHttpAlias()));
      fileWriter.write("HTTP_EXTRACT_TARGET=%HTTP_ALIAS%/%var2%\r\n");
      fileWriter.write("SRCSRVTRG=%HTTP_EXTRACT_TARGET%\r\n");
      fileWriter.write("SRCSRVCMD=\r\n");
      fileWriter.write("SRCSRV: source files ------------------------------------------\r\n");
      for(File sourceFile : sourceFiles){
        String url = null;
        try{
          url = myUrlProvider.getFileUrl(sourceFile);
        } catch (Exception ex){
          myProgressLogger.warning("Failed to calculate url for source file " + sourceFile);
          myProgressLogger.exception(ex);
        }
        if(url == null) continue;
        processedFilesCount++;
        fileWriter.write(String.format("%s*%s\r\n", sourceFile, url));
      }
      fileWriter.write("SRCSRV: end ------------------------------------------------");
    }
    finally {
      fileWriter.close();
    }
    return processedFilesCount;
  }
}
