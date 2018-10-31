package jetbrains.buildServer.symbols;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class PdbSignatureIndexUtilTest {

  @DataProvider(name = "extractGuidFromSignatureData")
  public static Object[][] extractGuidFromSignatureData() {
    return new Object[][]{
      {"8EF4E863187C45E78F4632152CC82FEB1", "8EF4E863187C45E78F4632152CC82FEB"},
      {"8EF4E863187C45E78F4632152CC82FEB", "8EF4E863187C45E78F4632152CC82FEB"},
      {"123", "123"}
    };
  }

  @Test(dataProvider = "extractGuidFromSignatureData")
  void testExtractGuidFromSignature(final String signature, final String expected) {
    Assert.assertEquals(PdbSignatureIndexUtil.extractGuid(signature), expected);
  }
}
