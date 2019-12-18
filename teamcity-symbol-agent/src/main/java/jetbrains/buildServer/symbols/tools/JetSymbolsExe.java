package jetbrains.buildServer.symbols.tools;

import com.intellij.execution.configurations.GeneralCommandLine;
import jetbrains.buildServer.ExecResult;
import jetbrains.buildServer.SimpleCommandLineProcessRunner;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.util.CollectionsUtil;
import jetbrains.buildServer.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * @author Evgeniy.Koshkin
 */
public class JetSymbolsExe {

  private static final String SYMBOLS_EXE = "JetBrains.CommandLine.Symbols.exe";
  private static final String DUMP_SYMBOL_SIGN_CMD = "dumpSymbolSign";
  private static final String LIST_SOURCES_CMD = "listSources";
  private static final String GET_PDB_TYPE_CMD = "getPdbType";
  private static final String UPDATE_SOURCE_URLS_CMD = "updateSourceUrls";
  private final File myExePath;

  public JetSymbolsExe(File homeDir) {
    myExePath = new File(homeDir, SYMBOLS_EXE);
  }

  public int dumpPdbGuidsToFile(Collection<File> files, File output, BuildProgressLogger buildLogger) throws IOException {
    final GeneralCommandLine commandLine = new GeneralCommandLine();
    commandLine.setExePath(myExePath.getPath());
    commandLine.addParameter(DUMP_SYMBOL_SIGN_CMD);
    commandLine.addParameter(String.format("/o=%s", output.getPath()));
    commandLine.addParameter(String.format("/i=%s", dumpPathsToFile(files).getPath()));

    final ExecResult execResult = executeCommandLine(commandLine, buildLogger);
    if (execResult.getExitCode() == 0 && !execResult.getStdout().isEmpty()) {
      buildLogger.message("Stdout: " + execResult.getStdout());
    }

    return execResult.getExitCode();
  }

  public Collection<File> getReferencedSourceFiles(File symbolsFile, BuildProgressLogger buildLogger) {
    final GeneralCommandLine commandLine = new GeneralCommandLine();
    commandLine.setExePath(myExePath.getPath());
    commandLine.addParameter(LIST_SOURCES_CMD);
    commandLine.addParameter(symbolsFile.getAbsolutePath());

    final ExecResult execResult = executeCommandLine(commandLine, buildLogger);
    if (execResult.getExitCode() == 0) {
      return CollectionsUtil.convertAndFilterNulls(Arrays.asList(execResult.getOutLines()), File::new);
    } else {
      return Collections.emptyList();
    }
  }

  public PdbType getPdbType(File symbolsFile, BuildProgressLogger buildLogger) {
    final GeneralCommandLine commandLine = new GeneralCommandLine();
    commandLine.setExePath(myExePath.getPath());
    commandLine.addParameter(GET_PDB_TYPE_CMD);
    commandLine.addParameter(symbolsFile.getAbsolutePath());

    final ExecResult execResult = executeCommandLine(commandLine, buildLogger);
    if (execResult.getExitCode() == 0) {
      return parsePdbType(execResult.getOutLines(), buildLogger);
    } else {
      buildLogger.error("Cannot parse PDB type.");
      return PdbType.Undefined;
    }
  }

  public ExecResult updatePortablePdbSourceUrls(final File symbolsFile, final File sourceUrlsFile, final BuildProgressLogger buildLogger) {
    final GeneralCommandLine commandLine = new GeneralCommandLine();
    commandLine.setExePath(myExePath.getPath());
    commandLine.addParameter(UPDATE_SOURCE_URLS_CMD);
    commandLine.addParameter(symbolsFile.getAbsolutePath());
    commandLine.addParameter(String.format("/i=%s", sourceUrlsFile.getPath()));

    return executeCommandLine(commandLine, buildLogger);
  }

  private PdbType parsePdbType(final String[] output, final BuildProgressLogger buildLogger) {
    final StringBuilder outputBuilder = new StringBuilder();
    for (int index = 0; index < output.length; index++) {
      if (index > 0) {
        outputBuilder.append("\n");
      }
      outputBuilder.append(output[index]);
    }

    final String outputValue = outputBuilder.toString();
    final PdbType result = PdbType.parse(outputValue);
    if (result == PdbType.Undefined) {
      buildLogger.error(String.format("Cannot parse PDB type: %s", outputValue));
    }
    return result;
  }

  private ExecResult executeCommandLine(GeneralCommandLine commandLine, BuildProgressLogger buildLogger) {
    buildLogger.message(String.format("Running command %s", commandLine.getCommandLineString()));
    final ExecResult execResult = SimpleCommandLineProcessRunner.runCommand(commandLine, null);
    if (execResult.getExitCode() != 0) {
      buildLogger.warning(String.format("%s completed with exit code %s.", SYMBOLS_EXE, execResult));
      buildLogger.warning("Stdout: " + execResult.getStdout());
      buildLogger.warning("Stderr: " + execResult.getStderr());
      final Throwable exception = execResult.getException();
      if(exception != null){
        buildLogger.exception(exception);
      }
    }
    return execResult;
  }

  private File dumpPathsToFile(Collection<File> files) throws IOException {
    final File result = FileUtil.createTempFile(DUMP_SYMBOL_SIGN_CMD, ".input");
    StringBuilder contentBuilder = new StringBuilder();
    for(File file : files){
      contentBuilder.append(file.getPath()).append("\n");
    }
    FileUtil.writeToFile(result, contentBuilder.toString().getBytes());
    return result;
  }
}
