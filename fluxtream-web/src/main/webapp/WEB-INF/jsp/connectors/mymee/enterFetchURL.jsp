<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"
%><%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"
%><%@ page isELIgnored="false"
%><%@ page import="java.util.*" %><%
	List<String> required = new ArrayList<String>();
	if (request.getAttribute("required")!=null) required = (ArrayList<String>) request.getAttribute("required");
	
	String username = "";
	if (request.getAttribute("username")!=null) username = (String) request.getAttribute("fetchURL");
	
	String errorMessage = null;
	if (request.getAttribute("errorMessage")!=null) errorMessage = (String) request.getAttribute("errorMessage");
%><p>Please enter your Mymee fetch URL (which you received when you created your Mymee account)</p>
<table>
	<% if (errorMessage!=null) { %>
		<tr><td colspan="2"><span class="formError"><%=errorMessage%></span></td></tr>
	<% } %>
	<tr>
	<td>Fetch URL:</td><td><input autocorrect="off" autocapitalize="off" style="width: 450px;"
                                  onkeypress="if(event.which==13) Connectors.submitMymeeFetchURL()"
                                  class="focushere" id="mymee-fetchURL" value="<%=username%>"></td>
	<% if (required.contains("fetchURL")) { %>
	<td>
		<span class="formError">* fetch URL is required</span>
	</td>
	<% } %>
	</tr>
	<tr>
	<td colspan="2"><button onclick="Connectors.submitMymeeFetchURL()">Send</button></td>
	</tr>
</table>
