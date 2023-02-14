/*
 * Copyright 2000-2023 JetBrains s.r.o.
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
import java.util.Collection;
import jetbrains.buildServer.ExecResult;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.symbols.tools.JetSymbolsExe;
import org.jetbrains.annotations.NotNull;

public class PortablePdbFilePatcherAdapterImpl implements PdbFilePatcherAdapter {
  private final SourceLinkStreamBuilder mySourceLinkStreamBuilder;
  private final JetSymbolsExe myJetSymbolsExe;
  private final BuildProgressLogger myBuildLogger;

  public PortablePdbFilePatcherAdapterImpl(
    @NotNull final SourceLinkStreamBuilder sourceLinkStreamBuilder,
    @NotNull final JetSymbolsExe jetSymbolsExe,
    @NotNull final BuildProgressLogger buildLogger) {
    mySourceLinkStreamBuilder = sourceLinkStreamBuilder;
    myJetSymbolsExe = jetSymbolsExe;
    myBuildLogger = buildLogger;
  }

  @Override
  public int serializeSourceLinks(final File sourceLinksFile, final Collection<File> sourceFiles) throws IOException {
    return mySourceLinkStreamBuilder.dumpStreamToFile(sourceLinksFile, sourceFiles);
  }

  @Override
  public ExecResult updatePdbSourceLinks(final File symbolsFile, final File sourceLinksFile) {
    return myJetSymbolsExe.updatePortablePdbSourceUrls(symbolsFile, sourceLinksFile, myBuildLogger);
  }

  @Override
  public Collection<File> getReferencedSourceFiles(final File symbolsFile) {
    return myJetSymbolsExe.getReferencedSourceFiles(symbolsFile, myBuildLogger);
  }
}
