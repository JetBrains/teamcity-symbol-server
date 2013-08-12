<%@ include file="/include-internal.jsp"%>

<jsp:useBean id="pageUrl" type="java.lang.String" scope="request"/>
<jsp:useBean id="isGuestEnabled" scope="request" type="java.lang.Boolean"/>
<jsp:useBean id="publicUrl" scope="request" type="java.lang.String" />
<jsp:useBean id="privateUrl" scope="request" type="java.lang.String" />
<jsp:useBean id="actualServerUrl" scope="request" type="java.lang.String" />

<table class="runnerFormTable">
  <tr>
    <td colspan="2">
      <em>Use this URL in Visual Studio and WinDbg settings.</em>
    </td>
  </tr>
  <tr>
    <th>Authenticated URL:</th>
    <td>
      <c:set var="url"><c:url value="${actualServerUrl}${privateUrl}"/></c:set>
      <div><a href="${url}">${url}</a></div>
      <span class="smallNote">Access to the url requires HTTP authentication</span>
    </td>
  </tr>
  <tr>
    <th>Public URL:</th>
    <td>
      <c:choose>
        <c:when test="${not isGuestEnabled}">
          <div>Not available.</div>
            <span class="smallNote">
              Guest user is disabled.
            </span>
            <span class="smallNote">
              You need to enable guest user login in TeamCity <a href="<c:url value="/admin/admin.html?item=auth"/>">Authentication Settings</a> for public url to work.
            </span>
        </c:when>
        <c:otherwise>
          <c:set var="url"><c:url value="${actualServerUrl}${publicUrl}"/></c:set>
          <div><a href="${url}">${url}</a></div>
          <span class="smallNote">No authentication is required.</span>
        </c:otherwise>
      </c:choose>
    </td>
  </tr>
</table>