

package jetbrains.buildServer.symbols;

import jetbrains.buildServer.controllers.AuthorizationInterceptor;
import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.controllers.BaseControllerTestCase;
import jetbrains.buildServer.controllers.interceptors.auth.HttpAuthenticationManager;
import jetbrains.buildServer.serverSide.RunningBuildEx;
import jetbrains.buildServer.serverSide.auth.Permission;
import jetbrains.buildServer.serverSide.auth.RoleScope;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.util.FileUtil;
import org.apache.commons.httpclient.HttpStatus;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * @author Evgeniy.Koshkin
 */
public class DownloadSymbolsControllerTest extends BaseControllerTestCase {

  private MetadataStorageMock myBuildMetadataStorage;

  @Override
  @BeforeMethod
  public void setUp() throws Exception {
    myBuildMetadataStorage = new MetadataStorageMock();
    super.setUp();
    myFixture.getLoginConfiguration().setGuestLoginAllowed(true);
  }

  @Override
  protected BaseController createController() throws IOException {
    AuthorizationInterceptor authInterceptor = myFixture.getSingletonService(AuthorizationInterceptor.class);
    AuthHelper authHelper = new AuthHelper(myFixture.getLoginConfiguration(), myFixture.getUserModel(), myFixture.getSingletonService(HttpAuthenticationManager.class));
    SymbolsCache symbolsCache = new SymbolsCache(myFixture.getEventDispatcher());
    return new DownloadSymbolsController(myServer, myWebManager, authInterceptor,  myFixture.getSecurityContext(), myBuildMetadataStorage, authHelper, symbolsCache);
  }

  @Test
  public void request_pdb_simple() throws Throwable {
    myFixture.getServerSettings().setPerProjectPermissionsEnabled(true);
    SUser user = myFixture.getUserModel().getGuestUser();
    user.addRole(RoleScope.projectScope(myProject.getProjectId()), getProjectDevRole());
    assertTrue(user.isPermissionGrantedForProject(myProject.getProjectId(), Permission.VIEW_BUILD_RUNTIME_DATA));

    myRequest.setRequestURI("mock", getRegisterPdbUrl("8EF4E863187C45E78F4632152CC82FEB1", "secur32.pdb", "secur32" +
                                                                                                          ".pdb"));

    doGet();

    assertEquals(HttpStatus.SC_NOT_FOUND, myResponse.getStatus());
    assertEquals("Symbol file not found", myResponse.getStatusText());
  }

  @Test
  public void request_pdb_two_slashes_in_url() throws Exception {
    myRequest.setRequestURI("mock", "/app/symbols//index2.txt'");
    doGet();
    assertEquals(HttpStatus.SC_NOT_FOUND, myResponse.getStatus());
  }

  @Test
  public void request_pdb_invalid_url() throws Exception {
    myRequest.setRequestURI("mock", "/app/symbols/foo");
    doGet();
    assertEquals(HttpStatus.SC_NOT_FOUND, myResponse.getStatus());
  }

  @Test
  public void request_not_existent_pdb() throws Exception {
    myRequest.setRequestURI("mock", "/app/symbols/fileName/FileId2/fileName");
    doGet();
    assertEquals(HttpStatus.SC_NOT_FOUND, myResponse.getStatus());
  }

  @Test
  public void request_pdb_unauthorized() throws Exception {
    myFixture.getLoginConfiguration().setGuestLoginAllowed(false);

    final File artDirectory = createTempDir();
    assertTrue(new File(artDirectory, "foo").createNewFile());;

    myBuildType.setArtifactPaths(artDirectory.getAbsolutePath());
    RunningBuildEx build = startBuild();
    finishBuild(build, false);

    final String fileSignature = "8EF4E863187C45E78F4632152CC82FEB1";
    final String guid = "8EF4E863187C45E78F4632152CC82FEB";
    final String fileName = "secur32.pdb";
    final String filePath = "foo/secur32.pdb";

    myBuildMetadataStorage.addEntry(build.getBuildId(), guid.toLowerCase(), fileName, filePath);
    myRequest.setRequestURI("mock", getRegisterPdbUrl(fileSignature, fileName, filePath));
    doGet();
    assertEquals(HttpStatus.SC_UNAUTHORIZED, myResponse.getStatus());
  }

