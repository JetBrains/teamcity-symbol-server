package jetbrains.buildServer.symbols;

import jetbrains.buildServer.controllers.AuthorizationInterceptor;
import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.serverSide.SecurityContextEx;
import jetbrains.buildServer.serverSide.artifacts.BuildArtifact;
import jetbrains.buildServer.serverSide.artifacts.BuildArtifactsViewMode;
import jetbrains.buildServer.serverSide.auth.Permission;
import jetbrains.buildServer.serverSide.metadata.BuildMetadataEntry;
import jetbrains.buildServer.serverSide.metadata.MetadataStorage;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.Predicate;
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

  @NotNull private final SecurityContextEx mySecurityContext;
  @NotNull private final MetadataStorage myBuildMetadataStorage;
  @NotNull private final AuthHelper myAuthHelper;

  public DownloadSymbolsController(@NotNull SBuildServer server,
                                   @NotNull WebControllerManager controllerManager,
                                   @NotNull AuthorizationInterceptor authInterceptor,
                                   @NotNull SecurityContextEx securityContext,
                                   @NotNull MetadataStorage buildMetadataStorage,
                                   @NotNull AuthHelper authHelper) {
    super(server);
    mySecurityContext = securityContext;
    myBuildMetadataStorage = buildMetadataStorage;
    myAuthHelper = authHelper;
    final String path = SymbolsConstants.APP_SYMBOLS + "**";
    controllerManager.registerController(path, this);
    authInterceptor.addPathNotRequiringAuth(path);
  }

  @Nullable
  @Override
  protected ModelAndView doHandle(final @NotNull HttpServletRequest request, final @NotNull HttpServletResponse response) throws Exception {
    final String requestURI = request.getRequestURI().replace("//", "/");

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

    final String valuableUriPart = requestURI.substring(requestURI.indexOf(SymbolsConstants.APP_SYMBOLS) + SymbolsConstants.APP_SYMBOLS.length());
    final int firstDelimiterPosition = valuableUriPart.indexOf('/');

    if(firstDelimiterPosition == -1){
      WebUtil.notFound(request, response, "File not found", null);
      return null;
    }

    final String fileName = valuableUriPart.substring(0, firstDelimiterPosition);
    final String signature = valuableUriPart.substring(firstDelimiterPosition + 1, valuableUriPart.indexOf('/', firstDelimiterPosition + 1));
    final String guid = signature.substring(0, signature.length() - 1).toLowerCase(); //last symbol is PEDebugType
    LOG.debug(String.format("Symbol file requested. File name: %s. Guid: %s.", fileName, guid));

    final String projectId = findRelatedProjectId(guid);
    if(projectId == null) {
      WebUtil.notFound(request, response, "File not found", null);
      return null;
    }

    final SUser user = myAuthHelper.getAuthenticatedUser(request, response, new Predicate<SUser>() {
      public boolean apply(SUser user) {
        return user.isPermissionGrantedForProject(projectId, Permission.VIEW_BUILD_RUNTIME_DATA);
      }
    });
    if (user == null) return null;

    try {
      mySecurityContext.runAs(user, new SecurityContextEx.RunAsAction() {
        public void run() throws Throwable {
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

  @Nullable
  private BuildArtifact findArtifact(String guid, String fileName) {
    final BuildMetadataEntry entry = getMetadataEntry(guid);
    if(entry == null) {
      LOG.debug(String.format("No items found in symbol index for guid '%s'", guid));
      return null;
    }
    final Map<String,String> metadata = entry.getMetadata();
    final String storedFileName = metadata.get(BuildSymbolsIndexProvider.FILE_NAME_KEY);
    final String artifactPath = metadata.get(BuildSymbolsIndexProvider.ARTIFACT_PATH_KEY);
    if(storedFileName == null || artifactPath == null){
      LOG.debug(String.format("Metadata stored for guid '%s' is invalid.", guid));
      return null;
    }
    if(!storedFileName.equalsIgnoreCase(fileName)){
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

  @Nullable
  private String findRelatedProjectId(String symbolFileId) {
    final BuildMetadataEntry metadataEntry = getMetadataEntry(symbolFileId);
    if(metadataEntry == null) {
      LOG.debug(String.format("There is no information about symbol file with id %s in the index.", symbolFileId));
      return null;
    }
    long buildId = metadataEntry.getBuildId();
    final SBuild build = myServer.findBuildInstanceById(buildId);
    if(build == null) {
      LOG.debug(String.format("Failed to find build by id %d. Requested symbol file with id %s expected to be produced by that build.", buildId, symbolFileId));
      return null;
    }
    return build.getProjectId();
  }

  @Nullable
  private BuildMetadataEntry getMetadataEntry(String key){
    final Iterator<BuildMetadataEntry> entryIterator = myBuildMetadataStorage.getEntriesByKey(BuildSymbolsIndexProvider.PROVIDER_ID, key);
    return !entryIterator.hasNext() ? null : entryIterator.next();
  }
}