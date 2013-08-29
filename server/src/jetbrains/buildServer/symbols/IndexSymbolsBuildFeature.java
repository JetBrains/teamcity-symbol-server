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
