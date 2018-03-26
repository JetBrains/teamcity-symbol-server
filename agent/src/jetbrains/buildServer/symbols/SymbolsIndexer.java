package jetbrains.buildServer.symbols;

import com.intellij.util.containers.ConcurrentHashSet;
import jetbrains.buildServer.BuildProblemData;
import jetbrains.buildServer.BuildProblemTypes;
import jetbrains.buildServer.agent.*;
import jetbrains.buildServer.agent.artifacts.ArtifactsWatcher;
import jetbrains.buildServer.agent.impl.artifacts.ArtifactsBuilderAdapter;
import jetbrains.buildServer.agent.impl.artifacts.ArtifactsCollection;
import jetbrains.buildServer.agent.plugins.beans.PluginDescriptor;
import jetbrains.buildServer.dotNet.DotNetConstants;
import jetbrains.buildServer.symbols.tools.BinaryGuidDumper;
import jetbrains.buildServer.symbols.tools.JetSymbolsExe;
import jetbrains.buildServer.util.EventDispatcher;
import jetbrains.buildServer.util.FileUtil;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * @author Evgeniy.Koshkin
 */
public class SymbolsIndexer extends ArtifactsBuilderAdapter {

  private static final Logger LOG = Logger.getLogger(SymbolsIndexer.class);

  private static final String PDB_FILE_EXTENSION = "pdb";
  private static final String DLL_FILE_EXTENSION = "dll";
  private static final String EXE_FILE_EXTENSION = "exe";
  private static final String X64_SRCSRV = "\\x64\\srcsrv";
  private static final String X86_SRCSRV = "\\x86\\srcsrv";
  private static final String NET_45_NOT_FOUND_PROBLEM_IDENTITY = "net45symbolindexing";
  private static final Pattern NET_4X_PATTERN = Pattern.compile(
          String.format("%s\\.\\d+.*", DotNetConstants.DOTNET_FRAMEWORK_4),
          Pattern.CASE_INSENSITIVE
  );

  @NotNull private final ArtifactsWatcher myArtifactsWatcher;
  @NotNull private final ArtifactPathHelper myArtifactPathHelper;

  @NotNull private final JetSymbolsExe myJetSymbolsExe;

  @NotNull private final Map<File, String> myPdbFileToArtifactMap = new ConcurrentHashMap<File, String>();
  @NotNull private final Set<PdbSignatureIndexEntry> myPdbFileSignatures = new ConcurrentHashSet<PdbSignatureIndexEntry>();

  @NotNull private final Map<File, String> myBinaryFileToArtifactMap = new ConcurrentHashMap<File, String>();
  @NotNull private final Set<PdbSignatureIndexEntry> myBinaryFileSignatures = new ConcurrentHashSet<PdbSignatureIndexEntry>();

  @Nullable private BuildProgressLogger myProgressLogger;
  @Nullable private File myBuildTempDirectory;
  @Nullable private File mySrcSrvHomeDir;
  @Nullable private FileUrlProvider myFileUrlProvider;
  private boolean myBuildHasIndexerFeature;

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
        myBuildHasIndexerFeature = !runningBuild.getBuildFeaturesOfType(SymbolsConstants.BUILD_FEATURE_TYPE).isEmpty();

