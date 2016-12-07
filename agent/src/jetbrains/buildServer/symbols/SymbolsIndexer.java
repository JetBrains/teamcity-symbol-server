package jetbrains.buildServer.symbols;

import jetbrains.buildServer.BuildProblemData;
import jetbrains.buildServer.BuildProblemTypes;
import jetbrains.buildServer.agent.*;
import jetbrains.buildServer.agent.artifacts.ArtifactsWatcher;
import jetbrains.buildServer.agent.impl.artifacts.ArtifactsBuilderAdapter;
import jetbrains.buildServer.agent.impl.artifacts.ArtifactsCollection;
import jetbrains.buildServer.agent.plugins.beans.PluginDescriptor;
import jetbrains.buildServer.symbols.tools.BinaryGuidDumper;
import jetbrains.buildServer.symbols.tools.JetSymbolsExe;
import jetbrains.buildServer.util.EventDispatcher;
import jetbrains.buildServer.util.FileUtil;
import org.apache.log4j.Logger;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static jetbrains.buildServer.dotNet.DotNetConstants.DOTNET_FRAMEWORK_3_5;

/**
 * @author Evgeniy.Koshkin
 */
public class SymbolsIndexer extends ArtifactsBuilderAdapter {

  private static final Logger LOG = Logger.getLogger(SymbolsIndexer.class);

  public static final String PDB_FILE_EXTENSION = "pdb";
  public static final String DLL_FILE_EXTENSION = "dll";
  public static final String EXE_FILE_EXTENSION = "exe";
  private static final String X64_SRCSRV = "\\x64\\srcsrv";
  private static final String X86_SRCSRV = "\\x86\\srcsrv";
  public static final String NET_35_NOT_FOUND_PROBLEM_IDENTITY = "net35symbolindexing";

  @NotNull private final ArtifactsWatcher myArtifactsWatcher;
  @NotNull private final ArtifactPathHelper myArtifactPathHelper;

  @NotNull private final JetSymbolsExe myJetSymbolsExe;
  @NotNull private final Map<File, String> myPdbFileToArtifactMapToProcess = new ConcurrentHashMap<File, String>();
  @NotNull private final Map<File, String> myBinaryFileToArtifactMapToProcess = new ConcurrentHashMap<File, String>();

  @Nullable private BuildProgressLogger myProgressLogger;
  @Nullable private File myBuildTempDirectory;
  @Nullable private File mySrcSrvHomeDir;
  @Nullable private FileUrlProvider myFileUrlProvider;

