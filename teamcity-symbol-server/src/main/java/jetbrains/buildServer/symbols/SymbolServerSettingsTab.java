

package jetbrains.buildServer.symbols;

import jetbrains.buildServer.serverSide.auth.AuthUtil;
import jetbrains.buildServer.serverSide.auth.Permission;
import jetbrains.buildServer.serverSide.auth.SecurityContext;
import jetbrains.buildServer.web.openapi.PagePlaces;
import jetbrains.buildServer.web.openapi.PlaceId;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.SimpleCustomTab;
import jetbrains.buildServer.web.util.WebUtil;
import org.jetbrains.annotations.NotNull;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * @author Evgeniy.Koshkin
 */
public class SymbolServerSettingsTab extends SimpleCustomTab {

  private static final String TAB_ID = "sourceServerSettingsTab";

  @NotNull private final SecurityContext mySecurityContext;

  public SymbolServerSettingsTab(@NotNull final PagePlaces pagePlaces,
                                 @NotNull final SecurityContext context,
                                 @NotNull final PluginDescriptor descriptor) {
    super(pagePlaces,
            PlaceId.ADMIN_SERVER_CONFIGURATION_TAB,
            TAB_ID,
            descriptor.getPluginResourcesPath("symbolServerSettings.jsp"),
            "Symbol Server");
    mySecurityContext = context;
    register();
  }

  @Override
  public boolean isVisible() {
    return super.isVisible() && hasAccess();
  }

  @Override
  public boolean isAvailable(@NotNull HttpServletRequest request) {
    return super.isAvailable(request) && hasAccess();
  }

  @Override
  public void fillModel(@NotNull Map<String, Object> model, @NotNull HttpServletRequest request) {
    super.fillModel(model, request);
    model.put("actualServerUrl", WebUtil.getRootUrl(request));
    model.put("appUrl", SymbolsConstants.APP_SYMBOLS);
  }

  private boolean hasAccess() {
    return AuthUtil.hasGlobalPermission(mySecurityContext.getAuthorityHolder(), Permission.CHANGE_SERVER_SETTINGS);
  }
}
