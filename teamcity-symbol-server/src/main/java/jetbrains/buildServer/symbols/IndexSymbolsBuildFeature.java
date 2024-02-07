

package jetbrains.buildServer.symbols;

import com.intellij.openapi.diagnostic.Logger;
import java.util.*;
import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.parameters.ReferencesResolverUtil;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.impl.LogUtil;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.vcs.InvalidBranchSpecException;
import jetbrains.buildServer.vcs.spec.BranchSpecs;
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
  private final String myEditParametersUrl;
  private final BranchSpecs mySpecs;

  public IndexSymbolsBuildFeature(final PluginDescriptor pluginDescriptor, final WebControllerManager web, final BranchSpecs specs) {
    final String jsp = pluginDescriptor.getPluginResourcesPath("editSymbolsBuildFeatureParams.jsp");
    final String html = pluginDescriptor.getPluginResourcesPath("symbolIndexerSettings.html");

    web.registerController(html, new BaseController() {
      @Override
      protected ModelAndView doHandle(final HttpServletRequest request, final HttpServletResponse response) throws Exception {
        return new ModelAndView(jsp);
      }
    });

    myEditParametersUrl = html;
    mySpecs = specs;
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

  @Nullable
  @Override
  public PropertiesProcessor getParametersProcessor() {
    return new PropertiesProcessor() {
      @Override
      public Collection<InvalidProperty> process(final Map<String, String> properties) {
        List<InvalidProperty> errors = new ArrayList<InvalidProperty>();
        String branchFilter = properties.get(SymbolsConstants.BRANCH_FILTER);
        if (StringUtil.isNotEmpty(branchFilter) && !ReferencesResolverUtil.mayContainReference(branchFilter)) {
          try {
            mySpecs.validate(branchFilter, false);
          } catch (InvalidBranchSpecException e) {
            errors.add(new InvalidProperty(SymbolsConstants.BRANCH_FILTER, "Line " + e.getLineNum() + ": " + e.getError()));
          }
        }
        return errors;
      }
    };
  }
}
