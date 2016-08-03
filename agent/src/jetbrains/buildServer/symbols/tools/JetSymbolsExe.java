package jetbrains.buildServer.symbols.tools;

import com.intellij.execution.configurations.GeneralCommandLine;
import jetbrains.buildServer.ExecResult;
import jetbrains.buildServer.SimpleCommandLineProcessRunner;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.XmlUtil;
import org.jdom.Document;
import org.jdom.Element;

import java.io.*;
import java.util.Collection;

/**
 * @author Evgeniy.Koshkin
 */
public class JetSymbolsExe {

  private static final String SYMBOLS_EXE = "JetBrains.CommandLine.Symbols.exe";
  private static final String DUMP_SYMBOL_SIGN_CMD = "dumpSymbolSign";
  private final File myExePath;

  public JetSymbolsExe(File homeDir) {
    myExePath = new File(homeDir, SYMBOLS_EXE);
  }

  public int dumpPdbGuidsToFile(Collection<File> files, File output, BuildProgressLogger buildLogger) throws IOException {
    final GeneralCommandLine commandLine = new GeneralCommandLine();
    commandLine.setExePath(myExePath.getPath());
    commandLine.addParameter(DUMP_SYMBOL_SIGN_CMD);
    commandLine.addParameter(String.format("/o=%s", output.getPath()));
    commandLine.addParameter(String.format("/i=%s", dumpPathsToFile(files).getPath()));
    buildLogger.message(String.format("Running command %s", commandLine.getCommandLineString()));
    final ExecResult execResult = SimpleCommandLineProcessRunner.runCommand(commandLine, null);
    final String stdout = execResult.getStdout();
    if(!stdout.isEmpty()){
      buildLogger.message("Stdout: " + stdout);
    }
    final int exitCode = execResult.getExitCode();
    if (exitCode != 0) {
      buildLogger.warning(String.format("%s ends with non-zero exit code %s.", SYMBOLS_EXE, execResult));
      buildLogger.warning("Stdout: " + stdout);
      buildLogger.warning("Stderr: " + execResult.getStderr());
      final Throwable exception = execResult.getException();
      if(exception != null){
        buildLogger.exception(exception);
      }
    }
    return exitCode;
  }

  public void dumpBinaryGuidsToFile(Collection<File> files, File output, BuildProgressLogger buildLogger)
  {
    final Element root = new Element("file-signs");
    for (File file : files) {
      RandomAccessFile randomAccess = null;
      try {
        randomAccess = new RandomAccessFile(file, "r");
        //the PE offset is at byte [60,64)
        int peOffset = readIntLE(randomAccess, 60);
        int timestamp = readIntLE(randomAccess, peOffset + 8);
        int size = readIntLE(randomAccess, peOffset + 80);
        String signature = Integer.toHexString(timestamp) + Integer.toHexString(size);
        final Element entry = new Element("file-sign-entry");
        entry.setAttribute("file", file.getPath());
        entry.setAttribute("sign", signature);
        root.addContent(entry);
      } catch (IOException e) {
        buildLogger.exception(e);
      } finally {
        if (randomAccess != null)
          try {
            randomAccess.close();
          } catch (IOException e) {
            //I hate you, checked exceptions
            buildLogger.exception(e);
          }
      }
    }
    try {
      XmlUtil.saveDocument(new Document(root), new FileOutputStream(output));
    } catch (IOException e) {
      buildLogger.exception(e);
    }
  }

  private static int readIntLE(RandomAccessFile file, long position) throws IOException {
    file.seek(position);
    byte[] bytes = new byte[4];
    int bytesRead = file.read(bytes);
    if (bytesRead != bytes.length) {
      throw new EOFException();
    }
    return (bytes[0]&0xff) +
          ((bytes[1]&0xff)<<8) +
          ((bytes[2]&0xff)<<16) +
          ((bytes[3]&0xff)<<24);
  }

  private File dumpPathsToFile(Collection<File> files) throws IOException {
    final File result = FileUtil.createTempFile(DUMP_SYMBOL_SIGN_CMD, ".input");
    StringBuilder contentBuilder = new StringBuilder();
    for(File file : files){
      contentBuilder.append(file.getPath()).append("\n");
    }
    FileUtil.writeToFile(result, contentBuilder.toString().getBytes());
    return result;
  }
}
