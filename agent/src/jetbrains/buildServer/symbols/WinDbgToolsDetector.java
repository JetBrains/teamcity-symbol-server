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

import static jetbrains.buildServer.util.Bitness.BIT32;
import static jetbrains.buildServer.util.Win32RegistryAccessor.Hive.LOCAL_MACHINE;

/**
 * @author Evgeniy.Koshkin
 */
public class WinDbgToolsDetector extends AgentLifeCycleAdapter {

  private static final Logger LOG = Logger.getLogger(WinDbgToolsDetector.class);

  private static final String WINDOWS_KITS_INSTALLED_ROOTS_KEY_PATH = "Software/Microsoft/Windows Kits/Installed Roots";
  private static final String WIN_DBG_81_ROOT_ENTRY_NAME = "WindowsDebuggersRoot81";
  private static final String WIN_SDK_81_ROOT_ENTRY_NAME = "KitsRoot81";
  private static final String WIN_DBG_8_ROOT_ENTRY_NAME = "WindowsDebuggersRoot8";
  private static final String WIN_SDK_8_ROOT_ENTRY_NAME = "KitsRoot8";
  private static final String WIN_DBG_HOME_DIR_RELATIVE_SDK8 = "\\Debuggers";

  public static final String WIN_DBG_PATH = "WinDbg" + DotNetConstants.PATH;

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

    LOG.debug("Searching the WinDbg as part of Windows 8.1 SDK");
    File winDbgHomeDir = searchSDK8x(WIN_DBG_81_ROOT_ENTRY_NAME, WIN_SDK_81_ROOT_ENTRY_NAME, "8.1");
    if(winDbgHomeDir == null) {
      LOG.debug("Searching the WinDbg as part of Windows 8 SDK");
      searchSDK8x(WIN_DBG_8_ROOT_ENTRY_NAME, WIN_SDK_8_ROOT_ENTRY_NAME, "8");
    } if(winDbgHomeDir == null) {
      LOG.debug("Searching the WinDbg as part of Windows 7 SDK");
      searchSDK7x();
    }

    if(winDbgHomeDir == null) LOG.info("WinDbg tools were not found on this machine.");
    else{
      final String winDbgHomeDirAbsolutePath = winDbgHomeDir.getAbsolutePath();
      LOG.info("WinDbg tools were found on path " + winDbgHomeDirAbsolutePath);
      config.addConfigurationParameter(WIN_DBG_PATH, winDbgHomeDirAbsolutePath);
    }
  }

  @Nullable
  private File searchSDK8x(String winDbgRootEntryName, String winSdkRootEntryName, String sdkVersion) {
    File winDbgHomeDir = myRegistryAccessor.readRegistryFile(LOCAL_MACHINE, BIT32, WINDOWS_KITS_INSTALLED_ROOTS_KEY_PATH, winDbgRootEntryName);
    if (winDbgHomeDir != null) return winDbgHomeDir;
    final File sdkHomeDir = myRegistryAccessor.readRegistryFile(LOCAL_MACHINE, BIT32, WINDOWS_KITS_INSTALLED_ROOTS_KEY_PATH, winSdkRootEntryName);
    if(sdkHomeDir == null){
      LOG.debug(String.format("Failed to locate Windows SDK %s home directory.", sdkVersion));
      return null;
    }
    LOG.debug(String.format("Windows SDK %s found, searching WinDbg under its home directory.", sdkHomeDir));
    winDbgHomeDir = new File(sdkHomeDir, WIN_DBG_HOME_DIR_RELATIVE_SDK8);
    if(winDbgHomeDir.isDirectory()) return winDbgHomeDir;
    LOG.debug("Failed to find WinDbg home directory under Windows SDK home directory detected on path " + sdkHomeDir.getAbsolutePath());
    return null;
  }

  @Nullable
  private File searchSDK7x() {
    //sdk 7, 71 - no info in registry, check for file system path under %PROGRAMFILES(x86)% or %PROGRAMFILES%
    return null;
  }
}
