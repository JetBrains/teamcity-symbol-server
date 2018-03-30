package jetbrains.buildServer.symbols.tools;

import com.intellij.execution.configurations.GeneralCommandLine;
import jetbrains.buildServer.ExecResult;
import jetbrains.buildServer.SimpleCommandLineProcessRunner;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.util.CollectionsUtil;
import jetbrains.buildServer.util.Converter;
import jetbrains.buildServer.util.FileUtil;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * @author Evgeniy.Koshkin
 */
public class JetSymbolsExe {

  private static final Logger LOG = Logger.getLogger(JetSymbolsExe.class);
  private static final String SYMBOLS_EXE = "JetBrains.CommandLine.Symbols.exe";
  private static final String DUMP_SYMBOL_SIGN_CMD = "dumpSymbolSign";
  private static final String LIST_SOURCES_CMD = "listSources";
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
    buildLogger.message(String.format("Running command %s", commandLine.getCommandLineString()));
    final ExecResult execResult = SimpleCommandLineProcessRunner.runCommand(commandLine, null);
    final String stdout = execResult.getStdout();
    if(!stdout.isEmpty()){
      buildLogger.message("Stdout: " + stdout);
    }
    final int exitCode = execResult.getExitCode();
    if (exitCode != 0) {
      buildLogger.warning(String.format("%s ends with non-zero exit code %s.", SYMBOLS_EXE, execResult));
      buildLogger.warning("Stdout: " + stdout);
      buildLogger.warning("Stderr: " + execResult.getStderr());
      final Throwable exception = execResult.getException();
      if(exception != null){
        buildLogger.exception(exception);
      }
    }
    return exitCode;
  }

  public Collection<File> getReferencedSourceFiles(File symbolsFile) {
    final GeneralCommandLine commandLine = new GeneralCommandLine();
    commandLine.setExePath(myExePath.getPath());
    commandLine.addParameter(LIST_SOURCES_CMD);
    commandLine.addParameter(symbolsFile.getAbsolutePath());
    final ExecResult execResult = SimpleCommandLineProcessRunner.runCommand(commandLine, null);

    if (execResult.getExitCode() == 0) {
      return CollectionsUtil.convertAndFilterNulls(Arrays.asList(execResult.getOutLines()), new Converter<File, String>() {
        public File createFrom(@NotNull String source) {
          final File file = new File(source);
          if (file.isFile()) return file;
          return null; //last string is not a source file path
        }
      });
    }

    LOG.info("Failed to read references source in file " + symbolsFile +
      "\nStdout: " + execResult.getStdout() +
      "\nStderr: " + execResult.getStderr());

    return Collections.emptyList();
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
