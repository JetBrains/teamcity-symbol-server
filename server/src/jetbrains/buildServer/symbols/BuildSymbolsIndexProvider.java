package jetbrains.buildServer.symbols;

import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.artifacts.BuildArtifact;
import jetbrains.buildServer.serverSide.artifacts.BuildArtifacts;
import jetbrains.buildServer.serverSide.artifacts.BuildArtifactsViewMode;
import jetbrains.buildServer.serverSide.metadata.BuildMetadataProvider;
import jetbrains.buildServer.serverSide.metadata.MetadataStorageWriter;
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

  private static final Logger LOG = Logger.getLogger(BuildSymbolsIndexProvider.class);
  private static final String ID = "symbols-index-provider";

  @NotNull
  public String getProviderId() {
    return ID;
  }

  public void generateMedatadata(@NotNull SBuild sBuild, @NotNull MetadataStorageWriter metadataStorageWriter) {
    final BuildArtifact symbols = sBuild.getArtifacts(BuildArtifactsViewMode.VIEW_HIDDEN_ONLY).getArtifact(".teamcity/symbols");
    if(symbols == null){
      LOG.debug("Build with id " + sBuild.getBuildId() + " doesn't provide symbols index data.");
      return;
    }
    for(BuildArtifact childArtifact : symbols.getChildren()){
      if (!childArtifact.getName().startsWith("symbol-signatures")) continue;
      Map<String, String> indexData = Collections.emptyMap();
      try {
        indexData = readIndex(childArtifact.getInputStream());
      } catch (IOException e) {
        LOG.debug("Failed to read symbols index data from artifact " + childArtifact.getRelativePath(), e);
      }
      for (String sign : indexData.keySet()){
        final String fileName = indexData.get(sign);
        final String artifactPath = locateArtifact(sBuild, fileName);
        if(artifactPath == null){
          LOG.debug(String.format("Failed to find artifact by name. BuildId - %d. Artifact name - %s.", sBuild.getBuildId(), fileName));
          continue;
        }
        final HashMap<String, String> data = new HashMap<String, String>();
        data.put("file-name", fileName);
        data.put("artifact-path", artifactPath);
        metadataStorageWriter.addParameters(sign, data);
      }
    }
  }

  private Map<String, String> readIndex(InputStream inputStream) throws IOException {
    SAXBuilder builder = new SAXBuilder();
    try {
      Document document = builder.build(inputStream);
      Map<String, String> result = new HashMap<String, String>();
      for (Object signElementObject : document.getRootElement().getChildren()){
        final Element signElement = (Element) signElementObject;
        result.put(signElement.getAttributeValue("sign"), signElement.getAttributeValue("file"));
      }
      return result;
    } catch (JDOMException e) {
      LOG.debug(e);
      return null;
    }
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
