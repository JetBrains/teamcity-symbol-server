<%@ include file="/include-internal.jsp"%>



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
