package jetbrains.buildServer.symbols;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import jetbrains.buildServer.ExecResult;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.symbols.tools.PdbStrExe;
import jetbrains.buildServer.symbols.tools.PdbStrExeCommands;
import org.jetbrains.annotations.NotNull;

public class PdbFilePatcherAdapterImpl implements PdbFilePatcherAdapter {
  private final SrcSrvStreamBuilder mySrcSrvStreamBuilder;
  private final PdbStrExe myPdbStrExe;

  public PdbFilePatcherAdapterImpl(
    @NotNull final SrcSrvStreamBuilder srcSrvStreamBuilder,
    @NotNull final PdbStrExe pdbStrExe) {
    mySrcSrvStreamBuilder = srcSrvStreamBuilder;
    myPdbStrExe = pdbStrExe;
  }

  @Override
  public int serializeSourceLinks(final File sourceLinksFile, final Collection<File> sourceFiles) throws IOException {
    return mySrcSrvStreamBuilder.dumpStreamToFile(sourceLinksFile, sourceFiles);
  }

  @Override
  public ExecResult updatePdbSourceLinks(final File symbolsFile, final File sourceLinksFile) {
    return myPdbStrExe.doCommand(PdbStrExeCommands.WRITE, symbolsFile, sourceLinksFile, PdbStrExe.SRCSRV_STREAM_NAME);
  }
}
