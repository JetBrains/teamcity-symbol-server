package jetbrains.buildServer.symbols;

import jetbrains.buildServer.agent.*;
import jetbrains.buildServer.agent.artifacts.ArtifactsWatcher;
import jetbrains.buildServer.agent.impl.artifacts.ArtifactsBuilderAdapter;
import jetbrains.buildServer.agent.impl.artifacts.ArtifactsCollection;
import jetbrains.buildServer.agent.plugins.beans.PluginDescriptor;
import jetbrains.buildServer.symbols.tools.JetSymbolsExe;
import jetbrains.buildServer.util.EventDispatcher;
import jetbrains.buildServer.util.FileUtil;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Evgeniy.Koshkin
 */
public class SymbolsIndexer extends ArtifactsBuilderAdapter {

  private static final Logger LOG = Logger.getLogger(SymbolsIndexer.class);

  public static final String PDB_FILE_EXTENSION = "pdb";
  private static final String X64_SRCSRV = "\\x64\\srcsrv";
  private static final String X86_SRCSRV = "\\x86\\srcsrv";

  @NotNull private final ArtifactsWatcher myArtifactsWatcher;
  @NotNull private final JetSymbolsExe myJetSymbolsExe;
  @NotNull private final Collection<File> mySymbolsToProcess = new CopyOnWriteArrayList<File>();

  @Nullable private BuildProgressLogger myProgressLogger;
  @Nullable private File myBuildTempDirectory;
  @Nullable private File mySrcSrvHomeDir;
  @Nullable private FileUrlProvider myFileUrlProvider;

  public SymbolsIndexer(@NotNull final PluginDescriptor pluginDescriptor, @NotNull final EventDispatcher<AgentLifeCycleListener> agentDispatcher, @NotNull final ArtifactsWatcher artifactsWatcher) {
    myArtifactsWatcher = artifactsWatcher;
    myJetSymbolsExe = new JetSymbolsExe(new File(pluginDescriptor.getPluginRoot(), "bin"));

    agentDispatcher.addListener(new AgentLifeCycleAdapter() {
      @Override
      public void buildStarted(@NotNull final AgentRunningBuild runningBuild) {
        final long buildId = runningBuild.getBuildId();
        if(runningBuild.getBuildFeaturesOfType(SymbolsConstants.BUILD_FEATURE_TYPE).isEmpty()){
          LOG.debug(SymbolsConstants.BUILD_FEATURE_TYPE + " build feature disabled. No indexing will be performed for build with id " + buildId);
          return;
        }
        LOG.debug(SymbolsConstants.BUILD_FEATURE_TYPE + " build feature enabled for build with id " + buildId);

        myProgressLogger = runningBuild.getBuildLogger();
        myBuildTempDirectory = runningBuild.getBuildTempDirectory();

        mySrcSrvHomeDir = getSrcSrvHomeDir(runningBuild);
        if (mySrcSrvHomeDir == null) {
          LOG.error("Failed to find Source Server tools home directory. No symbol and source indexing will be performed for build with id " + buildId);
          myProgressLogger.error("Failed to find Source Server tools home directory. No symbol and source indexing will be performed.");
          return;
        }
        LOG.debug("Source Server tools home directory located. " + mySrcSrvHomeDir.getAbsolutePath());
        myProgressLogger.message("Source Server tools home directory located. " + mySrcSrvHomeDir.getAbsolutePath());

        myFileUrlProvider = FileUrlProviderFactory.getProvider(runningBuild, myProgressLogger);
      }

      @Override
      public void afterAtrifactsPublished(@NotNull AgentRunningBuild build, @NotNull BuildFinishedStatus buildStatus) {
        super.afterAtrifactsPublished(build, buildStatus);
        if(!isIndexingApplicable()) return;
        if (mySymbolsToProcess.isEmpty()) {
          myProgressLogger.warning("Symbols weren't found in artifacts to be published.");
          LOG.debug("Symbols weren't found in artifacts to be published for build with id " + build.getBuildId());
        } else {
          myProgressLogger.message("Collecting symbol files signatures.");
          LOG.debug("Collecting symbol files signatures.");
          try {
            final File symbolSignaturesFile = FileUtil.createTempFile(myBuildTempDirectory, "symbol-signatures-", ".xml", false);
            myJetSymbolsExe.dumpGuidsToFile(mySymbolsToProcess, symbolSignaturesFile, myProgressLogger);
            if(symbolSignaturesFile.exists()){
              myProgressLogger.message("Publishing collected symbol files signatures.");
              myArtifactsWatcher.addNewArtifactsPath(symbolSignaturesFile + "=>" + ".teamcity/symbols");
            }
          } catch (IOException e) {
            LOG.error("Error while dumping symbols/binaries signatures for build with id " + build.getBuildId(), e);
            myProgressLogger.error("Error while dumping symbols/binaries signatures.");
            myProgressLogger.exception(e);
          }
        }
        mySymbolsToProcess.clear();
      }
    });
  }

  @Override
  public void afterCollectingFiles(@NotNull List<ArtifactsCollection> artifacts) {
    super.afterCollectingFiles(artifacts);
    if(!isIndexingApplicable()){
      LOG.debug("Symbols and sources indexing skipped.");
      return;
    }

    LOG.debug("Searching for symbol files in publishing artifacts.");
    final Collection<File> pdbFiles = getArtifactPathsByFileExtension(artifacts, PDB_FILE_EXTENSION);
    if(pdbFiles.isEmpty()) return;

    final PdbFilePatcher pdbFilePatcher = new PdbFilePatcher(myBuildTempDirectory, mySrcSrvHomeDir, new SrcSrvStreamBuilder(myFileUrlProvider, myProgressLogger));
    for(File pdbFile : pdbFiles){
      if(mySymbolsToProcess.contains(pdbFile)){
        LOG.debug(String.format("File %s already processed. Skipped.", pdbFile.getAbsolutePath()));
        continue;
      }
      try {
        myProgressLogger.message("Indexing sources appeared in file " + pdbFile.getAbsolutePath());
        pdbFilePatcher.patch(pdbFile, myProgressLogger);
        mySymbolsToProcess.add(pdbFile);
      } catch (Throwable e) {
        LOG.error("Error occurred while patching symbols file " + pdbFile, e);
        myProgressLogger.error("Error occurred while patching symbols file " + pdbFile);
        myProgressLogger.exception(e);
      }
    }
  }

  @Nullable
  private File getSrcSrvHomeDir(@NotNull AgentRunningBuild build) {
    final Map<String,String> agentConfigParameters = build.getAgentConfiguration().getConfigurationParameters();
    String winDbgHomeDir = agentConfigParameters.get(WinDbgToolsDetector.WIN_DBG_PATH);
    if(winDbgHomeDir == null){
      LOG.debug("WinDbg tools are not mentioned in agent configuration.");
      return null;
    }
    File srcSrvHomeDir = new File(winDbgHomeDir, X64_SRCSRV);
    if (srcSrvHomeDir.isDirectory()) return srcSrvHomeDir;
    srcSrvHomeDir = new File(winDbgHomeDir, X86_SRCSRV);
    if (srcSrvHomeDir.isDirectory()) return srcSrvHomeDir;
    LOG.debug("Failed to find Source Server tools home directory under WinDbg tools home directory detected on path " + winDbgHomeDir);
    return null;
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

  private boolean isIndexingApplicable() {
    return myFileUrlProvider != null && mySrcSrvHomeDir != null;
  }
}