  @Test
  public void request_pdb_no_permissions_granted() throws Exception {
    final File artDirectory = createTempDir();
    assertTrue(new File(artDirectory, "foo").createNewFile());;

    myBuildType.setArtifactPaths(artDirectory.getAbsolutePath());
    RunningBuildEx build = startBuild();
    finishBuild(build, false);

    final String fileSignature = "8EF4E863187C45E78F4632152CC82FEB1";
    final String guid = "8EF4E863187C45E78F4632152CC82FEB";
    final String fileName = "secur32.pdb";
    final String filePath = "foo/secur32.pdb";

    myBuildMetadataStorage.addEntry(build.getBuildId(), guid.toLowerCase(), fileName, filePath);
    myRequest.setRequestURI("mock", getRegisterPdbUrl(fileSignature, fileName, filePath));
    doGet();
    assertEquals(HttpStatus.SC_UNAUTHORIZED, myResponse.getStatus());
  }

  @DataProvider(name = "Booleans")
  public static Object[][] two_bool_combinations() {
    return new Boolean[][]{{false}, {true}};
  }

  @Test(dataProvider = "Booleans")
  public void request_pdb_guid_case_insensitive(boolean lowercaseSignature) throws Exception{
    myFixture.getServerSettings().setPerProjectPermissionsEnabled(true);
    SUser user = myFixture.getUserModel().getGuestUser();
    user.addRole(RoleScope.projectScope(myProject.getProjectId()), getProjectDevRole());
    assertTrue(user.isPermissionGrantedForProject(myProject.getProjectId(), Permission.VIEW_BUILD_RUNTIME_DATA));

    final String fileSignatureUpper = "8EF4E863187C45E78F4632152CC82FEB1";
    final String fileSignature = lowercaseSignature ? fileSignatureUpper.toLowerCase() : fileSignatureUpper;
    final String guid = "8EF4E863187C45E78F4632152CC82FEB";
    final String fileName = "secur32.pdb";
    final String filePath = "foo/secur32.pdb";
    final byte[] fileContent = new byte[]{(byte) (lowercaseSignature ? 1 : 0)};

    RunningBuildEx build = startBuild();
    build.publishArtifact(filePath, fileContent);
    finishBuild(build, false);

    myBuildMetadataStorage.addEntry(build.getBuildId(), guid.toLowerCase(), fileName, filePath);
    myRequest.setRequestURI("mock", String.format("/app/symbols/%s/%s/%s", fileName, fileSignature, fileName));

    doGet();

    assertEquals(-1, myResponse.getStatus());
    assertTrue("Returned data did not match set pdb data", Arrays.equals(fileContent, myResponse.getReturnedBytes()));
  }

  @Test
  public void request_file_with_plus_sign() throws Exception{
    myFixture.getServerSettings().setPerProjectPermissionsEnabled(true);

    SUser user = myFixture.getUserModel().getGuestUser();
    user.addRole(RoleScope.projectScope(myProject.getProjectId()), getProjectDevRole());
    assertTrue(user.isPermissionGrantedForProject(myProject.getProjectId(), Permission.VIEW_BUILD_RUNTIME_DATA));

    final File artDirectory = createTempDir();
    final String fileName = "file++.pdb";
    File file = new File(artDirectory, fileName);
    assertTrue(file.createNewFile());
    FileUtil.writeFile(file, "text", "UTF-8");

    myBuildType.setArtifactPaths(artDirectory.getAbsolutePath());
    RunningBuildEx build = startBuild();
    build.publishArtifact(fileName, file);
    finishBuild(build, false);

    final String fileSignature = "8EF4E863187C45E78F4632152CC82FEB1";
    final String guid = "8EF4E863187C45E78F4632152CC82FEB";

    myBuildMetadataStorage.addEntry(build.getBuildId(), guid.toLowerCase(), fileName, fileName);
    myRequest.setRequestURI("mock", String.format("/app/symbols/%s/%s/%s", fileName, fileSignature, fileName));

    doGet();

    assertEquals(-1, myResponse.getStatus());
    assertEquals("text", myResponse.getReturnedContent());

    myRequest.setRequestURI("mock", String.format("/app/symbols/%s/%s/%s", "file%2b%2b.pdb", fileSignature, fileName));

    doGet();

    assertEquals(-1, myResponse.getStatus());
    assertEquals("text", myResponse.getReturnedContent());
  }

  private String getRegisterPdbUrl(String fileSignature, String fileName, String artifactPath) throws IOException {
    final File artDirectory = createTempDir();
    new File(artDirectory, "foo").createNewFile();
    myBuildType.setArtifactPaths(artDirectory.getAbsolutePath());
    RunningBuildEx build = startBuild();
    myBuildMetadataStorage.addEntry(build.getBuildId(), PdbSignatureIndexUtil.extractGuid(fileSignature, true),
                                    fileName, artifactPath);
    return String.format("/app/symbols/%s/%s/%s", fileName, fileSignature, fileName);
  }
}
