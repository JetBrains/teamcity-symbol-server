package jetbrains.buildServer.symbols;

import com.intellij.execution.configurations.GeneralCommandLine;
import jetbrains.buildServer.ExecResult;
import jetbrains.buildServer.SimpleCommandLineProcessRunner;
import jetbrains.buildServer.agent.*;
import jetbrains.buildServer.agent.artifacts.ArtifactsWatcher;
import jetbrains.buildServer.agent.impl.artifacts.ArtifactsBuilderAdapter;
import jetbrains.buildServer.agent.impl.artifacts.ArtifactsCollection;
import jetbrains.buildServer.agent.plugins.beans.PluginDescriptor;
import jetbrains.buildServer.util.EventDispatcher;
import jetbrains.buildServer.util.FileUtil;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @author Evgeniy.Koshkin
 */
public class SymbolsIndexer extends ArtifactsBuilderAdapter {

  private static final Logger LOG = Logger.getLogger(SymbolsIndexer.class);

  public static final String PDB_FILE_EXTENSION = "pdb";
  public static final String EXE_FILE_EXTENSION = "exe";
  public static final String DLL_FILE_EXTENSION = "dll";

  public static final String SYMBOLS_EXE = "JetBrains.CommandLine.Symbols.exe";
  public static final String DUMP_SYMBOL_SIGN_CMD = "dumpSymbolSign";
  public static final String DUMP_BIN_SIGN_CMD = "dumpBinSign";

  @NotNull private final ArtifactsWatcher myArtifactsWatcher;
  @NotNull private final File myNativeToolPath;
  @Nullable private AgentRunningBuild myBuild;
  @Nullable private Collection<File> myBinariesToProcess;
  @Nullable private Collection<File> mySymbolsToProcess;

  public SymbolsIndexer(@NotNull final PluginDescriptor pluginDescriptor, @NotNull final EventDispatcher<AgentLifeCycleListener> agentDispatcher, @NotNull final ArtifactsWatcher artifactsWatcher) {
    myNativeToolPath = new File(new File(pluginDescriptor.getPluginRoot(), "bin"), SYMBOLS_EXE);
    myArtifactsWatcher = artifactsWatcher;
    agentDispatcher.addListener(new AgentLifeCycleAdapter() {
      @Override
      public void buildStarted(@NotNull final AgentRunningBuild runningBuild) {
        myBuild = runningBuild;
        myBinariesToProcess = new HashSet<File>();
        mySymbolsToProcess = new HashSet<File>();
      }

      @Override
      public void afterAtrifactsPublished(@NotNull AgentRunningBuild build, @NotNull BuildFinishedStatus buildStatus) {
        super.afterAtrifactsPublished(build, buildStatus);
        if (myBuild == null || mySymbolsToProcess == null || myBinariesToProcess == null) return;
        if(myBuild.getBuildFeaturesOfType(SymbolsConstants.BUILD_FEATURE_TYPE).isEmpty()) return;

        if (mySymbolsToProcess.isEmpty()) {
          myBuild.getBuildLogger().warning("Symbols weren't found in artifacts to be published.");
          LOG.debug("Symbols weren't found in artifacts to be published.");
        } else {
          final File targetDir = myBuild.getBuildTempDirectory();
          try {
            final File symbolSignaturesFile = dumpSymbolSignatures(mySymbolsToProcess, targetDir, myBuild.getBuildLogger());
            if(symbolSignaturesFile.exists()){
              myArtifactsWatcher.addNewArtifactsPath(symbolSignaturesFile + "=>" + ".teamcity/symbols");
            }
            final File binariesSignaturesFile = dumpBinarySignatures(myBinariesToProcess, targetDir, myBuild.getBuildLogger());
            if(binariesSignaturesFile.exists()){
              myArtifactsWatcher.addNewArtifactsPath(binariesSignaturesFile + "=>" + ".teamcity/symbols");
            }
          } catch (IOException e) {
            LOG.error("Error while dumping symbols/binaries signatures.", e);
            myBuild.getBuildLogger().error("Error while dumping symbols/binaries signatures.");
            myBuild.getBuildLogger().exception(e);
          }
        }
        mySymbolsToProcess = null;
        myBinariesToProcess = null;
        myBuild = null;
      }
    });
  }

