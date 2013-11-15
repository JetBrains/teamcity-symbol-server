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

import jetbrains.buildServer.controllers.AuthorizationInterceptor;
import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.controllers.BaseControllerTestCase;
import jetbrains.buildServer.controllers.interceptors.auth.HttpAuthenticationManager;
import jetbrains.buildServer.serverSide.metadata.MetadataStorage;
import jetbrains.buildServer.symbols.AuthHelper;
import jetbrains.buildServer.symbols.DownloadSymbolsController;
import org.apache.commons.httpclient.HttpStatus;
import org.testng.annotations.Test;

import java.io.IOException;

/**
 * @author Evgeniy.Koshkin
 */
public class DownloadSymbolsControllerTest extends BaseControllerTestCase {

  @Override
  protected BaseController createController() throws IOException {
    MetadataStorage buildMetadataStorage = myFixture.getSingletonService(MetadataStorage.class);
    AuthorizationInterceptor authInterceptor = myFixture.getSingletonService(AuthorizationInterceptor.class);
    AuthHelper authHelper = new AuthHelper(myFixture.getServerSettings(), myFixture.getUserModel(), myFixture.getSingletonService(HttpAuthenticationManager.class));
    return new DownloadSymbolsController(myServer, myWebManager, authInterceptor,  myFixture.getSecurityContext(), buildMetadataStorage, authHelper);
  }

  @Test
  public void request_not_existent_pdb() throws Exception {
    myRequest.setRequestURI("mock", "/app/symbols/boo");
    doGet();
    assertEquals(HttpStatus.SC_NOT_FOUND, myResponse.getStatus());
  }
}
