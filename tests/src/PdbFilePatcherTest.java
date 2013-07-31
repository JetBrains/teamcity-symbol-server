import com.intellij.openapi.util.io.FileUtil;
import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.symbols.PdbFilePatcher;
import jetbrains.buildServer.symbols.SrcSrvStreamProvider;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

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
    myPatcher = new PdbFilePatcher(myTestHomeDir, new SrcSrvStreamProvider(1111, new File("c:\\Data\\Work\\TeamCity\\trunk\\symbols-native")));
  }

  @Test
  public void testFoo() throws IOException {
    File tempFile = new File(myTestHomeDir, "tmp.pdb");
    FileUtil.copy(new File("c:\\temp\\JetBrains.CommandLine.Symbols.pdb"), tempFile);
    myPatcher.patch(tempFile);
  }
}
