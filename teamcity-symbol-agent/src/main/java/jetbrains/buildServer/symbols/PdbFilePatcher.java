package jetbrains.buildServer.symbols;

import jetbrains.buildServer.ExecResult;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.symbols.tools.JetSymbolsExe;
import jetbrains.buildServer.symbols.tools.PdbStrExe;
import jetbrains.buildServer.symbols.tools.PdbStrExeCommands;
import jetbrains.buildServer.util.FileUtil;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

/**
 * @author Evgeniy.Koshkin
 */
public class PdbFilePatcher {

  private static final Logger LOG = Logger.getLogger(PdbFilePatcher.class);
  private final File myWorkingDir;
  private final SrcSrvStreamBuilder mySrcSrvStreamBuilder;
  private final PdbStrExe myPdbStrExe;
  private final JetSymbolsExe myJetSymbolsExe;

  public PdbFilePatcher(@NotNull final File workingDir,
                        @NotNull final File srcSrvHomeDir,
                        @NotNull final SrcSrvStreamBuilder srcSrvStreamBuilder,
                        @NotNull final JetSymbolsExe jetSymbolsExe) {
    myWorkingDir = workingDir;
    mySrcSrvStreamBuilder = srcSrvStreamBuilder;
    myPdbStrExe = new PdbStrExe(srcSrvHomeDir);
    myJetSymbolsExe = jetSymbolsExe;
  }

  public void patch(File symbolsFile, BuildProgressLogger buildLogger) throws Exception {
    final Collection<File> sourceFiles = myJetSymbolsExe.getReferencedSourceFiles(symbolsFile, buildLogger);
    final String symbolsFileCanonicalPath = symbolsFile.getCanonicalPath();
    if (sourceFiles.isEmpty()) {
      final String message = "No source information found in pdb file " + symbolsFileCanonicalPath;
      buildLogger.warning(message);
      LOG.warn(message);
      return;
    }

    final File tmpFile = FileUtil.createTempFile(myWorkingDir, "pdb-", ".patch", false);
    try {
      int processedFilesCount = mySrcSrvStreamBuilder.dumpStreamToFile(tmpFile, sourceFiles);
      if (processedFilesCount == 0) {
        buildLogger.message(String.format("No local source files were found for pdb file %s. Looks like related binary file was not built during the current build.", symbolsFileCanonicalPath));
        return;
      } else {
        buildLogger.message(String.format("Information about %d source files will be updated.", processedFilesCount));
      }

      final ExecResult result = myPdbStrExe.doCommand(PdbStrExeCommands.WRITE, symbolsFile, tmpFile, PdbStrExe.SRCSRV_STREAM_NAME);
      if (result.getExitCode() != 0) {
        throw new IOException(String.format("Failed to update symbols file %s: %s", symbolsFile, result.getStderr()));
      }
    } finally {
      FileUtil.delete(tmpFile);
    }
  }
}