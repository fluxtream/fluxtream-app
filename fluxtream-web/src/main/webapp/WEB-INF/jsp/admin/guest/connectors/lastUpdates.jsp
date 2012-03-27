<%@ page isELIgnored="false"
%><%@ page import="com.fluxtream.mvc.admin.controllers.*"
%><%@ page import="java.util.*"
%><%@ page import="java.text.*"
%><%@ page import="com.fluxtream.domain.*"
%><%@ page import="org.joda.time.format.*"
%><%@ page import="com.fluxtream.connectors.picasa.*" %>
<%
	AdminHelper helper = (AdminHelper) request.getAttribute("helper");
	Long guestId = (Long) request.getAttribute("guestId");
	Guest guest = helper.guestService.getGuestById(guestId);
	List<ApiUpdate> apiUpdates = helper.jpaDaoService.find("apiUpdates.last", ApiUpdate.class, guest.getId(), request.getAttribute("api"));
	DateTimeFormatter format = DateTimeFormat.forPattern("dd/MM/yyy, H:mm");
%>

<div class="row-fluid">
	<div class="span4">Last Updates:</div>
	<div class="span12">
		<ul>
	<% for(int i=0; i<apiUpdates.size();i++) { ApiUpdate apiUpdate= apiUpdates.get(i);%>
			<li>
				<p><span><%=i+1 %>: </span><%=format.print(apiUpdate.ts) %>, <%=apiUpdate.success?"success":"failure" %></p>
				<p><pre><%= apiUpdate.query %></pre></p>
			</li>
	<% } %>
		</ul>
	</div>
</div>