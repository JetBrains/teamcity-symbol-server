package jetbrains.buildServer.symbols;

import jetbrains.buildServer.util.XmlUtil;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

/**
 * Created by Evgeniy.Koshkin.
 */
class PdbSignatureIndexUtil {
  private static final String SIGN = "sign";
  private static final String FILE_NAME = "file";
  private static final String FILE_PATH = "file-path";
  private static final String FILE_SIGNS = "file-signs";
  private static final String FILE_SIGN_ENTRY = "file-sign-entry";

  @NotNull
  static Set<PdbSignatureIndexEntry> read(@NotNull final InputStream inputStream, final boolean withDebugType) throws JDOMException, IOException {
    final SAXBuilder builder = new SAXBuilder();
    final Document document = builder.build(inputStream);
    final Set<PdbSignatureIndexEntry> result = new HashSet<PdbSignatureIndexEntry>();
    for (Object signElementObject : document.getRootElement().getChildren()){
      final Element signElement = (Element) signElementObject;
      result.add(new PdbSignatureIndexEntry(extractGuid(signElement.getAttributeValue(SIGN), withDebugType), signElement.getAttributeValue(FILE_NAME), signElement.getAttributeValue(FILE_PATH)));
    }
    return result;
  }

  static void write(@NotNull final OutputStream outputStream, @NotNull final Set<PdbSignatureIndexEntry> indexData) throws IOException {
    final Element root = new Element(FILE_SIGNS);
    for (final PdbSignatureIndexEntry indexEntry : indexData){
      final Element entry = new Element(FILE_SIGN_ENTRY);
      entry.setAttribute(SIGN, indexEntry.getGuid());
      entry.setAttribute(FILE_NAME, indexEntry.getFileName());
      String artifactPath = indexEntry.getArtifactPath();
      if(artifactPath != null){
        entry.setAttribute(FILE_PATH, artifactPath);
      }
      root.addContent(entry);
    }
    XmlUtil.saveDocument(new Document(root), outputStream);
  }

  public static String extractGuid(String sign, boolean cutDebugType) {
    if (cutDebugType)
      return sign.substring(0, sign.length() - 1).toLowerCase(); //last symbol is PEDebugType
    else
      return sign.toLowerCase();
  }
}
