

package jetbrains.buildServer.symbols.tools;

import com.intellij.execution.configurations.GeneralCommandLine;
import jetbrains.buildServer.ExecResult;
import jetbrains.buildServer.SimpleCommandLineProcessRunner;

import java.io.File;

/**
 * @author Evgeniy.Koshkin
 */
public class PdbStrExe {

  public static final String SRCSRV_STREAM_NAME = "srcsrv";

  private static final String PDBSTR_EXE = "pdbstr.exe";
  private static final String STREAM_NAME_SWITCH = "-s";
  private static final String PATH_TO_PDB_FILE_SWITCH = "-p";
  private static final String PATH_TO_INPUT_FILE_SWITCH = "-i";

  private final File myPath;

  public PdbStrExe(File srcSrvHomeDir) {
    myPath = new File(srcSrvHomeDir, PDBSTR_EXE);
  }

  public ExecResult doCommand(final PdbStrExeCommands cmd, final File pdbFile, final File inputStreamFile, final String streamName){
    final GeneralCommandLine commandLine = new GeneralCommandLine();
    commandLine.setWorkDirectory(myPath.getParent());
    commandLine.setExePath(myPath.getPath());
    commandLine.addParameter(cmd.getCmdSwitch());
    commandLine.addParameter(String.format("%s:%s", PATH_TO_PDB_FILE_SWITCH, pdbFile.getAbsolutePath()));
    commandLine.addParameter(String.format("%s:%s", PATH_TO_INPUT_FILE_SWITCH, inputStreamFile.getAbsolutePath()));
    commandLine.addParameter(STREAM_NAME_SWITCH + ":" + streamName);
    return SimpleCommandLineProcessRunner.runCommand(commandLine, null);
  }
}
