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
