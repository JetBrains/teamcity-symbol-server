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
