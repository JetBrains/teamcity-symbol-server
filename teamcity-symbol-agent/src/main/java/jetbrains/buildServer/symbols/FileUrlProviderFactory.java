

package jetbrains.buildServer.symbols;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildProgressLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Evgeniy.Koshkin
 */
public class FileUrlProviderFactory {
  private static final Logger LOG = Logger.getInstance(FileUrlProviderFactory.class.getCanonicalName());

  @Nullable
  public static FileUrlProvider getProvider(@NotNull AgentRunningBuild build, @NotNull BuildProgressLogger buildLogger) {
    final String sourceServerUrlPrefix = build.getSharedConfigParameters().get(SymbolsConstants.SOURCES_SERVER_URL_PARAM_NAME);
    if (sourceServerUrlPrefix == null) {
      final String message = String.format("%s configuration parameter was not set. No symbol and source indexing will be performed.", SymbolsConstants.SOURCES_SERVER_URL_PARAM_NAME);
      LOG.error(message);
      buildLogger.error(message);
      return null;
    }
    final String message = String.format("Using Sources Server URL %s", sourceServerUrlPrefix);
    buildLogger.message(message);
    LOG.debug(message);
    return new FileUrlProvider(sourceServerUrlPrefix, build.getBuildId(), build.getCheckoutDirectory());
  }
}
