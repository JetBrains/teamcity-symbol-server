

package jetbrains.buildServer.symbols;

import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.ExecResult;
import jetbrains.buildServer.symbols.tools.PdbStrExe;
import jetbrains.buildServer.symbols.tools.PdbStrExeCommands;
import jetbrains.buildServer.util.FileUtil;
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
    myTool = new PdbStrExe(new File("aaa"));
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

  public void testRead() throws Exception {
    final File tempFile = createTempFile();
    assertTrue(tempFile.length() == 0);
    ExecResult execResult = myTool.doCommand(PdbStrExeCommands.READ, myIndexedPdbFile, tempFile, PdbStrExe.SRCSRV_STREAM_NAME);
    assertEquals(0, execResult.getExitCode());
    assertFalse(tempFile.length() == 0);
  }

  public void testWrite() throws IOException {
    final File tempFile = createTempFile();
    assertTrue(tempFile.length() == 0);
    myTool.doCommand(PdbStrExeCommands.READ, myNotIndexedPdbFile, tempFile, PdbStrExe.SRCSRV_STREAM_NAME);
    assertTrue(tempFile.length() == 0);

    File inputStreamFile = new File("c:\\temp\\pdb-patch.txt");
    assertFalse(inputStreamFile.length() == 0);
    myTool.doCommand(PdbStrExeCommands.WRITE, myNotIndexedPdbFile, inputStreamFile, PdbStrExe.SRCSRV_STREAM_NAME);

    myTool.doCommand(PdbStrExeCommands.READ, myNotIndexedPdbFile, tempFile, PdbStrExe.SRCSRV_STREAM_NAME);
    assertFalse(tempFile.length() == 0);
  }
}
