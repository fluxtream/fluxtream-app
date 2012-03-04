<%@ page isELIgnored="false"
%><%@ page import="com.fluxtream.mvc.admin.controllers.*" 
%><%@ page import="java.util.*"
%><%@ page import="java.text.*"
%><%@ page import="com.fluxtream.domain.*"
%><%@ page import="com.fluxtream.connectors.Connector"
%><%@ page import="com.fluxtream.connectors.fitbit.*"
%><%@ page import="org.joda.time.format.*"
%><%@ page import="com.fluxtream.utils.Utils"
%><%@ page import="com.fluxtream.utils.HttpUtils"
%><%@ page import="java.net.URLEncoder"
%><%@ page import="net.sf.json.*"
%><%
	AdminHelper helper = (AdminHelper) request.getAttribute("helper");
	Long guestId = (Long) request.getAttribute("guestId");
	Guest guest = helper.guestService.getGuestById(guestId);
	DateTimeFormatter format = DateTimeFormat.forPattern("dd/MM/yyy, H:mm");
	request.setAttribute("api", Connector.getConnector("fitbit").value());
	FitbitAdminHelper fitbitHelper = (FitbitAdminHelper) helper.getHelper("fitbit");
	String jsonString = "";
	JSONArray toSleep, toActivity, listOfActivityDays, listOfSleepDays;
%>

<div class="row">
	<div class="span3">
		to_sleep
	</div>
	<div class="span13">
	<pre><% if(toSleep!=null) {for(int i=0;i<toSleep.size();i++) { %>
		<%=toSleep.getJSONObject(i).getString("text") %>
	<% }} else out.print("no toSleep value"); %>
	</pre>
	</div>
</div>
<div class="row">
	<div class="span3">
		to_activity
	</div>
	<div class="span13">
	<pre><% if(toActivity!=null) {for(int i=0;i<toActivity.size();i++) { %>
		<%=toActivity.getJSONObject(i).getString("text") %>
	<% }} else out.print("no toActivity value"); %>
	</pre>
	</div>
</div>
<div class="row">
	<div class="span3">
		listOfSleepDays
	</div>
	<div class="span13">
	<pre><% if(listOfSleepDays!=null) {for(int i=0;i<listOfSleepDays.size();i++) { %>
		<%=listOfSleepDays.getJSONObject(i).getString("text") %>
	<% }} else out.print("no listOfSleepDays value"); %>
	</pre>
	</div>
</div>
<div class="row">
	<div class="span3">
		listOfActivityDays
	</div>
	<div class="span13">
	<pre><% if(listOfActivityDays!=null) {for(int i=0;i<listOfActivityDays.size();i++) { %>
		<%=listOfActivityDays.getJSONObject(i).getString("text") %>
	<% }} else out.print("no listOfActivityDays value"); %>
	</pre>
	</div>
</div>
