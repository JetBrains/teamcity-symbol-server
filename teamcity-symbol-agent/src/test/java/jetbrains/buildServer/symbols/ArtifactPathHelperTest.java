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

import jetbrains.buildServer.ExtensionHolder;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.InternalPropertiesHolder;
import jetbrains.buildServer.agent.impl.artifacts.ArchivePreprocessor;
import jetbrains.buildServer.agent.impl.artifacts.ZipPreprocessor;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Collections;

public class ArtifactPathHelperTest {

  @Test(dataProvider = "testPaths")
  public void testPathTransformation(String prefix, String fileName, String expectedPath) {
    Mockery m = new Mockery();
    ExtensionHolder extensionHolder = m.mock(ExtensionHolder.class);
    BuildProgressLogger logger = m.mock(BuildProgressLogger.class);
    InternalPropertiesHolder propertiesHolder = m.mock(InternalPropertiesHolder.class);
    ArtifactPathHelper helper = new ArtifactPathHelper(extensionHolder);
    ZipPreprocessor zipPreprocessor = new ZipPreprocessor(logger, new File("."), propertiesHolder);

    m.checking(new Expectations(){{
      allowing(extensionHolder).getExtensions(with(ArchivePreprocessor.class));
      will(returnValue(Collections.singletonList(zipPreprocessor)));
    }});

    Assert.assertEquals(helper.concatenateArtifactPath(prefix, fileName), expectedPath);
  }

  @DataProvider
  public Object[][] testPaths() {
    return new Object[][]{
      {"", "file.pdb", "file.pdb"},
      {"path/to", "file.pdb", "path/to/file.pdb"},
      {"archive.zip", "file.pdb", "archive.zip!/file.pdb"},
      {"archive.zip/path/to", "file.pdb", "archive.zip!/path/to/file.pdb"}
    };
  }
}
