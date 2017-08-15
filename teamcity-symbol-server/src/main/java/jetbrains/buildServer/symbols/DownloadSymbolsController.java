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
import java.net.URLDecoder;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Evgeniy.Koshkin
 */
public class DownloadSymbolsController extends BaseController {

  private static final Logger LOG = Logger.getLogger(DownloadSymbolsController.class);
  private static final String COMPRESSED_FILE_EXTENSION = "_";
  private static final String FILE_POINTER_FILE_EXTENSION = "ptr";
  private static final Pattern DOWNLOAD_URL_PATTERN = Pattern.compile(
          String.format(".*%s/?([^/]+)/([^/]+)", SymbolsConstants.APP_SYMBOLS),
          Pattern.CASE_INSENSITIVE
  );

  @NotNull private final SecurityContextEx mySecurityContext;
  @NotNull private final MetadataStorage myBuildMetadataStorage;
  @NotNull private final AuthHelper myAuthHelper;
  private final SymbolsCache mySymbolsCache;

  public DownloadSymbolsController(@NotNull SBuildServer server,
                                   @NotNull WebControllerManager controllerManager,
                                   @NotNull AuthorizationInterceptor authInterceptor,
                                   @NotNull SecurityContextEx securityContext,
                                   @NotNull MetadataStorage buildMetadataStorage,
                                   @NotNull AuthHelper authHelper,
                                   @NotNull SymbolsCache symbolsCache) {
    super(server);
    mySecurityContext = securityContext;
    myBuildMetadataStorage = buildMetadataStorage;
    myAuthHelper = authHelper;
    mySymbolsCache = symbolsCache;
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

    final Matcher urlMatcher = DOWNLOAD_URL_PATTERN.matcher(requestURI);
    if (!urlMatcher.find()) {
      WebUtil.notFound(request, response, "File not found", null);
      if (!requestURI.endsWith("index2.txt")) {
        LOG.warn("Invalid request to symbol server: " + requestURI);
      }
      return null;
    }

    final String encodedFileName = urlMatcher.group(1).replaceAll("\\+", "%2b");
    final String fileName = URLDecoder.decode(encodedFileName, "UTF-8");
    final String signature = urlMatcher.group(2).toLowerCase();
    final String guid = PdbSignatureIndexUtil.extractGuid(signature, true);
    LOG.debug(String.format("Symbol file requested. File name: %s. Guid: %s.", fileName, guid));

    final BuildMetadataEntry metadataEntry = getMetadataEntry(guid, fileName);
    if(metadataEntry == null) {
      LOG.debug(String.format("There is no information about symbol file %s with id %s in the index.", fileName, guid));
      WebUtil.notFound(request, response, "File not found", null);
      return null;
    }
    final String projectId = findRelatedProjectId(metadataEntry);
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
          final BuildArtifact buildArtifact = findArtifact(metadataEntry);
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
      LOG.debug(String.format("Failed to send symbols for file %s: %s", fileName, throwable.getMessage()));
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, throwable.getMessage());
    }

    return null;
  }

  @Nullable
  private BuildArtifact findArtifact(@NotNull BuildMetadataEntry entry) {
    final Map<String,String> metadata = entry.getMetadata();
    final String storedArtifactPath = metadata.get(BuildSymbolsIndexProvider.ARTIFACT_PATH_KEY);
    if(storedArtifactPath == null){
      LOG.debug(String.format("Metadata stored for guid '%s' is invalid.", entry.getKey()));
      return null;
    }

    final long buildId = entry.getBuildId();
    final SBuild build = myServer.findBuildInstanceById(buildId);
    if(build == null){
      LOG.debug(String.format("Build not found by id %d.", buildId));
      return null;
    }
    final BuildArtifact buildArtifact = build.getArtifacts(BuildArtifactsViewMode.VIEW_ALL_WITH_ARCHIVES_CONTENT).getArtifact(storedArtifactPath);
    if(buildArtifact == null){
      LOG.debug(String.format("Artifact not found by path %s for build with id %d.", storedArtifactPath, buildId));
    }
    return buildArtifact;
  }

  @Nullable
  private String findRelatedProjectId(@NotNull BuildMetadataEntry metadataEntry) {
    long buildId = metadataEntry.getBuildId();
    final SBuild build = myServer.findBuildInstanceById(buildId);
    if(build == null) {
      LOG.debug(String.format("Failed to find build by id %d. Requested symbol file with id %s expected to be produced by that build.", buildId, metadataEntry.getKey()));
      return null;
    }
    return build.getProjectId();
  }

  @Nullable
  private BuildMetadataEntry getMetadataEntry(@NotNull String signature, @NotNull String fileName){
    final String metadataKey = BuildSymbolsIndexProvider.getMetadataKey(signature, fileName);
    return mySymbolsCache.getEntry(metadataKey, compositeMetadataKey -> {
      Iterator<BuildMetadataEntry> entryIterator = myBuildMetadataStorage.getEntriesByKey(
        BuildSymbolsIndexProvider.PROVIDER_ID, compositeMetadataKey);
      if (entryIterator.hasNext()) {
        return entryIterator.next();
      } else {
        //backward compatibility with old metadata published with signature only key
        entryIterator = myBuildMetadataStorage.getEntriesByKey(BuildSymbolsIndexProvider.PROVIDER_ID, signature);
        while (entryIterator.hasNext()) {
          final BuildMetadataEntry entry = entryIterator.next();
          if (entry == null)
            continue;
          final String entryFileName = entry.getMetadata().get(BuildSymbolsIndexProvider.FILE_NAME_KEY);
          if (fileName.equalsIgnoreCase(entryFileName))
            return entry;
        }
        return null;
      }
    });
  }
}
