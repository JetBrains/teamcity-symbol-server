package jetbrains.buildServer.symbols;

import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.util.FileUtil;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.Collection;

/**
 * @author Evgeniy.Koshkin
 */
public class PdbFilePatcher {

  private static final Logger LOG = Logger.getLogger(PdbFilePatcher.class);

  private final PdbStrExe myPdbStrExe = new PdbStrExe();
  private final SrcToolExe mySrcToolExe = new SrcToolExe();

  private final File myHomeDir;
  private SrcSrvStreamBuilder mySrcSrvStreamBuilder;

  public PdbFilePatcher(final File homeDir, final SrcSrvStreamBuilder srcSrvStreamBuilder) {
    myHomeDir = homeDir;
    mySrcSrvStreamBuilder = srcSrvStreamBuilder;
  }

  public void patch(File symbolsFile, BuildProgressLogger buildLogger) throws Exception {
    final Collection<File> sourceFiles = mySrcToolExe.getReferencedSourceFiles(symbolsFile);
    if(sourceFiles.isEmpty()){
      final String message = "No source information found in pdb file " + symbolsFile.getCanonicalPath();
      buildLogger.warning(message);
      LOG.debug(message);
      return;
    }
    final File tmpFile = FileUtil.createTempFile(myHomeDir, "pdb-", ".patch", false);
    mySrcSrvStreamBuilder.dumpStreamToFile(tmpFile, sourceFiles);
    myPdbStrExe.doCommand(PdbStrExeCommand.WRITE, symbolsFile, tmpFile, PdbStrExe.SRCSRV_STREAM_NAME);
  }
}
