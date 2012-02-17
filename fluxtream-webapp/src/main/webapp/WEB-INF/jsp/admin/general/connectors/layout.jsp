<%@ page isELIgnored="false"%>
<%@ page import="com.fluxtream.mvc.admin.controllers.*" %>
<%@ page import="com.fluxtream.connectors.*" %>
<%@ page import="java.util.*" %>

<% AdminHelper helper = (AdminHelper) request.getAttribute("helper"); %>

<div class="row">
	<div class="span16">
		<pre><%Collection<Connector> connectors = Connector.getAllConnectors();for(Connector connector : connectors) {out.println(connector.toString().trim());}%></pre>
	</div>
</div>
