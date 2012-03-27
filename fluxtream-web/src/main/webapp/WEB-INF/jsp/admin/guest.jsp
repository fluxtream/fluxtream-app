<%@ page isELIgnored="false"
%><%@ page import="com.fluxtream.mvc.admin.controllers.*"
%><%@ page import="com.fluxtream.domain.*"
%><%@ page import="java.util.*"
%><%
	AdminHelper helper = (AdminHelper) request.getAttribute("helper");
	long guestId = (Long) request.getAttribute("guestId");
	String activeTab = (String) request.getAttribute("activeTab");
%><% if (request.getAttribute("activeTab")!=null) { %>
<div class="row-fluid">
	<div class="span16">
		<ul class="nav nav-tabs">
			<li <%if(request.getAttribute("activeTab").equals("general")) out.println("class=\"active\""); %>><a href="/admin/guests/${guestId}/general">General</a></li>
			<li <%if(request.getAttribute("activeTab").equals("connectors")) out.println("class=\"active\""); %>><a href="/admin/guests/${guestId}/connectors">Connectors</a></li>
			<li <%if(request.getAttribute("activeTab").equals("sessions")) out.println("class=\"active\""); %>><a href="/admin/guests/${guestId}/sessions">Sessions</a></li>
		</ul>
	</div>
</div>

<jsp:include page="guest/${activeTab}.jsp">
	<jsp:param name="helper" value="${helper}"/>
</jsp:include>	

<% } else { %>

<jsp:include page="guest/connectors/details.jsp">
	<jsp:param name="helper" value="${helper}"/>
	<jsp:param name="connectorName" value="${connectorName}"/>
</jsp:include>		

<% } %>


