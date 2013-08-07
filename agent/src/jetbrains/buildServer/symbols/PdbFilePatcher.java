package jetbrains.buildServer.symbols;

import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.symbols.tools.PdbStrExe;
import jetbrains.buildServer.symbols.tools.PdbStrExeCommands;
import jetbrains.buildServer.symbols.tools.SrcToolExe;
import jetbrains.buildServer.util.FileUtil;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.Collection;

/**
 * @author Evgeniy.Koshkin
 */
public class PdbFilePatcher {

  private static final Logger LOG = Logger.getLogger(PdbFilePatcher.class);
  private static final File TOOLS_HOME_DIR = new File("c:\\Program Files (x86)\\Windows Kits\\8.0\\Debuggers\\x64\\srcsrv\\");

  private final PdbStrExe myPdbStrExe = new PdbStrExe(TOOLS_HOME_DIR);
  private final SrcToolExe mySrcToolExe = new SrcToolExe(TOOLS_HOME_DIR);

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
    myPdbStrExe.doCommand(PdbStrExeCommands.WRITE, symbolsFile, tmpFile, PdbStrExe.SRCSRV_STREAM_NAME);
  }
}
