package jetbrains.buildServer.symbols;

import jetbrains.buildServer.util.FileUtil;

import java.io.File;
import java.util.Collection;

/**
 * @author Evgeniy.Koshkin
 */
public class PdbFilePatcher {

  private final PdbStrExe myPdbStrExe = new PdbStrExe();
  private final SrcToolExe mySrcToolExe = new SrcToolExe();

  private final File myHomeDir;
  private SrcSrvStreamProvider myIndexInputProvider;

  public PdbFilePatcher(final File homeDir, final SrcSrvStreamProvider indexInputProvider) {
    myHomeDir = homeDir;
    myIndexInputProvider = indexInputProvider;
  }

  public void patch(File symbolsFile) throws Exception {
    final Collection<File> sourceFiles = mySrcToolExe.getReferencedSourceFiles(symbolsFile);
    final File tmpFile = FileUtil.createTempFile(myHomeDir, "pdb-", ".patch", false);
    myIndexInputProvider.dumpStreamToFile(tmpFile, sourceFiles);
    myPdbStrExe.doCommand(PdbStrExeCommand.WRITE, symbolsFile, tmpFile, PdbStrExe.SRCSRV_STREAM_NAME);
    final File tmpFile1 = FileUtil.createTempFile(myHomeDir, "pdb-", ".patch", false);
    myPdbStrExe.doCommand(PdbStrExeCommand.READ, symbolsFile, tmpFile1, PdbStrExe.SRCSRV_STREAM_NAME);
    if(!FileUtil.checkContentEqual(tmpFile, tmpFile1))
      throw new Exception("'srcsrv' stream content written to pdb differs from initial one");
  }
}
