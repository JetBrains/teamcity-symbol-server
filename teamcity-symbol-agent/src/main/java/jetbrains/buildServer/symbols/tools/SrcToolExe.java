package jetbrains.buildServer.symbols.tools;

import com.intellij.execution.configurations.GeneralCommandLine;
import java.io.File;
import jetbrains.buildServer.ExecResult;
import jetbrains.buildServer.SimpleCommandLineProcessRunner;

public class SrcToolExe {

  private static final String SRCTOOL_EXE = "srctool.exe";
  private static final String DUMP_REFERENCES_SWITCH = "-r";
  private static final String ZERRO_ON_SUCCESS_SWITCH = "-z";

  private final File myPath;

  public SrcToolExe(File srcToolHomeDir) {
    myPath = new File(srcToolHomeDir, SRCTOOL_EXE);
  }

  public ExecResult dumpSources(final File pdbFile){
    final GeneralCommandLine commandLine = new GeneralCommandLine();
    commandLine.setWorkDirectory(myPath.getParent());
    commandLine.setExePath(myPath.getPath());
    commandLine.addParameter(pdbFile.getAbsolutePath());
    commandLine.addParameter(DUMP_REFERENCES_SWITCH);
    commandLine.addParameter(ZERRO_ON_SUCCESS_SWITCH);
    return SimpleCommandLineProcessRunner.runCommand(commandLine, null);
  }
}