        if(!myBuildHasIndexerFeature) {
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

        if (myPdbFileToArtifactMap.isEmpty()) {
          myProgressLogger.warning("Symbols weren't found in artifacts to be published.");
          LOG.debug("Symbols weren't found in artifacts to be published for build with id " + build.getBuildId());
        } else if (myPdbFileSignatures.isEmpty()){
          LOG.warn("No information was collected about symbol files signatures");
          myProgressLogger.warning("No information was collected about symbol files signatures");
        } else {
          try {
            final File localIndexDataFile = FileUtil.createTempFile(myBuildTempDirectory, "symbol-signatures-local-", ".xml", false);
            PdbSignatureIndexUtil.write(new FileOutputStream(localIndexDataFile), myPdbFileSignatures);

            final Set<PdbSignatureIndexEntry> transformedIndexData = getSignatureIndexEntries(myPdbFileSignatures, myPdbFileToArtifactMap);
            final File transformedIndexDataFile = FileUtil.createTempFile(myBuildTempDirectory, SymbolsConstants.SYMBOL_SIGNATURES_FILE_NAME_PREFIX, ".xml", false);
            PdbSignatureIndexUtil.write(new FileOutputStream(transformedIndexDataFile), transformedIndexData);

            myProgressLogger.message("Publishing collected symbol files signatures.");
            myArtifactsWatcher.addNewArtifactsPath(localIndexDataFile + "=>" + ".teamcity/symbols");
            myArtifactsWatcher.addNewArtifactsPath(transformedIndexDataFile + "=>" + ".teamcity/symbols");
          } catch (Exception e) {
            LOG.error("Error while dumping symbols/binaries signatures for build with id " + build.getBuildId(), e);
            myProgressLogger.error("Error while dumping symbols/binaries signatures.");
            myProgressLogger.exception(e);
          }
        }
        myPdbFileToArtifactMap.clear();
        myPdbFileSignatures.clear();

        if (myBinaryFileToArtifactMap.isEmpty()) {
          myProgressLogger.warning("Binaries weren't found in artifacts to be published.");
          LOG.debug("Binaries weren't found in artifacts to be published for build with id " + build.getBuildId());
        } else if (myBinaryFileSignatures.isEmpty()) {
          LOG.warn("No information was collected about binary files signatures");
          myProgressLogger.warning("No information was collected about binary files signatures");
        } else {
          try {
            final File localIndexDataFile = FileUtil.createTempFile(myBuildTempDirectory, "binary-signatures-local-", ".xml", false);
            PdbSignatureIndexUtil.write(new FileOutputStream(localIndexDataFile), myBinaryFileSignatures);

            final Set<PdbSignatureIndexEntry> transformedIndexData = getSignatureIndexEntries(myBinaryFileSignatures, myBinaryFileToArtifactMap);
            final File transformedIndexDataFile = FileUtil.createTempFile(myBuildTempDirectory, SymbolsConstants.BINARY_SIGNATURES_FILE_NAME_PREFIX, ".xml", false);
            PdbSignatureIndexUtil.write(new FileOutputStream(transformedIndexDataFile), transformedIndexData);

            myProgressLogger.message("Publishing collected binary files signatures.");
            myArtifactsWatcher.addNewArtifactsPath(localIndexDataFile + "=>" + ".teamcity/symbols");
            myArtifactsWatcher.addNewArtifactsPath(transformedIndexDataFile + "=>" + ".teamcity/symbols");
          } catch (Exception e) {
            LOG.error("Error while dumping symbols/binaries signatures for build with id " + build.getBuildId(), e);
            myProgressLogger.error("Error while dumping symbols/binaries signatures.");
            myProgressLogger.exception(e);
          }
        }
        myBinaryFileToArtifactMap.clear();
        myBinaryFileSignatures.clear();
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
    processPdbArtifacts(getArtifactPathsByFileExtension(artifacts, PDB_FILE_EXTENSION));

    LOG.debug("Searching for binary files in publishing *.exe artifacts.");
    processBinaryArtifacts(artifacts, EXE_FILE_EXTENSION);

    LOG.debug("Searching for binary files in publishing *.dll artifacts.");
    processBinaryArtifacts(artifacts, DLL_FILE_EXTENSION);
  }

