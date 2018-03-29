/*
 * Copyright 2000-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jetbrains.buildServer.symbols;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildProgressLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * @author Evgeniy.Koshkin
 */
public class FileUrlProviderFactory {
  private static final Logger LOG = Logger.getInstance(FileUrlProviderFactory.class.getCanonicalName());

  @Nullable
  public static FileUrlProvider getProvider(@NotNull AgentRunningBuild build, @NotNull BuildProgressLogger buildLogger){
    String sourceServerUrlPrefix = build.getSharedConfigParameters().get(SymbolsConstants.SOURCES_SERVER_URL_PARAM_NAME);
    if(sourceServerUrlPrefix == null){
      final String message = String.format("%s configuration parameter was not set. No symbol and source indexing will be performed.", SymbolsConstants.SOURCES_SERVER_URL_PARAM_NAME);
      LOG.error(message);
      buildLogger.error(message);
      return null;
    }
    final String message = String.format("Using Sources Server URL %s", sourceServerUrlPrefix);
    buildLogger.message(message);
    LOG.debug(message);
    sourceServerUrlPrefix = sourceServerUrlPrefix.substring(0, sourceServerUrlPrefix.length() - 1); //cut last '/'
    try {
      return new FileUrlProvider(sourceServerUrlPrefix, build.getBuildId(), build.getCheckoutDirectory());
    } catch (IOException e) {
      buildLogger.exception(e);
      return null;
    }
  }
}
