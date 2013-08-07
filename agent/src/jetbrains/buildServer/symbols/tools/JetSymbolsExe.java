package jetbrains.buildServer.symbols.tools;

import com.intellij.execution.configurations.GeneralCommandLine;
import jetbrains.buildServer.ExecResult;
import jetbrains.buildServer.SimpleCommandLineProcessRunner;
import jetbrains.buildServer.agent.BuildProgressLogger;

import java.io.File;
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

  public void dumpGuidsToFile(Collection<File> files, File output, BuildProgressLogger buildLogger){
    final GeneralCommandLine commandLine = new GeneralCommandLine();
    commandLine.setExePath(myExePath.getPath());
    commandLine.addParameter(DUMP_SYMBOL_SIGN_CMD);
    commandLine.addParameter(String.format("/o=\"%s\"", output.getPath()));
    for(File file : files){
      commandLine.addParameter(file.getPath());
    }
    buildLogger.message(String.format("Running command %s", commandLine.getCommandLineString()));
    final ExecResult execResult = SimpleCommandLineProcessRunner.runCommand(commandLine, null);
    final String stdout = execResult.getStdout();
    if(!stdout.isEmpty()){
      buildLogger.message("Stdout: " + stdout);
    }
    if (execResult.getExitCode() == 0) return;
    buildLogger.warning(String.format("%s ends with non-zero exit code.", SYMBOLS_EXE));
    buildLogger.warning("Stdout: " + stdout);
    buildLogger.warning("Stderr: " + execResult.getStderr());
    buildLogger.exception(execResult.getException());
  }
}
