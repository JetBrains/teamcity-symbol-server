

package jetbrains.buildServer.symbols.tools;

import com.intellij.execution.configurations.GeneralCommandLine;
import java.io.File;
import jetbrains.buildServer.ExecResult;
import jetbrains.buildServer.SimpleCommandLineProcessRunner;
import jetbrains.buildServer.agent.BuildProgressLogger;

public class SrcToolExe {

  private static final String SRCTOOL_EXE = "srctool.exe";
  private static final String DUMP_REFERENCES_SWITCH = "-r";

  private final File myPath;

  public SrcToolExe(File srcToolHomeDir) {
    myPath = new File(srcToolHomeDir, SRCTOOL_EXE);
  }

  public ExecResult dumpSources(final File pdbFile, final BuildProgressLogger buildLogger){
    final GeneralCommandLine commandLine = new GeneralCommandLine();
    commandLine.setWorkDirectory(myPath.getParent());
    commandLine.setExePath(myPath.getPath());
    commandLine.addParameter(DUMP_REFERENCES_SWITCH);
    commandLine.addParameter(pdbFile.getAbsolutePath());

    buildLogger.message(String.format("Running command %s", commandLine.getCommandLineString()));

    final ExecResult execResult = SimpleCommandLineProcessRunner.runCommand(commandLine, null);
    if (execResult.getExitCode() < 0) {
      buildLogger.warning(String.format("%s completed with exit code %s.", SRCTOOL_EXE, execResult));
      buildLogger.warning("Stdout: " + execResult.getStdout());
      buildLogger.warning("Stderr: " + execResult.getStderr());
      final Throwable exception = execResult.getException();
      if(exception != null){
        buildLogger.exception(exception);
      }
    }
    return execResult;
  }
}
