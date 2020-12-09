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

<jsp:useBean id="bean" class="jetbrains.buildServer.symbols.IndexSymbolsBean" scope="request"/>
<jsp:useBean id="buildForm" type="jetbrains.buildServer.controllers.admin.projects.EditableBuildTypeSettingsForm" scope="request"/>

<table class="runnerFormTable featureDetails">
  <tr>
    <td colspan="2">
      <em>Symbols and sources indexing will be performed for all symbol files (.pdb) published as build artifacts.</em>
    </td>
  </tr>
  <tr>
    <th><label for="${bean.branchFilterKey}">Index in branches:</label></th>
    <td>
      <c:set var="note">
        Newline-delimited set of rules in the form of +|-:logical branch name (with an optional * placeholder)<bs:help file="Branch+Filter"/>
      </c:set>
      <props:multilineProperty name="${bean.branchFilterKey}" linkTitle="Branch filter" cols="60" rows="3" className="longField" expanded="true" note="${note}"/>
      <span class="error" id="error_${bean.branchFilterKey}"/>

      <script type="text/javascript">
        BS.BranchesPopup.attachHandler('${buildForm.settingsId}', '${bean.branchFilterKey}');
      </script>
    </td>
  </tr>
</table>
