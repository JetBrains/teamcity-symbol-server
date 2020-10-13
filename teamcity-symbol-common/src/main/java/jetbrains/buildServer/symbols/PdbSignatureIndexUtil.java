/*
 * Copyright 2000-2020 JetBrains s.r.o.
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

import jetbrains.buildServer.util.StringUtil;
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
  private static final int GUID_SIGN_LENGTH = 32;

  @NotNull
  static Set<PdbSignatureIndexEntry> read(@NotNull final InputStream inputStream, final boolean cutDbgAge) throws JDOMException, IOException {
    final SAXBuilder builder = new SAXBuilder();
    final Document document = builder.build(inputStream);
    final Set<PdbSignatureIndexEntry> result = new HashSet<PdbSignatureIndexEntry>();
    for (Object signElementObject : document.getRootElement().getChildren()){
      final Element signElement = (Element) signElementObject;
      result.add(new PdbSignatureIndexEntry(extractGuid(signElement.getAttributeValue(SIGN), cutDbgAge),
                                            signElement.getAttributeValue(FILE_NAME), signElement.getAttributeValue(FILE_PATH)));
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

  public static String extractGuid(String sign, boolean cutDbgAge) {
    if (cutDbgAge) {
      // Windows signature pdb ends with psb age value (usuall it is 1)
      // for Portable pdb it ends with FFFFFFFF.
      // But Guid is the first one for the all cases
      return StringUtil.truncateStringValue(sign, GUID_SIGN_LENGTH).toLowerCase();
    } else {
      return sign.toLowerCase();
    }
  }
}
