<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"
%><%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"
%><%@ page isELIgnored="false"
%><%@ page import="java.util.*" %><%
	List<String> required = new ArrayList<String>();
	if (request.getAttribute("required")!=null) required = (ArrayList<String>) request.getAttribute("required");
	
	String accessKey = "";
	if (request.getAttribute("accessKey")!=null) accessKey = (String) request.getAttribute("accessKey");
	
	String secretKey = "";
	if (request.getAttribute("secretKey")!=null) secretKey = (String) request.getAttribute("secretKey");
	
	String errorMessage = null;
	if (request.getAttribute("errorMessage")!=null) errorMessage = (String) request.getAttribute("errorMessage");
%><p>Please enter your OpenPath access and secret keys</p>
<table>
	<% if (errorMessage!=null) { %>
		<tr><td colspan="2"><span class="formError"><%=errorMessage%></span></td></tr>
	<% } %>
	<tr>
	<td>Access Key:</td><td><input autocorrect="off" autocapitalize="off" class="focushere" id="openpath-accessKey" onkeypress="if(event.which==13) Connectors.submitOpenPathKeypair()" value="<%=accessKey%>"></input></td>
	<% if (required.contains("accessKey")) { %>
	<td>
		<span class="formError">* access key is required</span>
	</td>
	<% } %>
	</tr>
	<tr>
	<td>Secret Key:</td><td><input autocorrect="off" autocapitalize="off" id="openpath-secretKey" value="<%=secretKey %>" onkeypress="if(event.which==13) Connectors.submitOpenPathKeypair()"></input></td>
	<% if (required.contains("secretKey")) { %>
	<td>
		<span class="formError">* secret key is required</span>
	</td>
	<% } %>
	</tr>
	<tr>
	<td colspan="2"><button onclick="Connectors.submitOpenPathKeypair()">Send</button></td>
	</tr>
</table>
