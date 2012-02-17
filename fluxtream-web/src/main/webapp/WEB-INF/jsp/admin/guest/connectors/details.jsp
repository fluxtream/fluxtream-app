<%@ page isELIgnored="false"%><%@ taglib prefix="c"
	uri="http://java.sun.com/jsp/jstl/core"%><%@ page
	import="com.fluxtream.mvc.admin.controllers.*"%><%@ page
	import="java.util.*"%><%@ page import="java.text.*"%><%@ page
	import="com.fluxtream.domain.*"%><%@ page
	import="com.fluxtream.connectors.Connector"%><%@ page
	import="com.fluxtream.connectors.fitbit.*"%><%@ page
	import="org.joda.time.*"%><%@ page import="org.joda.time.format.*"%><%@ page
	import="net.sf.json.*"%>
<%
	AdminHelper helper = (AdminHelper) request.getAttribute("helper");
	String connectorName = (String) request
			.getAttribute("connectorName");
	Long guestId = (Long) request.getAttribute("guestId");
	Guest guest = helper.guestService.getGuestById(guestId);
	DateTimeFormatter format = DateTimeFormat
			.forPattern("dd/MM/yyy, H:mm");
	request.setAttribute("api", Connector.getConnector("fitbit")
			.value());
	FitbitAdminHelper fitbitHelper = (FitbitAdminHelper) helper
			.getHelper("fitbit");
%>

<%
	List<ApiUpdate> lastUpdates = helper.jpaDaoService.find(
			"apiUpdates.last", ApiUpdate.class, 0, 10, guestId,
			Connector.getConnector(connectorName).value());
	List<ApiNotification> lastNotifications = helper.jpaDaoService
			.find("apiNotifications.last", ApiNotification.class, 0,
					10, guestId, Connector.getConnector(connectorName)
							.value());
%>


<c:catch var="e">
	<jsp:include page="${connectorName}.jsp" />
</c:catch>

<div class="row">
	<div class="span4">Last Updates</div>
	<div class="span12">
		<ul>
			<%
				for (int i = 0; i < lastUpdates.size(); i++) {
					ApiUpdate apiUpdate = lastUpdates.get(i);
			%>
			<li>
				<p>
					<span><%=i + 1%>: </span><%=format
						.withZone(DateTimeZone.forID("Europe/Brussels")).print(
								apiUpdate.ts)%>,
					<%=apiUpdate.success ? "success" : "failure"%></p>
				<p>
				<%=apiUpdate.query%>
				</p>
			</li>
			<%
				}
			%>
		
	</div>
</div>

<%
	if (lastNotifications.size() > 0) {
%>
<div class="row">
	<div class="span4">Last Notifications</div>
	<div class="span12">
		<ul>
			<%
				for (int i = 0; i < lastNotifications.size(); i++) {
						ApiNotification apiNotification = lastNotifications.get(i);
			%>
			<li>
				<p>
					<span><%=i + 1%>: </span><%=format.withZone(
							DateTimeZone.forID("Europe/Brussels")).print(
							apiNotification.ts)%></p>
				<p>
				<%=apiNotification.content%>
				</p>
			</li>
			<%
				}
			%>
		
	</div>
</div>
<%
	}
%>
