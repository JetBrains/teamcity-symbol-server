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

import jetbrains.buildServer.RootUrlHolder;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * @author Evgeniy.Koshkin
 */
public class SymbolsIndexerParametersPreprocessor implements BuildStartContextProcessor {

  private final RootUrlHolder myRootUrlHolder;

  public SymbolsIndexerParametersPreprocessor(RootUrlHolder rootUrlHolder) {
    myRootUrlHolder = rootUrlHolder;
  }

  public void updateParameters(@NotNull BuildStartContext context) {
    final SBuildType buildType = context.getBuild().getBuildType();
    if(buildType == null) return;
    final Collection<SBuildFeatureDescriptor> buildFeatures = buildType.getResolvedSettings().getBuildFeatures();
    for(SBuildFeatureDescriptor buildFeature : buildFeatures){
      if(!buildFeature.getType().equals(SymbolsConstants.BUILD_FEATURE_TYPE)) continue;
      String serverOwnUrl = context.getSharedParameters().get(SymbolsConstants.SERVER_OWN_URL_PARAM_NAME);
      if(serverOwnUrl == null){
        serverOwnUrl = myRootUrlHolder.getRootUrl();
      }
      final String sourceServerUrl = String.format("%s%s", StringUtil.removeTailingSlash(serverOwnUrl), SymbolsConstants.APP_SOURCES);
      context.addSharedParameter(SymbolsConstants.SOURCES_SERVER_URL_PARAM_NAME, sourceServerUrl);
    }
  }
}
