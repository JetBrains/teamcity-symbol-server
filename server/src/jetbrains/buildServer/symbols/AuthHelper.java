package jetbrains.buildServer.symbols;

import jetbrains.buildServer.controllers.interceptors.auth.HttpAuthenticationManager;
import jetbrains.buildServer.controllers.interceptors.auth.HttpAuthenticationResult;
import jetbrains.buildServer.serverSide.auth.ServerPrincipal;
import jetbrains.buildServer.serverSide.impl.ServerSettings;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.users.UserModel;
import jetbrains.buildServer.util.Predicate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author Evgeniy.Koshkin
 */
public class AuthHelper {

  @NotNull private final ServerSettings myServerSettings;
  @NotNull private final UserModel myUserModel;
  @NotNull private final HttpAuthenticationManager myAuthManager;

  public AuthHelper(@NotNull ServerSettings serverSettings,
                    @NotNull UserModel userModel,
                    @NotNull HttpAuthenticationManager authManager) {
    myServerSettings = serverSettings;
    myUserModel = userModel;
    myAuthManager = authManager;
  }

  @Nullable
  public SUser getAuthenticatedUser(@NotNull HttpServletRequest request,
                                    @NotNull HttpServletResponse response,
                                    @NotNull Predicate<SUser> hasPermissions) throws IOException {
    if(myServerSettings.isGuestLoginAllowed()) {
      final SUser guestUser = myUserModel.getGuestUser();
      if(hasPermissions.apply(guestUser)) return guestUser;
    }
    final HttpAuthenticationResult authResult = myAuthManager.processAuthenticationRequest(request, response, false);
    switch (authResult.getType()) {
      case NOT_APPLICABLE:
        myAuthManager.processUnauthenticatedRequest(request, response, "", false);
        return null;
      case UNAUTHENTICATED:
        return null;
    }
    final ServerPrincipal principal = authResult.getPrincipal();
    final SUser user = myUserModel.findUserAccount(principal.getRealm(), principal.getName());
    if(user == null){
      response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
      return null;
    }
    return hasPermissions.apply(user) ? user : null;
  }
}
