<%@ include file="/include-internal.jsp"%>

<jsp:useBean id="isGuestEnabled" scope="request" type="java.lang.Boolean"/>
<jsp:useBean id="publicFeedUrl" scope="request" type="java.lang.String" />
<jsp:useBean id="actualServerUrl" scope="request" type="java.lang.String" />

<table class="runnerFormTable">
  <tr>
    <td colspan="2">
      <em>Symbols and sources indexing will be performed for all symbol files (.pdb) appeared among build artifacts.</em>
    </td>
  </tr>
  <tr>
    <th>Symbol Server URL:</th>
    <td>
      <c:choose>
        <c:when test="${not isGuestEnabled}">
          <div>Not available.</div>
            <span class="smallNote">
              Guest user is disabled.
            </span>
            <span class="smallNote">
              You need to enable guest user login in TeamCity <a href="<c:url value="/admin/admin.html?item=auth"/>">Authentication Settings</a> for Symbol Server to work.
            </span>
        </c:when>
        <c:otherwise>
          <c:set var="url"><c:url value="${actualServerUrl}${publicFeedUrl}"/></c:set>
          <div><a href="${url}">${url}</a></div>
          <span class="smallNote">Use this URL in Visual Studio and WinDbg settings.</span>
        </c:otherwise>
      </c:choose>
    </td>
  </tr>
</table>