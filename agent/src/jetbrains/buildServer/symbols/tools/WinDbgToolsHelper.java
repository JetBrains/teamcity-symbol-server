package jetbrains.buildServer.symbols.tools;

import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.dotNet.DotNetConstants;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Map;

/**
 * @author Evgeniy.Koshkin
 */
public class WinDbgToolsHelper {

  private static final Logger LOG = Logger.getLogger(WinDbgToolsHelper.class);

  private static final String SRCSRV_HOME_DIR_RELATIVE_X64 = "\\Debuggers\\x64\\srcsrv\\";
  private static final String SRCSRV_HOME_DIR_RELATIVE_X86 = "\\Debuggers\\x86\\srcsrv\\";

  @Nullable
  public static File getSrcSrvHomeDir(@NotNull AgentRunningBuild build) {
    final Map<String,String> agentConfigParameters = build.getAgentConfiguration().getConfigurationParameters();
    for (String paramName : agentConfigParameters.keySet()){
      if(paramName.startsWith(DotNetConstants.WINDOWS_SDK) && paramName.endsWith(DotNetConstants.PATH)){
        final File winSdkHomeDir = new File(agentConfigParameters.get(paramName));
        if(!winSdkHomeDir.exists()) continue;
        File dir = new File(winSdkHomeDir, SRCSRV_HOME_DIR_RELATIVE_X64);
        if(dir.exists() && dir.isDirectory()) return dir;
        dir = new File(winSdkHomeDir, SRCSRV_HOME_DIR_RELATIVE_X86);
        if(dir.exists() && dir.isDirectory()) return dir;
        LOG.debug("Failed to find Source Server tools home directory under Windows SDK home directory detected on path " + winSdkHomeDir.getAbsolutePath());
      }
    }
    LOG.debug("None of Windows SDK versions are mentioned in agent configuration.");
    return null;
  }
}
