package jetbrains.buildServer.symbols;

import jetbrains.buildServer.util.FileUtil;

import java.io.File;
import java.io.IOException;
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

  public void patch(File symbolsFile) throws IOException {
    final Collection<File> sourceFiles = mySrcToolExe.getReferencedSourceFiles(symbolsFile);
    final File tmpFile = FileUtil.createTempFile(myHomeDir, "pdb-patch", ".xml", false);
    myIndexInputProvider.dumpStreamToFile(tmpFile, sourceFiles);
    myPdbStrExe.doCommand(PdbStrExeCommand.WRITE, symbolsFile, tmpFile, PdbStrExe.SRCSRV_STREAM_NAME);
    //TODO: check that data was actually written
  }
}
