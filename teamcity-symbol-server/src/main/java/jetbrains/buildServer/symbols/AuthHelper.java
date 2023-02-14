/*
 * Copyright 2000-2023 JetBrains s.r.o.
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

import jetbrains.buildServer.controllers.interceptors.auth.HttpAuthenticationManager;
import jetbrains.buildServer.controllers.interceptors.auth.HttpAuthenticationResult;
import jetbrains.buildServer.serverSide.auth.LoginConfiguration;
import jetbrains.buildServer.serverSide.auth.ServerPrincipal;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.users.UserModel;
import jetbrains.buildServer.util.Predicate;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author Evgeniy.Koshkin
 */
public class AuthHelper {

  private static final Logger LOG = Logger.getLogger(AuthHelper.class);

  @NotNull private final LoginConfiguration myLoginConfiguration;
  @NotNull private final UserModel myUserModel;
  @NotNull private final HttpAuthenticationManager myAuthManager;

  public AuthHelper(@NotNull LoginConfiguration loginConfiguration,
                    @NotNull UserModel userModel,
                    @NotNull HttpAuthenticationManager authManager) {
    myLoginConfiguration = loginConfiguration;
    myUserModel = userModel;
    myAuthManager = authManager;
  }

  @Nullable
  public SUser getAuthenticatedUser(@NotNull HttpServletRequest request,
                                    @NotNull HttpServletResponse response,
                                    @NotNull Predicate<SUser> hasPermissions) throws IOException {
    if(myLoginConfiguration.isGuestLoginAllowed()) {
      LOG.debug("Guest access enabled on the server. Trying to check permissions of Guest.");
      final SUser guestUser = myUserModel.getGuestUser();
      if (hasPermissions.apply(guestUser)) {
        LOG.debug("Guest user has enough permissions to process request.");
        return guestUser;
      }
      LOG.debug("Guest user has NO permissions to process request. Will try to authenticate incoming request.");
    } else {
      LOG.debug("Guest access disabled on the server. Will try to authenticate incoming request.");
    }
    LOG.debug("Trying to authenticate incoming request.");
    final HttpAuthenticationResult authResult = myAuthManager.processAuthenticationRequest(request, response, false);
    switch (authResult.getType()) {
      case NOT_APPLICABLE:
        //TODO
        LOG.debug("NOT_APPLICABLE");
        myAuthManager.processUnauthenticatedRequest(request, response, "", false);
        return null;
      case UNAUTHENTICATED:
        //TODO
        LOG.debug("UNAUTHENTICATED");
        return null;
    }
    LOG.debug("Incoming request was authenticated successfully.");
    final ServerPrincipal principal = authResult.getPrincipal();
    final String realm = principal.getRealm();
    final String name = principal.getName();
    final SUser user = myUserModel.findUserAccount(realm, name);
    if(user == null){
      LOG.warn(String.format("Failed to find user account by realm (%s) and name (%s)", realm, name));
      response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
      return null;
    }
    LOG.debug(String.format("Found user account (id %s) by realm (%s) and name (%s)", user.getId(), realm, name));
    final boolean hasAccess = hasPermissions.apply(user);
    if (hasAccess) {
      LOG.debug(String.format("Located user (name %s) has enough permissions to process the request.", name));
      return user;
    }
    LOG.warn(String.format("Located user (name %s) has NO permissions to process the request.", name));
    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
    return null;
  }
}
