package jetbrains.buildServer.symbols.tools;

import com.intellij.execution.configurations.GeneralCommandLine;
import jetbrains.buildServer.ExecResult;
import jetbrains.buildServer.SimpleCommandLineProcessRunner;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.util.FileUtil;

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
