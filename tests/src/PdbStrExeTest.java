/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

import com.intellij.openapi.util.io.FileUtil;
import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.ExecResult;
import jetbrains.buildServer.symbols.PdbStrExe;
import jetbrains.buildServer.symbols.PdbStrExeCommand;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

/**
 * @author Evgeniy.Koshkin
 */
public class PdbStrExeTest extends BaseTestCase {

  private PdbStrExe myTool;
  private File myNotIndexedPdbFile;
  private File myIndexedPdbFile;

  @BeforeMethod
  public void setUp() throws Exception {
    myTool = new PdbStrExe();
    File homeDir = createTempDir();

    File file = new File(homeDir, "notIndexed.pdb");
    FileUtil.copy(new File("c:\\temp\\JetBrains.CommandLine.Symbols.pdb"), file);
    myNotIndexedPdbFile = file;
    assertFalse(myNotIndexedPdbFile.length() == 0);

    file = new File(homeDir, "indexed.pdb");
    FileUtil.copy(new File("c:\\temp\\JetBrains.CommandLine.Symbols.Indexed.pdb"), file);
    myIndexedPdbFile = file;
    assertFalse(myIndexedPdbFile.length() == 0);
  }

  @Test
  public void testRead() throws Exception {
    final File tempFile = createTempFile();
    assertTrue(tempFile.length() == 0);
    ExecResult execResult = myTool.doCommand(PdbStrExeCommand.READ, myIndexedPdbFile, tempFile, PdbStrExe.SRCSRV_STREAM_NAME);
    assertEquals(0, execResult.getExitCode());
    assertFalse(tempFile.length() == 0);
  }

  @Test
  public void testWrite() throws IOException {
    final File tempFile = createTempFile();
    assertTrue(tempFile.length() == 0);
    myTool.doCommand(PdbStrExeCommand.READ, myNotIndexedPdbFile, tempFile, PdbStrExe.SRCSRV_STREAM_NAME);
    assertTrue(tempFile.length() == 0);

    File inputStreamFile = new File("c:\\temp\\pdb-patch.txt");
    assertFalse(inputStreamFile.length() == 0);
    myTool.doCommand(PdbStrExeCommand.WRITE, myNotIndexedPdbFile, inputStreamFile, PdbStrExe.SRCSRV_STREAM_NAME);

    myTool.doCommand(PdbStrExeCommand.READ, myNotIndexedPdbFile, tempFile, PdbStrExe.SRCSRV_STREAM_NAME);
    assertFalse(tempFile.length() == 0);
  }
}
