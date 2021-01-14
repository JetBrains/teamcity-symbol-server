/*
 * Copyright 2000-2021 JetBrains s.r.o.
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
import java.util.Optional;
import jetbrains.buildServer.RootUrlHolder;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.impl.LogUtil;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.vcs.InvalidBranchSpecException;
import jetbrains.buildServer.vcs.spec.BranchSpecs;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * @author Evgeniy.Koshkin
 */
public class SymbolsIndexerParametersPreprocessor implements BuildStartContextProcessor {
  private final static Logger LOG = Logger.getInstance(SymbolsIndexerParametersPreprocessor.class.getName());

  private final RootUrlHolder myRootUrlHolder;
  private final BranchSpecs mySpecs;

  public SymbolsIndexerParametersPreprocessor(RootUrlHolder rootUrlHolder, BranchSpecs specs) {
    myRootUrlHolder = rootUrlHolder;
    mySpecs = specs;
  }

  public void updateParameters(@NotNull BuildStartContext context) {
    final SBuildType buildType = context.getBuild().getBuildType();
    if(buildType == null) return;

    applyServerUrl(context);
    applyBranchFilter(context);
  }

  private void applyServerUrl(@NotNull BuildStartContext context) {
    final SBuildType buildType = context.getBuild().getBuildType();
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

  private void applyBranchFilter(@NotNull BuildStartContext context) {
    final SRunningBuild build = context.getBuild();
    final Optional<SBuildFeatureDescriptor> buildFeature = build.getBuildFeaturesOfType(SymbolsConstants.BUILD_FEATURE_TYPE).stream().findFirst();
    if (buildFeature.isPresent()) {
      final String branchFilter = buildFeature.get().getParameters().get(SymbolsConstants.BRANCH_FILTER);
      if (!acceptBuildBranch(build, branchFilter)) {
        context.addSharedParameter(SymbolsConstants.INDEXING_ENABLED_PARAM_NAME, "false");
      }
    }
  }

  private boolean acceptBuildBranch(@NotNull final SBuild build, final String branchFilter) {
    if (StringUtil.isEmpty(branchFilter)) {
      return true;
    }

    final Branch branch = build.getBranch();
    if (branch == null) {
      return false;
    }

    final BranchFilter filter;
    try {
      filter = mySpecs.createFilter(branchFilter);
    } catch (InvalidBranchSpecException e) {
      LOG.warnAndDebugDetails("branch filter error in " + LogUtil.describe(build.getBuildType()), e);
      return false;
    }

    if (!filter.accept(branch)) {
      LOG.info("Indexing of symbols and sources is skipped for build " + LogUtil.describe(build) +
               ". Reason: branch filter \"" + branchFilter + "\" does not accept build branch \"" + branch + "\"");
      return false;
    }

    return true;
  }
}
