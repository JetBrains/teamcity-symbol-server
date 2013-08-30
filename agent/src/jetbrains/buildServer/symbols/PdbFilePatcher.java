package jetbrains.buildServer.symbols;

import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.symbols.tools.PdbStrExe;
import jetbrains.buildServer.symbols.tools.PdbStrExeCommands;
import jetbrains.buildServer.symbols.tools.SrcToolExe;
import jetbrains.buildServer.util.FileUtil;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Collection;

/**
 * @author Evgeniy.Koshkin
 */
public class PdbFilePatcher {

  private static final Logger LOG = Logger.getLogger(PdbFilePatcher.class);

  private final File myWorkingDir;
  private final SrcSrvStreamBuilder mySrcSrvStreamBuilder;
  private final PdbStrExe myPdbStrExe;
  private final SrcToolExe mySrcToolExe;

  public PdbFilePatcher(@NotNull final File workingDir, @NotNull final File srcSrvHomeDir, @NotNull final SrcSrvStreamBuilder srcSrvStreamBuilder) {
    myWorkingDir = workingDir;
    mySrcSrvStreamBuilder = srcSrvStreamBuilder;
    myPdbStrExe = new PdbStrExe(srcSrvHomeDir);
    mySrcToolExe = new SrcToolExe(srcSrvHomeDir);
  }

  public void patch(File symbolsFile, BuildProgressLogger buildLogger) throws Exception {
    final Collection<File> sourceFiles = mySrcToolExe.getReferencedSourceFiles(symbolsFile);
    if(sourceFiles.isEmpty()){
      final String message = "No source information found in pdb file " + symbolsFile.getCanonicalPath();
      buildLogger.warning(message);
      LOG.debug(message);
      return;
    }
    final File tmpFile = FileUtil.createTempFile(myWorkingDir, "pdb-", ".patch", false);
    mySrcSrvStreamBuilder.dumpStreamToFile(tmpFile, sourceFiles);
    myPdbStrExe.doCommand(PdbStrExeCommands.WRITE, symbolsFile, tmpFile, PdbStrExe.SRCSRV_STREAM_NAME);
  }
}
