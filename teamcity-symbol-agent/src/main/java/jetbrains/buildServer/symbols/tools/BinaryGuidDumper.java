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

package jetbrains.buildServer.symbols.tools;

import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.util.XmlUtil;
import org.jdom.Document;
import org.jdom.Element;

import java.io.*;
import java.util.Collection;

/**
 * Created by maldorasi on 8/8/16.
 */
public class BinaryGuidDumper {
    public static void dumpBinaryGuidsToFile(Collection<File> files, File output, BuildProgressLogger buildLogger) {
        final Element root = new Element("file-signs");
        for (File file : files) {
            RandomAccessFile randomAccess = null;
            try {
                randomAccess = new RandomAccessFile(file, "r");
                //the PE offset is at byte [60,64)
                int peOffset = readIntLE(randomAccess, 60);
                int timestamp = readIntLE(randomAccess, peOffset + 8);
                int size = readIntLE(randomAccess, peOffset + 80);
                String signature = Integer.toHexString(timestamp) + Integer.toHexString(size);
                final Element entry = new Element("file-sign-entry");
                entry.setAttribute("file-path", file.getPath());
                entry.setAttribute("file", file.getName());
                entry.setAttribute("sign", signature);
                root.addContent(entry);
            } catch (IOException e) {
                buildLogger.exception(e);
            } finally {
                if (randomAccess != null)
                    try {
                        randomAccess.close();
                    } catch (IOException e) {
                        //I hate you, checked exceptions
                        buildLogger.exception(e);
                    }
            }
        }
        try {
            XmlUtil.saveDocument(new Document(root), new FileOutputStream(output));
        } catch (IOException e) {
            buildLogger.exception(e);
        }
    }

    private static int readIntLE(RandomAccessFile file, long position) throws IOException {
        file.seek(position);
        byte[] bytes = new byte[4];
        int bytesRead = file.read(bytes);
        if (bytesRead != bytes.length) {
            throw new EOFException();
        }
        return (bytes[0] & 0xff) +
                ((bytes[1] & 0xff) << 8) +
                ((bytes[2] & 0xff) << 16) +
                ((bytes[3] & 0xff) << 24);
    }

}