  private void processPdbArtifacts(Map<File, String> pdbFiles) {
    final PdbFilePatcher pdbFilePatcher = new PdbFilePatcher(myBuildTempDirectory, mySrcSrvHomeDir, new SrcSrvStreamBuilder(myFileUrlProvider, myProgressLogger));
    for(File pdbFile : pdbFiles.keySet()){
      if(myPdbFileToArtifactMap.containsKey(pdbFile)){
        LOG.debug(String.format("File %s already processed. Skipped.", pdbFile.getAbsolutePath()));
        continue;
      }
      try {
        myProgressLogger.message("Indexing sources appeared in file " + pdbFile.getAbsolutePath());
        pdbFilePatcher.patch(pdbFile, myProgressLogger);
        final String artifactPath = myArtifactPathHelper.concatenateArtifactPath(pdbFiles.get(pdbFile), pdbFile.getName());
        final PdbSignatureIndexEntry signatureIndexEntry = getPdbSignature(pdbFile);
        myPdbFileToArtifactMap.put(pdbFile, artifactPath);
        myPdbFileSignatures.add(signatureIndexEntry);
      } catch (Throwable e) {
        LOG.error("Error occurred while processing symbols file " + pdbFile, e);
        myProgressLogger.error("Error occurred while processing symbols file " + pdbFile);
        myProgressLogger.exception(e);
      }
    }
  }

  private void processBinaryArtifacts(@NotNull List<ArtifactsCollection> artifacts, String fileExtension) {
    final Map<File, String> binaryFiles = getArtifactPathsByFileExtension(artifacts, fileExtension);
    for (File binaryFile : binaryFiles.keySet()){
      if(myBinaryFileToArtifactMap.containsKey(binaryFile)){
        LOG.debug(String.format("File %s already processed. Skipped.", binaryFile.getAbsolutePath()));
        continue;
      }
      try {
        final String artifactPath = myArtifactPathHelper.concatenateArtifactPath(binaryFiles.get(binaryFile), binaryFile.getName());
        final PdbSignatureIndexEntry signatureIndexEntry = getBinarySignature(binaryFile);
        myBinaryFileToArtifactMap.put(binaryFile, artifactPath);
        myBinaryFileSignatures.add(signatureIndexEntry);
      } catch (Throwable e) {
        LOG.error("Error occurred while processing binary file " + binaryFile, e);
        myProgressLogger.error("Error occurred while processing binary file " + binaryFile);
        myProgressLogger.exception(e);
      }
    }
  }

  @NotNull
  private PdbSignatureIndexEntry getPdbSignature(File pdbFile) throws Exception {
    final File guidDumpFile = FileUtil.createTempFile(myBuildTempDirectory, "symbol-signature-local-", ".xml", false);
    myJetSymbolsExe.dumpPdbGuidsToFile(Collections.singleton(pdbFile), guidDumpFile, myProgressLogger);
    if(guidDumpFile.isFile())
      return PdbSignatureIndexUtil.read(new FileInputStream(guidDumpFile), true).iterator().next();
    else
      throw new Exception("Failed to get signature of " + pdbFile.getPath());
  }

  @NotNull
  private PdbSignatureIndexEntry getBinarySignature(File binaryFile) throws Exception {
    final File guidDumpFile = FileUtil.createTempFile(myBuildTempDirectory, "binary-signature-local-", ".xml", false);
    BinaryGuidDumper.dumpBinaryGuidsToFile(Collections.singleton(binaryFile), guidDumpFile, myProgressLogger);
    if(guidDumpFile.isFile())
      return PdbSignatureIndexUtil.read(new FileInputStream(guidDumpFile), true).iterator().next();
    else
      throw new Exception("Failed to get signature of " + binaryFile.getPath());
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
    return myBuildHasIndexerFeature && myFileUrlProvider != null && mySrcSrvHomeDir != null;
  }

  private static void checkAndReportRuntimeRequirements(@NotNull BuildAgentConfiguration agentConfiguration, @NotNull BuildProgressLogger logger){
    for (String parameterName : agentConfiguration.getConfigurationParameters().keySet()) {
      if(NET_4X_PATTERN.matcher(parameterName).find()) return;
    }
    logger.logBuildProblem(BuildProblemData.createBuildProblem(NET_45_NOT_FOUND_PROBLEM_IDENTITY, BuildProblemTypes.TC_ERROR_MESSAGE_TYPE, ".NET 4.5 runtime required for symbols indexing was not found on build agent."));
  }
}
