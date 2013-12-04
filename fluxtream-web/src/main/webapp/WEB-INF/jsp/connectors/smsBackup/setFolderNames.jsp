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
%>

<h4>Please enter your smsBackup mail credentials</h4><br>


<form class="form-horizontal" action="javascript:void(0);">
    <div class="control-group<%if (required.contains("smsFolderName")){out.print(" error");}%>">
        <label class="control-label" for="smsBackup-smsFolderName">Sms label (in gmail)</label>
        <div class="controls">
            <input type="text" id="smsBackup-smsFolderName" placeholder="SMS Folder Label"
                   value="<%=smsFolderName%>"
                   onkeypress="if(event.which==13) Connectors.submitSmsBackupFolderNames()">
            <% if (required.contains("smsFolderName")) { %>
            <span class="help-inline">SMS label is required</span>
            <% } %>
        </div>
    </div>
    <div class="control-group<%if (required.contains("callLogFolderName")){out.print(" error");}%>">
        <label class="control-label" for="smsBackup-callLogFolderName">Call log label (in gmail)</label>
        <div class="controls">
            <input type="text" id="smsBackup-callLogFolderName" placeholder="Call Log Label"
                   value="<%=callLogFolderName%>"
                   onkeypress="if(event.which==13) Connectors.submitSmsBackupFolderNames()">
            <% if (required.contains("callLogFolderName")) { %>
            <span class="help-inline">Call Log label is required</span>
            <% } %>
        </div>
    </div>
    <div class="control-group<% if (errorMessage!=null) {out.print(" error");}%>">
        <div class="controls">
            <% if (errorMessage!=null) { %>
            <span class="help-inline"><%=errorMessage%></span><br><br>
            <% } %>
            <button onclick="Connectors.submitSmsBackupFolderNames()" class="btn">Continue</button>
        </div>
    </div>
</form>