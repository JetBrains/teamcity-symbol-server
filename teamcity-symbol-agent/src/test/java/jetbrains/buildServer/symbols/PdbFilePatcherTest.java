

package jetbrains.buildServer.symbols;

import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.agent.NullBuildProgressLogger;
import jetbrains.buildServer.symbols.tools.JetSymbolsExe;
import jetbrains.buildServer.symbols.tools.PdbStrExe;
import jetbrains.buildServer.symbols.tools.SrcToolExe;
import jetbrains.buildServer.util.FileUtil;
import org.testng.annotations.BeforeMethod;

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
    File homeDir = new File("../jet-symbols/out").getCanonicalFile();
    assertTrue("Failed to find JetSymbolsExe home dir on path " + homeDir.getAbsolutePath(), homeDir.isDirectory());
    JetSymbolsExe exe = new JetSymbolsExe(homeDir);
    PdbFilePatcherAdapterFactory patcheAdapterFactory = new PdbFilePatcherAdapterFactoryImpl(
      null,
      new NullBuildProgressLogger(),
      new PdbStrExe(myTestHomeDir),
      exe,
      new SrcToolExe(myTestHomeDir));
    myPatcher = new PdbFilePatcher(
      myTestHomeDir,
      exe,
      patcheAdapterFactory,
      new NullBuildProgressLogger());
  }

  public void testFoo() throws Exception {
    File tempFile = new File(myTestHomeDir, "tmp.pdb");
    FileUtil.copy(new File("c:\\temp\\JetBrains.CommandLine.Symbols.pdb"), tempFile);
    myPatcher.patch(tempFile);
  }
}
