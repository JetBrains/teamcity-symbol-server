package jetbrains.buildServer.symbols;

import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.symbols.tools.JetSymbolsExe;
import jetbrains.buildServer.symbols.tools.PdbStrExe;
import jetbrains.buildServer.symbols.tools.PdbType;
import jetbrains.buildServer.symbols.tools.SrcToolExe;
import org.jetbrains.annotations.NotNull;

public class PdbFilePatcherAdapterFactoryImpl implements PdbFilePatcherAdapterFactory {
  private final FileUrlProvider myUrlProvider;
  private final BuildProgressLogger myProgressLogger;
  private final PdbStrExe myPdbStrExe;
  private final JetSymbolsExe myJetSymbolsExe;
  private final SrcToolExe mySrcToolExe;

  public PdbFilePatcherAdapterFactoryImpl(
    @NotNull final FileUrlProvider urlProvider,
    @NotNull final BuildProgressLogger progressLogger,
    @NotNull final PdbStrExe pdbStrExe,
    @NotNull  final JetSymbolsExe jetSymbolsExe,
    @NotNull final SrcToolExe srcToolExe) {
    myUrlProvider = urlProvider;
    myProgressLogger = progressLogger;
    myPdbStrExe = pdbStrExe;
    myJetSymbolsExe = jetSymbolsExe;
    mySrcToolExe = srcToolExe;
  }

  @Override
  public PdbFilePatcherAdapter create(final PdbType type) {
    if (type == PdbType.Portable) {
      final SourceLinkStreamBuilder sourceLinkStreamBuilder = new SourceLinkStreamBuilder(myUrlProvider, myProgressLogger);
      return new PortablePdbFilePatcherAdapterImpl(sourceLinkStreamBuilder, myJetSymbolsExe, myProgressLogger);
    }
    final SrcSrvStreamBuilder srcSrvStreamBuilder = new SrcSrvStreamBuilder(myUrlProvider, myProgressLogger);
    return new PdbFilePatcherAdapterImpl(srcSrvStreamBuilder, myPdbStrExe, mySrcToolExe, myProgressLogger);
  }
}
