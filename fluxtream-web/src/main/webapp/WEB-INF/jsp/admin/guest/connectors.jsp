<%@ page isELIgnored="false"
%><%@ page import="com.fluxtream.mvc.admin.controllers.*"
%><%@ page import="com.fluxtream.connectors.*"
%><%@ page import="com.fluxtream.domain.*"
%><%@ page import="java.util.*"
%><%@ page import="org.joda.time.format.*"
%><%
	AdminHelper helper = (AdminHelper) request.getAttribute("helper");
	Long guestId = (Long) request.getAttribute("guestId");
	Guest guest = helper.guestService.getGuestById(guestId);
	List<ApiKey> apiKeys = helper.guestService.getApiKeys(guest.getId());
	DateTimeFormatter format = DateTimeFormat.forPattern("dd/MM/yyy, H:mm");
%><div class="row-fluid">
	<% if (request.getAttribute("successMessage")!=null) { %>
	<div id="messageBox" class="span12">
		<div class="alert alert-success">
		<a class="close" href="javascript:$('#messageBox').hide()">�</a>
		<p><%=request.getAttribute("successMessage") %></p>
		</div>
	</div>
	<script>setTimeout("hideMessageBox()", 5000)</script>
	<% } else if (request.getAttribute("errorMessage")!=null) { %>
	<div id="messageBox" class="span12">
		<div class="alert alert-error">
		<a class="close" href="javascript:$('#messageBox').hide()">�</a>
		<p><%=request.getAttribute("errorMessage") %></p>
		<p><%=request.getAttribute("stackTrace") %></p>
		</div>
	</div>
	<% } %>
</div><% for (ApiKey apiKey : apiKeys) { %>
<div class="row-fluid connector">
	<div class="span3">
		<img src="/${release}/admin/images/connectors/<%=apiKey.getConnector().getName()%>.jpeg"/>
		<p><a href="/admin/guests/<%=guestId%>/connectors/<%=apiKey.getConnector().getName() %>">More...</a></a></p>
		<% if (helper.env.connectors.getProperty(apiKey.getConnector().getName()+".rateLimit")!=null) { %>
		<p>
			Rate Limit: <%= helper.env.connectors.getProperty(apiKey.getConnector().getName()+".rateLimit") %>
		</p>
		<% } %>
		
		<% long count = helper.jpaDaoService.countFacets(apiKey.getConnector(), guestId); %>
		
		<p>Count: <%=count %></p>
		
	</div>
	<div class="span8">
	<table>
		<tr><td width="200px">Initial History completed:</td><td>
		<%
			Connector connector = apiKey.getConnector();
			List<String> icons = new ArrayList<String>();
			List<String> labels = new ArrayList<String>();
			List<Integer> failedObjectTypeValues = new ArrayList<Integer>();
			int[] objectTypeValues = connector.objectTypeValues();
			for (int objectTypes : objectTypeValues) {
				boolean b = helper.connectorUpdateService.isHistoryUpdateCompleted(guest.getId(), connector.getName(), objectTypes);
				if (!b) failedObjectTypeValues.add(objectTypes);
				String release = (String)request.getAttribute("release");
				icons.add(b? ("/"+release+"/admin/images/icons/check.png"):"/"+release+"/admin/images/icons/uncheck.png");
				labels.add(helper.getLabelForObjectTypes(connector, objectTypes));
			}
			for (int i=0;i<icons.size();i++) {
				if (icons.size()>1) {
					if (i>0) out.print(" | ");
					out.print(labels.get(i) + ": ");
				}
				out.println("<img src=\"" + icons.get(i) + "\"/>");
			}
		%>
		</td></tr>
 		<% if (failedObjectTypeValues.size()>0) {
 			out.println("<tr><td>Next scheduled updateWorkerTask:</td><td>");
			
			for (int i=0; i<failedObjectTypeValues.size(); i++) {
				int objectTypes = failedObjectTypeValues.get(i);
				UpdateWorkerTask updateWorkerTask = helper.connectorUpdateService.getNextScheduledUpdateTask(guest.getId(), connector, objectTypes);
				if (i>0) out.println(" | ");
				out.print(helper.getLabelForObjectTypes(connector, objectTypes) + ": ");
				out.print(updateWorkerTask ==null?"-":format.print(updateWorkerTask.timeScheduled));
			}
			
 			out.println("</td></tr>");
 		}
 		%>
 		<tr><td>Last successful updateWorkerTask:</td><td>
 		<%
 		for (int i=0; i<objectTypeValues.length; i++) {
			int objectTypes = objectTypeValues[i];
			ApiUpdate apiUpdate = helper.connectorUpdateService.getLastSuccessfulUpdate(guest.getId(), connector, objectTypes);
			if (i>0) out.println(" | ");
			out.print(helper.getLabelForObjectTypes(connector, objectTypes) + ": ");
			out.print(apiUpdate==null?"-":format.print(apiUpdate.ts));
 		}
 		%>
 		</td></tr>
	</table>
	<div>
	<a href="/admin/scheduleUpdate?guestId=<%=guest.getId() %>&connectorName=<%=connector.getName() %>" class="btn small"><img src="/${release}/admin/images/icons/timer.png"/><span>Request History Update Now</span></a>
	<a href="/admin/updateConnectorData?guestId=<%=guest.getId() %>&connectorName=<%=connector.getName() %>" class="btn small"><img src="/${release}/admin/images/icons/cloud_download.png"/><span>Update Connector Data Now</span></a>
	<a href="/admin/resetConnector?guestId=<%=guest.getId() %>&connectorName=<%=connector.getName() %>" class="btn small"><img src="/${release}/admin/images/icons/recycle.png"/><span>Reset this Connector</span></a>
	</div>
	</div>
</div><% } %>