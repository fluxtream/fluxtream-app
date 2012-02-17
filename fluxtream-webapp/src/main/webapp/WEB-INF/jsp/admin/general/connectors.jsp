<%@ page isELIgnored="false"%>
<%@ page import="com.fluxtream.mvc.admin.controllers.*"%>
<%@ page import="com.fluxtream.connectors.*"%>
<%@ page import="java.util.*"%>

<%
	AdminHelper helper = (AdminHelper) request.getAttribute("helper");
%>

<% if (request.getAttribute("settings")==null) { %>
<ul class="pills" style="margin-top:-30px">
	<li <%if(request.getAttribute("activePill").equals("stats")) out.println("class=\"active\""); %>><a href="/admin/general/connectors/stats">Overview</a></li>
	<li <%if(request.getAttribute("activePill").equals("layout")) out.println("class=\"active\""); %>><a href="/admin/general/connectors/layout">Layout</a></li>
</ul>
<% } %>

<jsp:include page="connectors/${activePill}.jsp">
	<jsp:param name="helper" value="${helper}"/>
</jsp:include>		
