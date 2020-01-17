/*
 * Copyright 2000-2020 JetBrains s.r.o.
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

import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.serverSide.BuildFeature;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Evgeniy.Koshkin
 */
public class IndexSymbolsBuildFeature extends BuildFeature {

  private String myEditParametersUrl;

  public IndexSymbolsBuildFeature(final PluginDescriptor pluginDescriptor, final WebControllerManager web) {
    final String jsp = pluginDescriptor.getPluginResourcesPath("editSymbolsBuildFeatureParams.jsp");
    final String html = pluginDescriptor.getPluginResourcesPath("symbolIndexerSettings.html");

    web.registerController(html, new BaseController() {
      @Override
      protected ModelAndView doHandle(final HttpServletRequest request, final HttpServletResponse response) throws Exception {
        return new ModelAndView(jsp);
      }
    });

    myEditParametersUrl = html;
  }

  @NotNull
  @Override
  public String getType() {
    return SymbolsConstants.BUILD_FEATURE_TYPE;
  }

  @NotNull
  @Override
  public String getDisplayName() {
    return "Symbol Files Indexer";
  }

  @Override
  public boolean isMultipleFeaturesPerBuildTypeAllowed() {
    return false;
  }

  @Nullable
  @Override
  public String getEditParametersUrl() {
    return myEditParametersUrl;
  }
}
