package jetbrains.buildServer.symbols;

import jetbrains.buildServer.controllers.AuthorizationInterceptor;
import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.controllers.interceptors.auth.HttpAuthenticationManager;
import jetbrains.buildServer.controllers.interceptors.auth.HttpAuthenticationResult;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.serverSide.SecurityContextEx;
import jetbrains.buildServer.serverSide.artifacts.BuildArtifact;
import jetbrains.buildServer.serverSide.artifacts.BuildArtifactsViewMode;
import jetbrains.buildServer.serverSide.auth.Permission;
import jetbrains.buildServer.serverSide.auth.ServerPrincipal;
import jetbrains.buildServer.serverSide.metadata.BuildMetadataEntry;
import jetbrains.buildServer.serverSide.metadata.MetadataStorage;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.users.UserModel;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import jetbrains.buildServer.web.util.WebUtil;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.servlet.ModelAndView;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Evgeniy.Koshkin
 */
public class DownloadSymbolsController extends BaseController {

  private static final String COMPRESSED_FILE_EXTENSION = "pd_";
  private static final String FILE_POINTER_FILE_EXTENSION = "ptr";

  private static final Logger LOG = Logger.getLogger(DownloadSymbolsController.class);

  @NotNull private final UserModel myUserModel;
  @NotNull private final SecurityContextEx mySecurityContext;
  @NotNull private final MetadataStorage myBuildMetadataStorage;
  @NotNull private final HttpAuthenticationManager myAuthManager;

  public DownloadSymbolsController(@NotNull SBuildServer server,
                                   @NotNull WebControllerManager controllerManager,
                                   @NotNull AuthorizationInterceptor authInterceptor,
                                   @NotNull SecurityContextEx securityContext,
                                   @NotNull HttpAuthenticationManager authManager,
                                   @NotNull UserModel userModel,
                                   @NotNull MetadataStorage buildMetadataStorage) {
    super(server);
    mySecurityContext = securityContext;
    myUserModel = userModel;
    myBuildMetadataStorage = buildMetadataStorage;
    myAuthManager = authManager;
    final String path = SymbolsConstants.APP_SYMBOLS + "**";
    controllerManager.registerController(path, this);
    authInterceptor.addPathNotRequiringAuth(path);
  }

  @Nullable
  @Override
  protected ModelAndView doHandle(final @NotNull HttpServletRequest request, final @NotNull HttpServletResponse response) throws Exception {
    final String requestURI = request.getRequestURI();

    if(requestURI.endsWith(SymbolsConstants.APP_SYMBOLS)){
      response.sendError(HttpServletResponse.SC_OK, "TeamCity Symbol Server available");
      return null;
    }

    if(requestURI.endsWith(COMPRESSED_FILE_EXTENSION)){
      WebUtil.notFound(request, response, "File not found", null);
      return null;
    }
    if(requestURI.endsWith(FILE_POINTER_FILE_EXTENSION)){
      WebUtil.notFound(request, response, "File not found", null);
      return null;
    }

    final HttpAuthenticationResult authResult = myAuthManager.processAuthenticationRequest(request, response);
    switch (authResult.getType()) {
      case NOT_APPLICABLE:
        response.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE, "TODO"); //TODO error message
        return null;
      case UNAUTHENTICATED:
        return null;
    }

    final ServerPrincipal principal = authResult.getPrincipal();
    final SUser user = myUserModel.findUserAccount(principal.getRealm(), principal.getName());
    if(user == null){
      response.sendError(HttpServletResponse.SC_FORBIDDEN, "TODO"); //TODO error message
      return null;
    }
    if (!user.isPermissionGrantedGlobally(Permission.VIEW_BUILD_RUNTIME_DATA)) { //TODO: check permissions locally (for particular project)
      response.sendError(HttpServletResponse.SC_FORBIDDEN, "You have no permissions to download PDB files.");
      return null;
    }

    try {
      mySecurityContext.runAs(user, new SecurityContextEx.RunAsAction() {
        public void run() throws Throwable {
          final String valuableUriPart = requestURI.substring(requestURI.indexOf(SymbolsConstants.APP_SYMBOLS) + SymbolsConstants.APP_SYMBOLS.length());
          final int firstDelimiterPosition = valuableUriPart.indexOf('/');
          final String fileName = valuableUriPart.substring(0, firstDelimiterPosition);
          final String signature = valuableUriPart.substring(firstDelimiterPosition + 1, valuableUriPart.indexOf('/', firstDelimiterPosition + 1));
          final String guid = signature.substring(0, signature.length() - 1); //last symbol is PEDebugType
          LOG.debug(String.format("Symbol file requested. File name: %s. Guid: %s.", fileName, guid));

          final BuildArtifact buildArtifact = findArtifact(guid, fileName);
          if(buildArtifact == null){
            WebUtil.notFound(request, response, "Symbol file not found", null);
            LOG.debug(String.format("Symbol file not found. File name: %s. Guid: %s.", fileName, guid));
            return;
          }

          BufferedOutputStream output = new BufferedOutputStream(response.getOutputStream());
          try {
            InputStream input = buildArtifact.getInputStream();
            try {
              FileUtil.copyStreams(input, output);
            } finally {
              FileUtil.close(input);
            }
          } finally {
            FileUtil.close(output);
          }
        }
      });
    } catch (Throwable throwable) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, throwable.getMessage());
    }

    return null;
  }

  private BuildArtifact findArtifact(String guid, String fileName) {
    final Iterator<BuildMetadataEntry> entryIterator = myBuildMetadataStorage.getEntriesByKey(BuildSymbolsIndexProvider.PROVIDER_ID, guid);
    if(!entryIterator.hasNext()){
      LOG.debug(String.format("No items found in symbol index for guid '%s'", guid));
      return null;
    }
    final BuildMetadataEntry entry = entryIterator.next();
    final Map<String,String> metadata = entry.getMetadata();
    final String storedFileName = metadata.get(BuildSymbolsIndexProvider.FILE_NAME_KEY);
    final String artifactPath = metadata.get(BuildSymbolsIndexProvider.ARTIFACT_PATH_KEY);
    if(!storedFileName.equals(fileName)){
      LOG.debug(String.format("File name '%s' stored for guid '%s' differs from requested '%s'.", storedFileName, guid, fileName));
      return null;
    }
    final long buildId = entry.getBuildId();
    final SBuild build = myServer.findBuildInstanceById(buildId);
    if(build == null){
      LOG.debug(String.format("Build not found by id %d.", buildId));
      return null;
    }
    final BuildArtifact buildArtifact = build.getArtifacts(BuildArtifactsViewMode.VIEW_DEFAULT_WITH_ARCHIVES_CONTENT).getArtifact(artifactPath);
    if(buildArtifact == null){
      LOG.debug(String.format("Artifact not found by path %s for build with id %d.", artifactPath, buildId));
    }
    return buildArtifact;
  }
}