  public SymbolsIndexer(@NotNull final PluginDescriptor pluginDescriptor,
                        @NotNull final EventDispatcher<AgentLifeCycleListener> agentDispatcher,
                        @NotNull final ArtifactsWatcher artifactsWatcher,
                        @NotNull final ArtifactPathHelper artifactPathHelper) {
    myArtifactsWatcher = artifactsWatcher;
    myJetSymbolsExe = new JetSymbolsExe(new File(pluginDescriptor.getPluginRoot(), "bin"));
    myArtifactPathHelper = artifactPathHelper;

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

        checkAndReportRuntimeRequirements(runningBuild.getAgentConfiguration(), myProgressLogger);

        mySrcSrvHomeDir = getSrcSrvHomeDir(runningBuild.getAgentConfiguration());
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
        if (myPdbFileToArtifactMapToProcess.isEmpty()) {
          myProgressLogger.warning("Symbols weren't found in artifacts to be published.");
          LOG.debug("Symbols weren't found in artifacts to be published for build with id " + build.getBuildId());
        } else {
          myProgressLogger.message("Collecting symbol files signatures.");
          LOG.debug("Collecting symbol files signatures.");
          try {
            final Set<PdbSignatureIndexEntry> signatureLocalFilesData = getPdbSignatures(myPdbFileToArtifactMapToProcess.keySet());
            if (signatureLocalFilesData.isEmpty()) {
              LOG.warn("No information was collected about symbol files signatures");
              myProgressLogger.warning("No information was collected about symbol files signatures");
            } else {
              final Set<PdbSignatureIndexEntry> indexData = getSignatureIndexEntries(signatureLocalFilesData, myPdbFileToArtifactMapToProcess);
              final File indexDataFile = FileUtil.createTempFile(myBuildTempDirectory, SymbolsConstants.SYMBOL_SIGNATURES_FILE_NAME_PREFIX, ".xml", false);
              PdbSignatureIndexUtil.write(new FileOutputStream(indexDataFile), indexData);
              myProgressLogger.message("Publishing collected symbol files signatures.");
              myArtifactsWatcher.addNewArtifactsPath(indexDataFile + "=>" + ".teamcity/symbols");
            }
          } catch (Exception e) {
            LOG.error("Error while dumping symbols/binaries signatures for build with id " + build.getBuildId(), e);
            myProgressLogger.error("Error while dumping symbols/binaries signatures.");
            myProgressLogger.exception(e);
          }
        }
        myPdbFileToArtifactMapToProcess.clear();

        if (myBinaryFileToArtifactMapToProcess.isEmpty()) {
          myProgressLogger.warning("Binaries weren't found in artifacts to be published.");
          LOG.debug("Binaries weren't found in artifacts to be published for build with id " + build.getBuildId());
        } else {
          myProgressLogger.message("Collecting binary files signatures.");
          LOG.debug("Collecting binary files signatures.");
          try {
            final Set<PdbSignatureIndexEntry> signatureLocalFilesData = getBinarySignatures(myBinaryFileToArtifactMapToProcess.keySet());
            if (signatureLocalFilesData.isEmpty()) {
              LOG.warn("No information was collected about binary files signatures");
              myProgressLogger.warning("No information was collected about binary files signatures");
            } else {
              final Set<PdbSignatureIndexEntry> indexData = getSignatureIndexEntries(signatureLocalFilesData, myBinaryFileToArtifactMapToProcess);
              final File indexDataFile = FileUtil.createTempFile(myBuildTempDirectory, SymbolsConstants.BINARY_SIGNATURES_FILE_NAME_PREFIX, ".xml", false);
              PdbSignatureIndexUtil.write(new FileOutputStream(indexDataFile), indexData);
              myProgressLogger.message("Publishing collected binary files signatures.");
              myArtifactsWatcher.addNewArtifactsPath(indexDataFile + "=>" + ".teamcity/symbols");
            }
          } catch (Exception e) {
            LOG.error("Error while dumping symbols/binaries signatures for build with id " + build.getBuildId(), e);
            myProgressLogger.error("Error while dumping symbols/binaries signatures.");
            myProgressLogger.exception(e);
          }
        }
        myBinaryFileToArtifactMapToProcess.clear();
      }

      @NotNull
      private Set<PdbSignatureIndexEntry> getSignatureIndexEntries(Set<PdbSignatureIndexEntry> signatureLocalFilesData, Map<File, String> artifactMap) {
        final Set<PdbSignatureIndexEntry> indexData = new HashSet<PdbSignatureIndexEntry>();
        for(PdbSignatureIndexEntry signatureIndexEntry : signatureLocalFilesData){
          final String artifactPath = signatureIndexEntry.getArtifactPath();
          if(artifactPath == null) continue;
          final File targetFile = new File(artifactPath);
          if(artifactMap.containsKey(targetFile)) {
            indexData.add(new PdbSignatureIndexEntry(signatureIndexEntry.getGuid(), targetFile.getName(), artifactMap.get(targetFile)));
          }
        }
        return indexData;
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
    final Map<File, String> pdbFiles = getArtifactPathsByFileExtension(artifacts, PDB_FILE_EXTENSION);

    final PdbFilePatcher pdbFilePatcher = new PdbFilePatcher(myBuildTempDirectory, mySrcSrvHomeDir, new SrcSrvStreamBuilder(myFileUrlProvider, myProgressLogger));
    for(File pdbFile : pdbFiles.keySet()){
      if(myPdbFileToArtifactMapToProcess.containsKey(pdbFile)){
        LOG.debug(String.format("File %s already processed. Skipped.", pdbFile.getAbsolutePath()));
        continue;
      }
      try {
        myProgressLogger.message("Indexing sources appeared in file " + pdbFile.getAbsolutePath());
        pdbFilePatcher.patch(pdbFile, myProgressLogger);
        myPdbFileToArtifactMapToProcess.put(pdbFile, myArtifactPathHelper.concatenateArtifactPath(pdbFiles.get(pdbFile), pdbFile.getName()));
      } catch (Throwable e) {
        LOG.error("Error occurred while patching symbols file " + pdbFile, e);
        myProgressLogger.error("Error occurred while patching symbols file " + pdbFile);
        myProgressLogger.exception(e);
      }
    }

    LOG.debug("Searching for binary files in publishing artifacts.");
    getBinaryArtifacts(artifacts, EXE_FILE_EXTENSION);
    getBinaryArtifacts(artifacts, DLL_FILE_EXTENSION);
  }

