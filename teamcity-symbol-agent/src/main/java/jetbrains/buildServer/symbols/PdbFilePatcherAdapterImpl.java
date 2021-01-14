/*
 * Copyright 2000-2021 JetBrains s.r.o.
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

import java.io.File;
import java.io.IOException;
import java.util.*;
import jetbrains.buildServer.ExecResult;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.symbols.tools.PdbStrExe;
import jetbrains.buildServer.symbols.tools.PdbStrExeCommands;
import jetbrains.buildServer.symbols.tools.SrcToolExe;
import org.jetbrains.annotations.NotNull;

public class PdbFilePatcherAdapterImpl implements PdbFilePatcherAdapter {
  private final SrcSrvStreamBuilder mySrcSrvStreamBuilder;
  private final PdbStrExe myPdbStrExe;
  private final SrcToolExe mySrcToolExe;
  private final BuildProgressLogger myBuildLogger;

  public PdbFilePatcherAdapterImpl(
    @NotNull final SrcSrvStreamBuilder srcSrvStreamBuilder,
    @NotNull final PdbStrExe pdbStrExe,
    @NotNull final SrcToolExe srcToolExe,
    @NotNull final BuildProgressLogger buildLogger) {
    mySrcSrvStreamBuilder = srcSrvStreamBuilder;
    myPdbStrExe = pdbStrExe;
    mySrcToolExe = srcToolExe;
    myBuildLogger = buildLogger;
  }

  @Override
  public int serializeSourceLinks(final File sourceLinksFile, final Collection<File> sourceFiles) throws IOException {
    return mySrcSrvStreamBuilder.dumpStreamToFile(sourceLinksFile, sourceFiles);
  }

  @Override
  public ExecResult updatePdbSourceLinks(final File symbolsFile, final File sourceLinksFile) {
    return myPdbStrExe.doCommand(PdbStrExeCommands.WRITE, symbolsFile, sourceLinksFile, PdbStrExe.SRCSRV_STREAM_NAME);
  }

  @Override
  public Collection<File> getReferencedSourceFiles(final File symbolsFile) throws IOException {
    final ExecResult result = mySrcToolExe.dumpSources(symbolsFile, myBuildLogger);
    if (result.getExitCode() < 0) {
      throw new IOException(String.format("Failed to dump sources from symbols file %s: %s", symbolsFile, result));
    }

    String[] outLines = result.getOutLines();
    ArrayList<File> resultCollection = new ArrayList<File>();
    for(int index = 0; index < outLines.length - 1; index++) {
      final String line = outLines[index];
      if (line != null) {
        resultCollection.add(new File(line));
      }
    }
    return resultCollection;
  }
}
