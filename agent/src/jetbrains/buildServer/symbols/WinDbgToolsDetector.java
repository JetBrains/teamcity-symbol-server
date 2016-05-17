package jetbrains.buildServer.symbols;

import jetbrains.buildServer.agent.AgentLifeCycleAdapter;
import jetbrains.buildServer.agent.AgentLifeCycleListener;
import jetbrains.buildServer.agent.BuildAgent;
import jetbrains.buildServer.agent.BuildAgentConfiguration;
import jetbrains.buildServer.dotNet.DotNetConstants;
import jetbrains.buildServer.util.EventDispatcher;
import jetbrains.buildServer.util.Win32RegistryAccessor;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileFilter;

import static jetbrains.buildServer.util.Bitness.BIT32;
import static jetbrains.buildServer.util.Win32RegistryAccessor.Hive.LOCAL_MACHINE;

/**
 * @author Evgeniy.Koshkin
 */
public class WinDbgToolsDetector extends AgentLifeCycleAdapter {

  private static final Logger LOG = Logger.getLogger(WinDbgToolsDetector.class);

  private static final String WINDOWS_KITS_INSTALLED_ROOTS_KEY_PATH = "Software/Microsoft/Windows Kits/Installed Roots";
  private static final String WIN_DBG_10_ROOT_ENTRY_NAME = "WindowsDebuggersRoot10";
  private static final String WIN_SDK_10_ROOT_ENTRY_NAME = "KitsRoot10";
  private static final String WIN_DBG_81_ROOT_ENTRY_NAME = "WindowsDebuggersRoot81";
  private static final String WIN_SDK_81_ROOT_ENTRY_NAME = "KitsRoot81";
  private static final String WIN_DBG_8_ROOT_ENTRY_NAME = "WindowsDebuggersRoot8";
  private static final String WIN_SDK_8_ROOT_ENTRY_NAME = "KitsRoot8";
  private static final String WIN_DBG_HOME_DIR_RELATIVE = "\\Debuggers";

  public static final String WIN_DBG_PATH = "WinDbg" + DotNetConstants.PATH;
  private static final String DEBUGGING_TOOLS_FOR_WINDOWS = "Debugging Tools for Windows";

  @NotNull private final Win32RegistryAccessor myRegistryAccessor;

  public WinDbgToolsDetector(@NotNull final EventDispatcher<AgentLifeCycleListener> events,
                             @NotNull final Win32RegistryAccessor registryAccessor) {
    myRegistryAccessor = registryAccessor;
    events.addListener(this);
  }

  @Override
  public void agentInitialized(@NotNull BuildAgent agent) {
    final BuildAgentConfiguration config = agent.getConfiguration();
    if (!config.getSystemInfo().isWindows()) return;
    LOG.info("Searching WinDbg installation...");

    LOG.info("Searching the WinDbg as part of Windows 10 SDK");
    File winDbgHomeDir = searchSDK8AndLater(WIN_DBG_10_ROOT_ENTRY_NAME, WIN_SDK_10_ROOT_ENTRY_NAME, "10");
    if(winDbgHomeDir == null){
      LOG.info("Searching the WinDbg as part of Windows 8.1 SDK");
      winDbgHomeDir = searchSDK8AndLater(WIN_DBG_81_ROOT_ENTRY_NAME, WIN_SDK_81_ROOT_ENTRY_NAME, "8.1");
      if(winDbgHomeDir == null) {
        LOG.info("Searching the WinDbg as part of Windows 8 SDK");
        winDbgHomeDir = searchSDK8AndLater(WIN_DBG_8_ROOT_ENTRY_NAME, WIN_SDK_8_ROOT_ENTRY_NAME, "8");
      } if(winDbgHomeDir == null) {
        LOG.info("Searching the WinDbg as part of Windows 7 SDK");
        winDbgHomeDir = searchSDK7x();
      }
    }

    if(winDbgHomeDir == null) LOG.info("WinDbg tools were not found on this machine.");
    else{
      final String winDbgHomeDirAbsolutePath = winDbgHomeDir.getAbsolutePath();
      LOG.info("WinDbg tools were found on path " + winDbgHomeDirAbsolutePath);
      config.addConfigurationParameter(WIN_DBG_PATH, winDbgHomeDirAbsolutePath);
    }
  }

  @Nullable
  private File searchSDK8AndLater(String winDbgRootEntryName, String winSdkRootEntryName, String sdkVersion) {
    File winDbgHomeDir = myRegistryAccessor.readRegistryFile(LOCAL_MACHINE, BIT32, WINDOWS_KITS_INSTALLED_ROOTS_KEY_PATH, winDbgRootEntryName);
    if (winDbgHomeDir != null) return winDbgHomeDir;
    final File sdkHomeDir = myRegistryAccessor.readRegistryFile(LOCAL_MACHINE, BIT32, WINDOWS_KITS_INSTALLED_ROOTS_KEY_PATH, winSdkRootEntryName);
    if(sdkHomeDir == null){
      LOG.debug(String.format("Failed to locate Windows SDK %s home directory.", sdkVersion));
      return null;
    }
    LOG.debug(String.format("Windows SDK %s found, searching WinDbg under its home directory.", sdkHomeDir));
    winDbgHomeDir = new File(sdkHomeDir, WIN_DBG_HOME_DIR_RELATIVE);
    if(winDbgHomeDir.isDirectory()) return winDbgHomeDir;
    LOG.debug("Failed to find WinDbg home directory under Windows SDK home directory detected on path " + sdkHomeDir.getAbsolutePath());
    return null;
  }

  @Nullable
  private File searchSDK7x() {
    final String systemDrive = ensureSuffix(getEnv("SYSTEMDRIVE", "C:"), ":").toUpperCase();
    final File programFilesDir = new File(systemDrive, "Program Files");
    final File programFilesX86Dir = new File(systemDrive, "Program Files (x86)");
    if(!programFilesDir.isDirectory() && !programFilesX86Dir.isDirectory()){
      LOG.debug(String.format("Failed to locate 'Program Files' directory on the machine. Checked paths: %s, %s", programFilesDir, programFilesX86Dir));
      return null;
    }
    File winDbgHome = findWinDbgHomeInDirectory(programFilesDir);
    if(winDbgHome == null){
      winDbgHome = findWinDbgHomeInDirectory(programFilesX86Dir);
    }
    return winDbgHome;
  }

  @Nullable
  private File findWinDbgHomeInDirectory(File directory) {
    if (!directory.isDirectory()) return null;

    final String directoryAbsolutePath = directory.getAbsolutePath();
    LOG.info("Searching for WinDbg home under " + directoryAbsolutePath);

    final File[] matches = directory.listFiles(new FileFilter() {
      public boolean accept(File pathname) {
        return pathname.isDirectory() && pathname.getName().startsWith(DEBUGGING_TOOLS_FOR_WINDOWS);
      }
    });

    if (matches == null || matches.length == 0) LOG.info("WinDbg home was NOT found under " + directoryAbsolutePath);
    else return matches[0];
    return null;
  }

  @NotNull
  private static String ensureSuffix(final @NotNull String string, final @NotNull String suffix) {
    int n = string.length(),
            m = suffix.length();
    return n >= m && string.endsWith(suffix) ? string : string + suffix;
  }

  @NotNull
  private static String getEnv(final @NotNull String variableName, final @NotNull String defaultValue) {
    String value = System.getenv(variableName);
    return value != null ? value : defaultValue;
  }
}
