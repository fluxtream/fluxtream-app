<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"
%><%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"
%><%@ page isELIgnored="false"
%><%@ page import="java.util.*" %><%
	List<String> required = new ArrayList<String>();
	if (request.getAttribute("required")!=null) required = (ArrayList<String>) request.getAttribute("required");
	
	String smsFolderName = "";
	if (request.getAttribute("smsFolderName")!=null) smsFolderName = (String) request.getAttribute("smsFolderName");
	else smsFolderName="SMS";
	
	String callLogFolderName = "";
	if (request.getAttribute("callLogFolderName")!=null) callLogFolderName = (String) request.getAttribute("callLogFolderName");
	else callLogFolderName = "Call log";
	
	String errorMessage = null;
	if (request.getAttribute("errorMessage")!=null) errorMessage = (String) request.getAttribute("errorMessage");
%><p>Please enter your smsBackup mail credentials</p>
<table>
	<% if (errorMessage!=null) { %>
		<tr><td colspan="2"><span class="formError"><%=errorMessage%></span></td></tr>
	<% } %>
	<tr>
	<td>Sms label (in gmail):</td><td><input autocorrect="off" autocapitalize="off" class="focushere" id="smsBackup-smsFolderName" value="<%=smsFolderName%>"
		onkeypress="if(event.which==13) Connectors.submitSmsBackupFolderNames()"></input></td>
	<% if (required.contains("smsFolderName")) { %>
	<td>
		<span class="formError">* SMS label is required</span>
	</td>
	<% } %>
	</tr>
	<tr>
	<td>Call log label (in gmail):</td><td><input autocorrect="off" autocapitalize="off" id="smsBackup-callLogFolderName" value="<%=callLogFolderName%>"
		onkeypress="if(event.which==13) Connectors.submitSmsBackupFolderNames()"></input></td>
	<% if (required.contains("callLogFolderName")) { %>
	<td>
		<span class="formError">* Call Log label is required</span>
	</td>
	<% } %>
	</tr>
	<tr>
	<td colspan="2"><button onclick="submitSmsBackupFolderNames()">Send</button></td>
	</tr>
</table>
