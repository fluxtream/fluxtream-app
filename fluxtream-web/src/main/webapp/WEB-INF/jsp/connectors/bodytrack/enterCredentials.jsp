<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"
%><%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"
%><%@ page isELIgnored="false"%><%@ page import="java.util.*" %>
<%
	List<String> required = new ArrayList<String>();
	if (request.getAttribute("required")!=null) required = (ArrayList<String>) request.getAttribute("required");
	
	String username = "";
	if (request.getAttribute("username")!=null) username = (String) request.getAttribute("username");
	
	String host = "";
	if (request.getAttribute("host")!=null) host = (String) request.getAttribute("host");
	
	String errorMessage = null;
	if (request.getAttribute("errorMessage")!=null) errorMessage = (String) request.getAttribute("errorMessage");
%><p>Please enter your BodyTrack credentials</p>
<table>
	<% if (errorMessage!=null) { %>
		<tr><td colspan="2"><span class="formError"><%=errorMessage%></span></td></tr>
	<% } %>
	<tr>
	<td>Login:</td><td><input autocorrect="off" autocapitalize="off" class="focushere" id="bodytrack-username" value="<%=username%>"></input></td>
	<% if (required.contains("username")) { %>
	<td>
		<span class="formError">* login is required</span>
	</td>
	<% } %>
	</tr>
	<tr>
	<td>Password:</td><td><input autocorrect="off" autocapitalize="off" id="bodytrack-password" type="password" onkeypress="if(event.which==13) Connectors.submitBodytrackCredentials()"></input></td>
	<% if (required.contains("password")) { %>
	<td>
		<span class="formError">* password is required</span>
	</td>
	<% } %>
	</tr>
	<tr>
	<td>Server:</td><td><input autocorrect="off" autocapitalize="off" class="focushere" id="bodytrack-host" value="<%=host%>"></input></td>
	<% if (required.contains("host")) { %>
	<td>
		<span class="formError">* server is required</span>
	</td>
	<% } %>
	</tr>
	<tr>
	<td colspan="2"><button onclick="Connectors.submitBodytrackCredentials()">Send</button></td>
	</tr>
</table>
