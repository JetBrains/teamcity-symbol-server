package jetbrains.buildServer.symbols;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;

/**
 * @author Evgeniy.Koshkin
 */
public class SrcSrvStreamBuilder {

  private final FileUrlProvider myUrlProvider;

  public SrcSrvStreamBuilder(final FileUrlProvider urlProvider) {
    myUrlProvider = urlProvider;
  }

  public void dumpStreamToFile(File targetFile, Collection<File> sourceFiles) throws IOException {
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
        final String sourceFileCanonical = sourceFile.getCanonicalPath();
        fileWriter.write(String.format("%s*%s\r\n", sourceFileCanonical, myUrlProvider.getFileUrl(sourceFileCanonical)));
      }
      fileWriter.write("SRCSRV: end ------------------------------------------------");
    }
    finally {
      fileWriter.close();
    }
  }
}