  private void getBinaryArtifacts(@NotNull List<ArtifactsCollection> artifacts, String fileExtension) {
    final Map<File, String> binaryFiles = getArtifactPathsByFileExtension(artifacts, fileExtension);
    for (File binaryFile : binaryFiles.keySet()){
      if(myBinaryFileToArtifactMapToProcess.containsKey(binaryFile)){
        LOG.debug(String.format("File %s already processed. Skipped.", binaryFile.getAbsolutePath()));
        continue;
      }
      myBinaryFileToArtifactMapToProcess.put(binaryFile, myArtifactPathHelper.concatenateArtifactPath(binaryFiles.get(binaryFile), binaryFile.getName()));
    }
  }

  private Set<PdbSignatureIndexEntry> getPdbSignatures(Collection<File> files) throws IOException, JDOMException {
    final File guidDumpFile = FileUtil.createTempFile(myBuildTempDirectory, "symbol-signatures-local-", ".xml", false);
    myJetSymbolsExe.dumpPdbGuidsToFile(files, guidDumpFile, myProgressLogger);
    return getSignaturesFromDumpXml(guidDumpFile);
  }

  private Set<PdbSignatureIndexEntry> getBinarySignatures(Collection<File> files) throws IOException, JDOMException {
    final File guidDumpFile = FileUtil.createTempFile(myBuildTempDirectory, "binary-signatures-local-", ".xml", false);
    BinaryGuidDumper.dumpBinaryGuidsToFile(files, guidDumpFile, myProgressLogger);
    return getSignaturesFromDumpXml(guidDumpFile);
  }

  @NotNull
  private Set<PdbSignatureIndexEntry> getSignaturesFromDumpXml(File guidDumpFile) throws JDOMException, IOException {
    if(guidDumpFile.exists()){
      myArtifactsWatcher.addNewArtifactsPath(guidDumpFile + "=>" + ".teamcity/symbols");
    }
    if(guidDumpFile.isFile())
      return PdbSignatureIndexUtil.read(new FileInputStream(guidDumpFile), true);
    else
      return Collections.emptySet();
  }

  @Nullable
  private File getSrcSrvHomeDir(@NotNull BuildAgentConfiguration agentConfiguration) {
    final Map<String,String> agentConfigParameters = agentConfiguration.getConfigurationParameters();
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

  private Map<File, String> getArtifactPathsByFileExtension(List<ArtifactsCollection> artifactsCollections, String fileExtension){
    final Map<File, String> result = new HashMap<File, String>();
    for(ArtifactsCollection artifactsCollection : artifactsCollections){
      if(artifactsCollection.isEmpty()) continue;
      final Map<File, String> filePathMap = artifactsCollection.getFilePathMap();
      for (final File artifact : filePathMap.keySet()){
        if(FileUtil.getExtension(artifact.getPath()).equalsIgnoreCase(fileExtension))
          result.put(artifact, filePathMap.get(artifact));
      }
    }
    return result;
  }

  private boolean isIndexingApplicable() {
    return myFileUrlProvider != null && mySrcSrvHomeDir != null;
  }

  private static void checkAndReportRuntimeRequirements(@NotNull BuildAgentConfiguration agentConfiguration, @NotNull BuildProgressLogger logger){
    for (String parameterName : agentConfiguration.getConfigurationParameters().keySet()){
      if(parameterName.startsWith(DOTNET_FRAMEWORK_3_5)) return;
    }
    logger.logBuildProblem(BuildProblemData.createBuildProblem(NET_35_NOT_FOUND_PROBLEM_IDENTITY, BuildProblemTypes.TC_ERROR_MESSAGE_TYPE, ".NET 3.5 runtime required for symbols indexing was not found on build agent."));
  }
}
