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

package jetbrains.buildServer.symbols;

import com.intellij.execution.configurations.GeneralCommandLine;
import jetbrains.buildServer.ExecResult;
import jetbrains.buildServer.SimpleCommandLineProcessRunner;
import jetbrains.buildServer.util.CollectionsUtil;
import jetbrains.buildServer.util.Converter;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

/**
 * @author Evgeniy.Koshkin
 */
public class SrcToolExe {
  private static final String DUMP_SOURCES_FROM_PDB_SWITCH = "-r";

  private final File mySrcToolPath = new File("c:\\Program Files (x86)\\Windows Kits\\8.0\\Debuggers\\x64\\srcsrv\\srctool.exe");

  public Collection<File> getReferencedSourceFiles(File symbolsFile) {
    final GeneralCommandLine commandLine = new GeneralCommandLine();
    commandLine.setExePath(mySrcToolPath.getPath());
    commandLine.addParameter(symbolsFile.getAbsolutePath());
    commandLine.addParameter(DUMP_SOURCES_FROM_PDB_SWITCH);
    final ExecResult execResult = SimpleCommandLineProcessRunner.runCommand(commandLine, null);
    return CollectionsUtil.convertAndFilterNulls(Arrays.asList(execResult.getOutLines()), new Converter<File, String>() {
      public File createFrom(@NotNull String source) {
        final File file = new File(source);
        if (file.isFile()) return file;
        return null; //last string is not a source file path
      }
    });
  }
}
