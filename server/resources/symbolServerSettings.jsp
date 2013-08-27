<%@ include file="/include-internal.jsp"%>

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