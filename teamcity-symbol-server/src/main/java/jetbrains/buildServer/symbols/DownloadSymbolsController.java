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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import jetbrains.buildServer.controllers.AuthorizationInterceptor;
import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.serverSide.SecurityContextEx;
import jetbrains.buildServer.serverSide.TeamCityProperties;
import jetbrains.buildServer.serverSide.artifacts.BuildArtifact;
import jetbrains.buildServer.serverSide.artifacts.BuildArtifactsViewMode;
import jetbrains.buildServer.serverSide.auth.Permission;
import jetbrains.buildServer.serverSide.metadata.BuildMetadataEntry;
import jetbrains.buildServer.serverSide.metadata.MetadataStorage;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.Predicate;
import jetbrains.buildServer.util.StringUtil;
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
          String.format(".*%s/([^/]+)/([^/]+)", SymbolsConstants.APP_SYMBOLS),
          Pattern.CASE_INSENSITIVE
  );

  @NotNull private final SecurityContextEx mySecurityContext;
  @NotNull private final AuthHelper myAuthHelper;
  private final SymbolsCache mySymbolsCache;
  private final MetadatSourceFactoryImpl myMetadataSourceFactory;

  public DownloadSymbolsController(@NotNull SBuildServer server,
                                   @NotNull WebControllerManager controllerManager,
                                   @NotNull AuthorizationInterceptor authInterceptor,
                                   @NotNull SecurityContextEx securityContext,
                                   @NotNull MetadataStorage buildMetadataStorage,
                                   @NotNull AuthHelper authHelper,
                                   @NotNull SymbolsCache symbolsCache) {
    super(server);
    mySecurityContext = securityContext;
    myAuthHelper = authHelper;
    mySymbolsCache = symbolsCache;
    myMetadataSourceFactory = new MetadatSourceFactoryImpl(buildMetadataStorage);
    final String path = SymbolsConstants.APP_SYMBOLS + "/**";
    controllerManager.registerController(path, this);
    authInterceptor.addPathNotRequiringAuth(path);
  }

  @Nullable
  @Override
  protected ModelAndView doHandle(final @NotNull HttpServletRequest request, final @NotNull HttpServletResponse response) throws Exception {
    final String requestURI = StringUtil.removeTailingSlash(request.getRequestURI());

    if (requestURI.endsWith(SymbolsConstants.APP_SYMBOLS)) {
      return simpleView("TeamCity symbol server is running");
    }

    if (requestURI.endsWith(COMPRESSED_FILE_EXTENSION)) {
      WebUtil.notFound(request, response, "File not found", null);
      return null;
    }
    if (requestURI.endsWith(FILE_POINTER_FILE_EXTENSION)) {
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

    String guid = PdbSignatureIndexUtil.extractGuid(signature, true);
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

          LOG.debug(String.format("Start sending symbols file. File name: %s. Guid: %s.", fileName, guid));
          BufferedOutputStream output = new BufferedOutputStream(response.getOutputStream());
          try {
            LOG.debug(String.format("Getting artifact stream for symbols file. File name: %s. Guid: %s.", fileName, guid));
            InputStream input = buildArtifact.getInputStream();
            try {
              LOG.debug(String.format("Sending symbols file. File name: %s. Guid: %s.", fileName, guid));
              FileUtil.copyStreams(input, output);
            } finally {
              FileUtil.close(input);
            }
          } finally {
            FileUtil.close(output);
          }
          LOG.debug(String.format("Symbols file successfully transfered. File name: %s. Guid: %s.", fileName, guid));
        }
      });
    } catch (Throwable throwable) {
      LOG.debug(String.format("Failed to send symbols for file %s: %s", fileName, throwable.getMessage()), throwable);
      if (!response.isCommitted()) {
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, throwable.getMessage());
      }
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
    final MetadatSourceFactoryImpl.MetadataSourceImpl metadataSource = myMetadataSourceFactory.create();
    try {
      return mySymbolsCache.getEntry(metadataKey, metadataSource);
    }
    finally {
      metadataSource.release();
    }
  }


  private class MetadatSourceFactoryImpl {
    private final MetadataStorage myBuildMetadataStorage;
    private final Semaphore myConcurrentDataRequestSemaphore = new Semaphore(TeamCityProperties.getInteger(SymbolsConstants.SYMBOLS_SERVER_CACHE_MAXREADREQUESTS, 10), false);

    private MetadatSourceFactoryImpl(@NotNull final MetadataStorage buildMetadataStorage) {
      myBuildMetadataStorage = buildMetadataStorage;
    }

    public MetadataSourceImpl create() {
      return new MetadataSourceImpl();
    }

    public class MetadataSourceImpl implements MetadataSource {
      private boolean lockRequested = false;

      @Override
      public List<BuildMetadataEntry> getEntriesByBuildId(final Long buildId) throws InterruptedException, TimeoutException {
        acquireLockIfNeed();

        final Iterator<BuildMetadataEntry> entriesIterator =
          myBuildMetadataStorage.getBuildEntry(buildId, BuildSymbolsIndexProvider.PROVIDER_ID);

        final ArrayList<BuildMetadataEntry> result = new ArrayList<BuildMetadataEntry>();
        while(entriesIterator.hasNext()) {
          final BuildMetadataEntry entry = entriesIterator.next();
          result.add(entry);
        }
        return result;
      }

      @Override
      public Long getBuildIdByEntryKey(final String key) throws InterruptedException, TimeoutException {
        acquireLockIfNeed();

        final Iterator<BuildMetadataEntry> entryIterator =
          myBuildMetadataStorage.getEntriesByKey(BuildSymbolsIndexProvider.PROVIDER_ID, key);

        if (entryIterator.hasNext()) {
          final BuildMetadataEntry entry = entryIterator.next();
          if (entry != null) {
            return entry.getBuildId();
          }
        }
        return null;
      }

      public void release() {
        releaseLockIfNeed();
      }

      private void acquireLockIfNeed() throws InterruptedException, TimeoutException {
        if (lockRequested) {
          return;
        }

        final int timeout = TeamCityProperties.getInteger(SymbolsConstants.SYMBOLS_SERVER_CACHE_ACQUIRE_LOCK_TIMEOUT, 150);
        if (myConcurrentDataRequestSemaphore.tryAcquire(timeout, TimeUnit.SECONDS)) {
          lockRequested = true;
        } else {
          LOG.warn("Could not acquire read metadata lock during " + timeout + " sec");
          throw new TimeoutException("Could not acquire read metadata lock during " + timeout + " sec");
        }
      }

      private void releaseLockIfNeed() {
        if (!lockRequested) {
          return;
        }
        myConcurrentDataRequestSemaphore.release();
        lockRequested = false;
      }
    }
  }
}
