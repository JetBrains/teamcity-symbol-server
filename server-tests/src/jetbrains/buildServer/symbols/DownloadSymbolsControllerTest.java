package jetbrains.buildServer.symbols;/*
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

import jetbrains.buildServer.controllers.AuthorizationInterceptor;
import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.controllers.BaseControllerTestCase;
import jetbrains.buildServer.controllers.interceptors.auth.HttpAuthenticationManager;
import jetbrains.buildServer.serverSide.RunningBuildEx;
import jetbrains.buildServer.serverSide.auth.Permission;
import jetbrains.buildServer.serverSide.auth.RoleScope;
import jetbrains.buildServer.users.SUser;
import org.apache.commons.httpclient.HttpStatus;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

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
    myFixture.getServerSettings().setGuestLoginAllowed(true);
  }

  @Override
  protected BaseController createController() throws IOException {
    AuthorizationInterceptor authInterceptor = myFixture.getSingletonService(AuthorizationInterceptor.class);
    AuthHelper authHelper = new AuthHelper(myFixture.getServerSettings(), myFixture.getUserModel(), myFixture.getSingletonService(HttpAuthenticationManager.class));
    return new DownloadSymbolsController(myServer, myWebManager, authInterceptor,  myFixture.getSecurityContext(), myBuildMetadataStorage, authHelper);
  }

  @Test
  public void request_pdb_simple() throws Throwable {
    myFixture.getServerSettings().setPerProjectPermissionsEnabled(true);
    SUser user = myFixture.getUserModel().getGuestUser();
    user.addRole(RoleScope.projectScope(myProject.getProjectId()), getProjectDevRole());
    assertTrue(user.isPermissionGrantedForProject(myProject.getProjectId(), Permission.VIEW_BUILD_RUNTIME_DATA));

    myRequest.setRequestURI("mock", getRegisterPdbUrl("secur32.pdb", "8EF4E863187C45E78F4632152CC82FEB"));
    doGet();
    assertEquals(HttpStatus.SC_OK, myResponse.getStatus());
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
    myFixture.getServerSettings().setGuestLoginAllowed(false);
    myRequest.setRequestURI("mock", getRegisterPdbUrl("secur32.pdb", "8EF4E863187C45E78F4632152CC82FEB"));
    doGet();
    assertEquals(HttpStatus.SC_UNAUTHORIZED, myResponse.getStatus());
  }

  @Test
  public void request_pdb_no_permissions_granted() throws Exception {
    myRequest.setRequestURI("mock", getRegisterPdbUrl("secur32.pdb", "8EF4E863187C45E78F4632152CC82FEB"));
    doGet();
    assertEquals(HttpStatus.SC_UNAUTHORIZED, myResponse.getStatus());
  }

  private String getRegisterPdbUrl(String fileName, String fileSignature) throws IOException {
    final File artDirectory = createTempDir();
    new File(artDirectory, "foo").createNewFile();
    myBuildType.setArtifactPaths(artDirectory.getAbsolutePath());
    RunningBuildEx build = startBuild();
    myBuildMetadataStorage.addEntry(build.getBuildId(), fileName, fileSignature);
    return String.format("/app/symbols/%s/%s/%s", fileName, fileSignature, fileName);
  }
}
