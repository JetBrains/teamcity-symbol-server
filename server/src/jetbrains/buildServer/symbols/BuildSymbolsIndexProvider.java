package jetbrains.buildServer.symbols;

import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.artifacts.BuildArtifact;
import jetbrains.buildServer.serverSide.artifacts.BuildArtifacts;
import jetbrains.buildServer.serverSide.artifacts.BuildArtifactsViewMode;
import jetbrains.buildServer.serverSide.metadata.BuildMetadataProvider;
import jetbrains.buildServer.serverSide.metadata.MetadataStorageWriter;
import jetbrains.buildServer.util.CollectionsUtil;
import jetbrains.buildServer.util.filters.Filter;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Evgeniy.Koshkin
 */
public class BuildSymbolsIndexProvider implements BuildMetadataProvider {

  public static final String PROVIDER_ID = "symbols-index-provider";
  public static final String FILE_NAME_KEY = "file-name";
  public static final String ARTIFACT_PATH_KEY = "artifact-path";

  private static final Logger LOG = Logger.getLogger(BuildSymbolsIndexProvider.class);

  @NotNull
  public String getProviderId() {
    return PROVIDER_ID;
  }

  public void generateMedatadata(@NotNull SBuild sBuild, @NotNull MetadataStorageWriter metadataStorageWriter) {
    final long buildId = sBuild.getBuildId();

    final BuildArtifact symbols = sBuild.getArtifacts(BuildArtifactsViewMode.VIEW_HIDDEN_ONLY).getArtifact(".teamcity/symbols");
    final BuildArtifact symbolSignaturesSource = symbols == null ? null : CollectionsUtil.findFirst(symbols.getChildren(), new Filter<BuildArtifact>() {
      public boolean accept(@NotNull BuildArtifact data) {
        return data.getName().startsWith("symbol-signatures");
      }
    });
    if(symbolSignaturesSource == null) {
      LOG.debug("Build with id " + buildId + " doesn't provide symbols index data.");
      return;
    }

    Map<String, String> indexData = Collections.emptyMap();
    try {
      indexData = readIndex(symbolSignaturesSource.getInputStream());
    } catch (IOException e) {
      LOG.debug("Failed to read symbols index data from artifact " + symbolSignaturesSource.getRelativePath(), e);
    } catch (JDOMException e) {
      LOG.debug("Failed to read symbols index data from artifact " + symbolSignaturesSource.getRelativePath(), e);
    }

    LOG.debug(String.format("Build with id %d provides %d symbol file signatures.", buildId, indexData.size()));

    for (String signature : indexData.keySet()) {
      final String fileName = indexData.get(signature);
      final String artifactPath = locateArtifact(sBuild, fileName);
      if (artifactPath == null) {
        LOG.debug(String.format("Failed to find artifact by name. BuildId - %d. Artifact name - %s.", buildId, fileName));
        continue;
      }
      final HashMap<String, String> data = new HashMap<String, String>();
      data.put(ARTIFACT_PATH_KEY, artifactPath);
      data.put(FILE_NAME_KEY, fileName);
      metadataStorageWriter.addParameters(signature, data);
      LOG.debug("Stored symbol file signature " + signature + " for file name " + fileName);
    }
  }

  @NotNull
  private Map<String, String> readIndex(InputStream inputStream) throws JDOMException, IOException {
    SAXBuilder builder = new SAXBuilder();
    Document document = builder.build(inputStream);
    Map<String, String> result = new HashMap<String, String>();
    for (Object signElementObject : document.getRootElement().getChildren()){
      final Element signElement = (Element) signElementObject;
      result.put(extractGuid(signElement.getAttributeValue("sign")), signElement.getAttributeValue("file"));
    }
    return result;
  }

  private String extractGuid(String sign) {
    return sign.substring(0, sign.length() - 1).toLowerCase(); //last symbol is PEDebugType
  }

  private String locateArtifact(SBuild build, final String fileName) {
    final AtomicReference<String> locatedArtifactPath = new AtomicReference<String>(null);
    build.getArtifacts(BuildArtifactsViewMode.VIEW_DEFAULT_WITH_ARCHIVES_CONTENT).iterateArtifacts(new BuildArtifacts.BuildArtifactsProcessor() {
      @NotNull
      public Continuation processBuildArtifact(@NotNull BuildArtifact artifact) {
        if(artifact.getName().equals(fileName)){
          locatedArtifactPath.set(artifact.getRelativePath());
          return Continuation.BREAK;
        }
        else return Continuation.CONTINUE;
      }
    });
    return locatedArtifactPath.get();
  }
}
