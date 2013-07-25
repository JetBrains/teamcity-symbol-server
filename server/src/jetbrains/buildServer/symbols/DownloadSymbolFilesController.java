package jetbrains.buildServer.symbols;

import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.serverSide.artifacts.BuildArtifact;
import jetbrains.buildServer.serverSide.artifacts.BuildArtifactsViewMode;
import jetbrains.buildServer.serverSide.metadata.BuildMetadataEntry;
import jetbrains.buildServer.serverSide.metadata.MetadataStorage;
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
public class DownloadSymbolFilesController extends BaseController {

  public static final String APP_SYMBOLS = "/app/symbols/";

  private static final String COMPRESSED_FILE_EXTENSION = "pd_";
  private static final String FILE_POINTER_FILE_EXTENSION = "ptr";

  private static final Logger LOG = Logger.getLogger(DownloadSymbolFilesController.class);

  @NotNull private final MetadataStorage myBuildMetadataStorage;

  public DownloadSymbolFilesController(@NotNull SBuildServer server, @NotNull WebControllerManager controllerManager, @NotNull MetadataStorage buildMetadataStorage) {
    super(server);
    myBuildMetadataStorage = buildMetadataStorage;
    controllerManager.registerController(APP_SYMBOLS + "**", this);
  }

  @Nullable
  @Override
  protected ModelAndView doHandle(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response) throws Exception {
    final String requestURI = request.getRequestURI();
    if(requestURI.endsWith(COMPRESSED_FILE_EXTENSION)){
      WebUtil.notFound(request, response, "File not found", null);
      return null;
    }
    if(requestURI.endsWith(FILE_POINTER_FILE_EXTENSION)){
      WebUtil.notFound(request, response, "File not found", null);
      return null;
    }

    final String valuableUriPart = requestURI.substring(requestURI.indexOf(APP_SYMBOLS) + APP_SYMBOLS.length());
    final int firstDelimiterPosition = valuableUriPart.indexOf('/');
    final String fileName = valuableUriPart.substring(0, firstDelimiterPosition);
    final String guid = valuableUriPart.substring(firstDelimiterPosition + 1, valuableUriPart.indexOf('/', firstDelimiterPosition + 1));
    LOG.debug(String.format("Symbol file requested. File name: %s. Guid: %s.", fileName, guid));

    final BuildArtifact buildArtifact = findArtifact(guid, fileName);
    if(buildArtifact == null){
      WebUtil.notFound(request, response, "Symbol file not found", null);
      LOG.debug(String.format("Symbol file not found. File name: %s. Guid: %s.", fileName, guid));
      return null;
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