<%@ include file="/include-internal.jsp"%>

<%--
  ~ Copyright 2000-2020 JetBrains s.r.o.
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~ http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  --%>

<jsp:useBean id="pageUrl" type="java.lang.String" scope="request"/>
<jsp:useBean id="appUrl" scope="request" type="java.lang.String" />
<jsp:useBean id="actualServerUrl" scope="request" type="java.lang.String" />

<table class="runnerFormTable">
  <tr>
    <th>Symbol Server URL:</th>
    <td>
      <c:set var="url"><c:url value="${actualServerUrl}${appUrl}"/></c:set>
      <div><a href="${url}">${url}</a></div>
      <span class="smallNote">Use this URL in Visual Studio and WinDbg settings.</span>
    </td>
  </tr>
</table>
