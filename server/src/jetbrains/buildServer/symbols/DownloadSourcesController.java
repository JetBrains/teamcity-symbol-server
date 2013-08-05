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

package jetbrains.buildServer.symbols;

import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import jetbrains.buildServer.web.util.WebUtil;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Evgeniy.Koshkin
 */
public class DownloadSourcesController extends BaseController {
  private static final String APP_SOURCES = "/app/sources/";
  private static final String VALID_URL_PATTERN = ".*/builds/id-\\d*/sources/.*";

  private static final Logger LOG = Logger.getLogger(DownloadSourcesController.class);

  public DownloadSourcesController(@NotNull SBuildServer server, @NotNull WebControllerManager controllerManager) {
    super(server);
    controllerManager.registerController(APP_SOURCES + "**", this);
  }

  @Nullable
  @Override
  protected ModelAndView doHandle(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response) throws Exception {
    final String requestURI = request.getRequestURI();
    if(!requestURI.matches(VALID_URL_PATTERN)){
      WebUtil.notFound(request, response, "Url is invalid", null);
      return null;
    }
    String restMethodUrl = requestURI.replace("/builds/id-", "/builds/id:").replace("/app/sources/", "/app/rest/");
    final String contextPath = request.getContextPath();
    if(restMethodUrl.startsWith(contextPath)){
      restMethodUrl = restMethodUrl.substring(contextPath.length());
    }
    RequestDispatcher dispatcher = request.getRequestDispatcher(restMethodUrl);
    if (dispatcher != null) {
      LOG.debug(String.format("Forwarding request. From %s To %s", requestURI, restMethodUrl));
      dispatcher.forward(request, response);
    }
    return null;
  }
}
