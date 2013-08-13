package jetbrains.buildServer.symbols;

import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.serverSide.BuildFeature;
import jetbrains.buildServer.serverSide.impl.ServerSettings;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import jetbrains.buildServer.web.util.WebUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * @author Evgeniy.Koshkin
 */
public class IndexSymbolsBuildFeature extends BuildFeature {

  private String myEditParametersUrl;

  public IndexSymbolsBuildFeature(final PluginDescriptor pluginDescriptor, final WebControllerManager web, final ServerSettings serverSettings) {
    final String jsp = pluginDescriptor.getPluginResourcesPath("editSymbolsBuildFeatureParams.jsp");
    final String html = pluginDescriptor.getPluginResourcesPath("symbolIndexerSettings.html");

    web.registerController(html, new BaseController() {
      @Override
      protected ModelAndView doHandle(final HttpServletRequest request, final HttpServletResponse response) throws Exception {
        final ModelAndView modelAndView = new ModelAndView(jsp);
        modelAndView.getModel().put("isGuestEnabled", serverSettings.isGuestLoginAllowed());
        modelAndView.getModel().put("actualServerUrl",  WebUtil.getRootUrl(request));
        modelAndView.getModel().put("publicFeedUrl", WebUtil.GUEST_AUTH_PREFIX + SymbolsConstants.APP_SYMBOLS);
        return modelAndView;
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

  @NotNull
  @Override
  public String describeParameters(@NotNull Map<String, String> params) {
    return Boolean.parseBoolean(params.get(SymbolsConstants.SOURCES_AUTH_REQUIRED_PARAM_NAME))
            ? "Access to the indexed sources requires HTTP authentication."
            : "No authentication is required to retrieve indexed sources";
  }
}
