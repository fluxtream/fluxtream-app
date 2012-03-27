<%@ page isELIgnored="false"%>
<%@ page import="com.fluxtream.mvc.admin.controllers.*" %>
<%@ page import="com.fluxtream.mvc.admin.controllers.*"%>
<%@ page import="com.fluxtream.connectors.*"%>
<%@ page import="java.util.*"%>

<% AdminHelper helper = (AdminHelper) request.getAttribute("helper"); %>

<% if (request.getAttribute("settings")!=null) { %>
	<jsp:include page="settings/${settings}.jsp"/>
<% } else { %>
<%
	//TODO: this is a hack because I don't know how to sort in desc order based on the
	// most popular connectors - works but will probably break
	Collection<Connector> allConnectors = Connector.getAllConnectors();
	TreeMap<Long,Connector> byMostPopular = new TreeMap<Long,Connector>();
	for (Connector connector : allConnectors) {
		Long n = helper.connectorUpdateService.getTotalNumberOfGuestsUsingConnector(connector);
		n*=1000000;
		while (byMostPopular.containsKey(n)) n+=1;
		byMostPopular.put(n, connector);
	}
	Collection<Connector> values = byMostPopular.values();
	List<Connector> sorted = new ArrayList<Connector>();
	for(Connector value : values) {
		sorted.add(value);
	}
	for (int i=sorted.size()-1; i>=0; i--) {
		Connector connector = sorted.get(i);
		long now = System.currentTimeMillis();
		long oneHourAgo = (now - 60*60*1000);
		long twentyFourHoursAgo = (now - 24*60*60*1000);
%>

<div class="row connector">
	<div class="span4" style="padding-top:5px">
		<img src="/${release}/admin/images/connectors/<%=connector.getName()%>.jpeg" />
		<% if (helper.env.connectors.get(connector.getName()+".rateLimit")!=null) { %>
			<p>
				<%= helper.env.connectors.get(connector.getName()+".rateLimit") %>
			</p>
		<% } %>
		
<c:catch var="e">
    <c:import url="<%=connector.getName()%>.jsp" />
</c:catch>
<c:if test="${empty e}">
	<p><a href="settings/<%=connector.getName()%>">More...</a></p>
</c:if>
		
	</div>
	<div class="span12">
		<div class="well">
			<div class="row-fluid">
				<div class="span4">Number of guests:</div>
				<div class="span7"><%= helper.connectorUpdateService.getTotalNumberOfGuestsUsingConnector(connector) %></div>
			</div>
			<div class="row-fluid">
				<div class="span4">Total number of API calls:</div>
				<div class="span7"><%= helper.connectorUpdateService.getTotalNumberOfUpdates(connector) %></div>
			</div>
			<div class="row-fluid">
				<div class="span4">Number of API calls /last hour:</div>
				<div class="span7"><%= helper.connectorUpdateService.getTotalNumberOfUpdatesSince(connector, oneHourAgo) %></div>
			</div>
			<div class="row-fluid">
				<div class="span4">Number of API calls /last 24 hours:</div>
				<div class="span7"><%= helper.connectorUpdateService.getTotalNumberOfUpdatesSince(connector, twentyFourHoursAgo) %></div>
			</div>
			<div class="row-fluid">
				<div class="span7">
				<a href="/admin/reinitializeConnectors?connectorName=<%=connector.getName() %>" class="btn small"><img src="/${release}/admin/images/icons/timer.png"/><span>Re-initialize all guests' data</span></a>
				</div>
			</div>
		</div>
	</div>
</div>

<%
	}}
%>
