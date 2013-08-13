<%@ include file="/include-internal.jsp"%>

<jsp:useBean id="isGuestEnabled" scope="request" type="java.lang.Boolean"/>

<table class="runnerFormTable">
  <tr>
    <td colspan="2">
      <em>Symbols and sources indexing will be performed for all symbol files (.pdb) appeared among build artifacts.</em>
    </td>
  </tr>
  <tr>
    <th>Sources Access:</th>
    <td>
      <props:checkboxProperty name="symbols.sources-auth-required"/>
      <label for="symbols.sources-auth-required">Grand access to the sources to authenticated users only.</label>
    </td>
  </tr>
</table>