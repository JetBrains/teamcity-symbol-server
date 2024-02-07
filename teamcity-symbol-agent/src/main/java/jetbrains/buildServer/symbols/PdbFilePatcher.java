

package jetbrains.buildServer.symbols;

import jetbrains.buildServer.ExecResult;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.symbols.tools.JetSymbolsExe;
import jetbrains.buildServer.symbols.tools.PdbType;
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
  private final JetSymbolsExe myJetSymbolsExe;
  private final BuildProgressLogger myProgressLogger;
  private final PdbFilePatcherAdapterFactory myPatcheAdapterFactory;

  public PdbFilePatcher(@NotNull final File workingDir,
                        @NotNull final JetSymbolsExe jetSymbolsExe,
                        @NotNull final PdbFilePatcherAdapterFactory patcheAdapterFactory,
                        @NotNull final BuildProgressLogger progressLogger) {
    myWorkingDir = workingDir;
    myPatcheAdapterFactory = patcheAdapterFactory;
    myJetSymbolsExe = jetSymbolsExe;
    myProgressLogger = progressLogger;
  }

  /**
   * Executes patching process.
   *
   * @param symbolsFile is a source PDB file.
   * @param buildLogger is a build logger.
   * @return true if file was patched, otherwise false.
   * @throws Exception is error has happen during patching process.
   */
  public boolean patch(File symbolsFile) throws Exception {
    final PdbType pdbType = myJetSymbolsExe.getPdbType(symbolsFile, myProgressLogger);
    final PdbFilePatcherAdapter patherAdapter = myPatcheAdapterFactory.create(pdbType);

    final Collection<File> sourceFiles = patherAdapter.getReferencedSourceFiles(symbolsFile);
    final String symbolsFileCanonicalPath = symbolsFile.getCanonicalPath();
    if (sourceFiles.isEmpty()) {
      final String message = "No source information found in pdb file " + symbolsFileCanonicalPath;
      myProgressLogger.warning(message);
      LOG.warn(message);
      return false;
    }

    final File tmpFile = FileUtil.createTempFile(myWorkingDir, "pdb-", ".patch", false);
    try {
      int processedFilesCount = patherAdapter.serializeSourceLinks(tmpFile, sourceFiles);
      if (processedFilesCount == 0) {
        myProgressLogger.message(String.format("No local source files were found for pdb file %s. Looks like it was not produced during the current build.", symbolsFileCanonicalPath));
        return false;
      } else {
        myProgressLogger.message(String.format("Information about %d source files will be updated.", processedFilesCount));
      }

      final ExecResult result = patherAdapter.updatePdbSourceLinks(symbolsFile, tmpFile);
      if (result.getExitCode() != 0) {
        throw new IOException(String.format("Failed to update symbols file %s: %s", symbolsFile, result.getStderr()));
      }
    } finally {
      FileUtil.delete(tmpFile);
    }
    return true;
  }
}
