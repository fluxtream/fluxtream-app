<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"
%><%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"
%><%@ page isELIgnored="false"
%><%@ page import="java.util.*" %>
<%
	List<String> required = new ArrayList<String>();
	if (request.getAttribute("required")!=null) required = (ArrayList<String>) request.getAttribute("required");
	
	String username = "";
	if (request.getAttribute("username")!=null) username = (String) request.getAttribute("username");
	
	String errorMessage = null;
	if (request.getAttribute("errorMessage")!=null) errorMessage = (String) request.getAttribute("errorMessage");
%><h4>Please enter your withings credentials</h4><br>

<form class="form-horizontal" action="javascript:void(0);">
    <div class="control-group<%if (required.contains("username")){out.print(" error");}%>">
        <label class="control-label" for="withings-username">Username</label>
        <div class="controls">
            <input type="text" id="withings-username" placeholder="Username" value="<%=username%>"
                   onkeypress="if(event.which==13) Connectors.submitWithingsUsernameAndPassword()">
            <% if (required.contains("username")) { %>
            <span class="help-inline">username is required</span>
            <% } %>
        </div>
    </div>
    <div class="control-group<%if (required.contains("password")){out.print(" error");}%>">
        <label class="control-label" for="withings-password">Password</label>
        <div class="controls">
            <input type="password" id="withings-password" placeholder="Password"
                   onkeypress="if(event.which==13) Connectors.submitWithingsUsernameAndPassword()">
            <% if (required.contains("password")) { %>
            <span class="help-inline">password is required</span>
            <% } %>
        </div>
    </div>
    <div class="control-group<% if (errorMessage!=null) {out.print(" error");}%>">
        <div class="controls">
            <% if (errorMessage!=null) { %>
            <span class="help-inline"><%=errorMessage%></span><br><br>
            <% } %>
            <button onclick="Connectors.submitWithingsUsernameAndPassword()" class="btn">Continue</button>
        </div>
    </div>
</form>