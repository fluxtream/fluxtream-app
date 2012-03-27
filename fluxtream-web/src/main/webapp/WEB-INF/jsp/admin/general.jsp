<%@ page isELIgnored="false"%>

<div class="row-fluid">
	<div class="span16">
		<ul class="nav nav-tabs">
			<li <%if(request.getParameter("activeTab").equals("stats")) out.println("class=\"active\""); %>><a href="/admin/general/stats">Stats</a></li>
			<li <%if(request.getParameter("activeTab").equals("tasks")) out.println("class=\"active\""); %>><a href="/admin/general/tasks">Tasks</a></li>
			<li <%if(request.getParameter("activeTab").equals("lucene")) out.println("class=\"active\""); %>><a href="/admin/general/lucene">Lucene Index</a></li>
			<li <%if(request.getParameter("activeTab").equals("connectors")) out.println("class=\"active\""); %>><a href="/admin/general/connectors">Connectors</a></li>
			<li <%if(request.getParameter("activeTab").equals("roles")) out.println("class=\"active\""); %>><a href="/admin/general/roles">Users/Roles</a></li>
		</ul>
	</div>
</div>

<jsp:include page="general/${activeTab}.jsp">
	<jsp:param name="helper" value="${helper}"/>
</jsp:include>		