  @Override
  public void afterCollectingFiles(@NotNull List<ArtifactsCollection> artifacts) {
    super.afterCollectingFiles(artifacts);
    if(myBuild == null || mySymbolsToProcess == null || myBinariesToProcess == null) return;
    if(myBuild.getBuildFeaturesOfType(SymbolsConstants.BUILD_FEATURE_TYPE).isEmpty()){
      myBuild.getBuildLogger().warning(SymbolsConstants.BUILD_FEATURE_TYPE + " build feature disabled. No indexing performed.");
      LOG.debug(SymbolsConstants.BUILD_FEATURE_TYPE + " build feature disabled. No indexing performed.");
      return;
    }
    myBuild.getBuildLogger().message(SymbolsConstants.BUILD_FEATURE_TYPE + " build feature enabled. Searching for suitable files.");
    LOG.debug(SymbolsConstants.BUILD_FEATURE_TYPE + " build feature enabled. Searching for suitable files.");
    mySymbolsToProcess.addAll(getArtifactPathsByFileExtension(artifacts, PDB_FILE_EXTENSION));
    myBinariesToProcess.addAll(getArtifactPathsByFileExtension(artifacts, EXE_FILE_EXTENSION));
    myBinariesToProcess.addAll(getArtifactPathsByFileExtension(artifacts, DLL_FILE_EXTENSION));
  }

  private Collection<File> getArtifactPathsByFileExtension(List<ArtifactsCollection> artifactsCollections, String fileExtension){
    final Collection<File> result = new HashSet<File>();
    for(ArtifactsCollection artifactsCollection : artifactsCollections){
      if(artifactsCollection.isEmpty()) continue;
      for (File artifact : artifactsCollection.getFilePathMap().keySet()){
        if(FileUtil.getExtension(artifact.getPath()).equalsIgnoreCase(fileExtension))
          result.add(artifact);
      }
    }
    return result;
  }

  private File dumpSymbolSignatures(Collection<File> files, File targetDir, BuildProgressLogger buildLogger) throws IOException {
    final File tempFile = FileUtil.createTempFile(targetDir, "symbol-signatures-", ".xml", false);
    runTool(DUMP_SYMBOL_SIGN_CMD, files, tempFile, buildLogger);
    return tempFile;
  }

  private File dumpBinarySignatures(Collection<File> files, File targetDir, BuildProgressLogger buildLogger) throws IOException {
    final File tempFile = FileUtil.createTempFile(targetDir, "binary-signatures-", ".xml", false);
    runTool(DUMP_BIN_SIGN_CMD, files, tempFile, buildLogger);
    return tempFile;
  }

  private void runTool(String cmd, Collection<File> files, File output, BuildProgressLogger buildLogger){
    final GeneralCommandLine commandLine = new GeneralCommandLine();
    commandLine.setExePath(myNativeToolPath.getPath());
    commandLine.addParameter(cmd);
    commandLine.addParameter(String.format("/o=\"%s\"", output.getPath()));
    for(File file : files){
      commandLine.addParameter(file.getPath());
    }
    buildLogger.message(String.format("Running command %s", commandLine.getCommandLineString()));
    final ExecResult execResult = SimpleCommandLineProcessRunner.runCommand(commandLine, null);
    buildLogger.message(execResult.getStdout());
    if (execResult.getExitCode() == 0) return;
    buildLogger.warning(String.format("%s ends with non-zero exit code.", SYMBOLS_EXE));
    buildLogger.warning(execResult.getStderr());
  }
}
