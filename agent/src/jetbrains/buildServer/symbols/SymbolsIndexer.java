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
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Evgeniy.Koshkin
 */
public class SymbolsIndexer extends ArtifactsBuilderAdapter {

  private static final Logger LOG = Logger.getLogger(SymbolsIndexer.class);

  public static final String PDB_FILE_EXTENSION = "pdb";

  @NotNull private final ArtifactsWatcher myArtifactsWatcher;
  @NotNull private final JetSymbolsExe myJetSymbolsExe;
  @Nullable private AgentRunningBuild myBuild;
  @Nullable private Collection<File> mySymbolsToProcess;

  public SymbolsIndexer(@NotNull final PluginDescriptor pluginDescriptor, @NotNull final EventDispatcher<AgentLifeCycleListener> agentDispatcher, @NotNull final ArtifactsWatcher artifactsWatcher) {
    myArtifactsWatcher = artifactsWatcher;
    myJetSymbolsExe = new JetSymbolsExe(new File(pluginDescriptor.getPluginRoot(), "bin"));

    agentDispatcher.addListener(new AgentLifeCycleAdapter() {
      @Override
      public void buildStarted(@NotNull final AgentRunningBuild runningBuild) {
        myBuild = runningBuild;
        mySymbolsToProcess = new CopyOnWriteArrayList<File>();
      }

      @Override
      public void afterAtrifactsPublished(@NotNull AgentRunningBuild build, @NotNull BuildFinishedStatus buildStatus) {
        super.afterAtrifactsPublished(build, buildStatus);
        if (myBuild == null || mySymbolsToProcess == null) return;
        if(myBuild.getBuildFeaturesOfType(SymbolsConstants.BUILD_FEATURE_TYPE).isEmpty()) return;

        if (mySymbolsToProcess.isEmpty()) {
          myBuild.getBuildLogger().warning("Symbols weren't found in artifacts to be published.");
          LOG.debug("Symbols weren't found in artifacts to be published.");
        } else {
          try {
            final File symbolSignaturesFile = FileUtil.createTempFile(myBuild.getBuildTempDirectory(), "symbol-signatures-", ".xml", false);
            myJetSymbolsExe.dumpGuidsToFile(mySymbolsToProcess, symbolSignaturesFile,  myBuild.getBuildLogger());
            if(symbolSignaturesFile.exists()){
              myArtifactsWatcher.addNewArtifactsPath(symbolSignaturesFile + "=>" + ".teamcity/symbols");
            }
          } catch (IOException e) {
            LOG.error("Error while dumping symbols/binaries signatures.", e);
            myBuild.getBuildLogger().error("Error while dumping symbols/binaries signatures.");
            myBuild.getBuildLogger().exception(e);
          }
        }
        mySymbolsToProcess = null;
        myBuild = null;
      }
    });
  }

  @Override
  public void afterCollectingFiles(@NotNull List<ArtifactsCollection> artifacts) {
    super.afterCollectingFiles(artifacts);
    if(myBuild == null || mySymbolsToProcess == null) return;
    final BuildProgressLogger buildLogger = myBuild.getBuildLogger();
    if(myBuild.getBuildFeaturesOfType(SymbolsConstants.BUILD_FEATURE_TYPE).isEmpty()){
      LOG.debug(SymbolsConstants.BUILD_FEATURE_TYPE + " build feature disabled. No indexing performed.");
      return;
    }
    LOG.debug(SymbolsConstants.BUILD_FEATURE_TYPE + " build feature enabled. Searching for suitable files.");
    final Collection<File> pdbFiles = getArtifactPathsByFileExtension(artifacts, PDB_FILE_EXTENSION);
    if(pdbFiles.isEmpty()) return;

    final FileUrlProvider urlProvider = FileUrlProviderFactory.getProvider(myBuild, buildLogger);
    if(urlProvider == null) return;

    final PdbFilePatcher pdbFilePatcher = new PdbFilePatcher(myBuild.getBuildTempDirectory(), new SrcSrvStreamBuilder(urlProvider));
    for(File pdbFile : pdbFiles){
      if(mySymbolsToProcess.contains(pdbFile)){
        LOG.debug(String.format("File %s already processed. Skipped.", pdbFile.getAbsolutePath()));
        continue;
      }
      try {
        buildLogger.message("Indexing sources appeared in file " + pdbFile.getAbsolutePath());
        pdbFilePatcher.patch(pdbFile, buildLogger);
        mySymbolsToProcess.add(pdbFile);
      } catch (Throwable e) {
        LOG.error("Error occurred while patching symbols file " + pdbFile, e);
        buildLogger.error("Error occurred while patching symbols file " + pdbFile);
        buildLogger.exception(e);
      }
    }
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
}
