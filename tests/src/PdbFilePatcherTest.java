import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.symbols.PdbFilePatcher;
import jetbrains.buildServer.symbols.SrcSrvStreamBuilder;
import jetbrains.buildServer.util.FileUtil;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;

/**
 * @author Evgeniy.Koshkin
 */
public class PdbFilePatcherTest extends BaseTestCase {

  private PdbFilePatcher myPatcher;
  private File myTestHomeDir;

  @Override
  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    myTestHomeDir = createTempDir();
    myPatcher = new PdbFilePatcher(myTestHomeDir, new SrcSrvStreamBuilder(null));
  }

  @Test
  public void testFoo() throws Exception {
    File tempFile = new File(myTestHomeDir, "tmp.pdb");
    FileUtil.copy(new File("c:\\temp\\JetBrains.CommandLine.Symbols.pdb"), tempFile);
    myPatcher.patch(tempFile, null);
  }
}
