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
