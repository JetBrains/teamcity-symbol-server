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
    private static final String FILE = "file";
    private static final String FILE_SIGNS = "file-signs";
    private static final String FILE_SIGN_ENTRY = "file-sign-entry";

    @NotNull
    static Set<PdbSignatureIndexEntry> read(@NotNull final InputStream inputStream) throws JDOMException, IOException {
        final SAXBuilder builder = new SAXBuilder();
        final Document document = builder.build(inputStream);
        final Set<PdbSignatureIndexEntry> result = new HashSet<PdbSignatureIndexEntry>();
        for (Object signElementObject : document.getRootElement().getChildren()){
            final Element signElement = (Element) signElementObject;
            result.add(new PdbSignatureIndexEntry(extractGuid(signElement.getAttributeValue(SIGN)), signElement.getAttributeValue(FILE)));
        }
        return result;
    }

    static void write(@NotNull final OutputStream outputStream, @NotNull final Set<PdbSignatureIndexEntry> indexData) throws IOException {
        final Element root = new Element(FILE_SIGNS);
        for (final PdbSignatureIndexEntry indexEntry : indexData){
            final Element entry = new Element(FILE_SIGN_ENTRY);
            entry.setAttribute(SIGN, indexEntry.getGuid());
            entry.setAttribute(FILE, indexEntry.getArtifactPath());
            root.addContent(root);
        }
        XmlUtil.saveDocument(new Document(root), outputStream);
    }

    private static String extractGuid(String sign) {
        return sign.substring(0, sign.length() - 1).toLowerCase(); //last symbol is PEDebugType
    }
}