<%@ page isELIgnored="false"
%><%@ page import="com.fluxtream.mvc.admin.controllers.*"
%><%@ page import="com.fluxtream.domain.*"
%><%@ page import="java.util.*"
%><%@ page import="com.fluxtream.domain.metadata.*"
%><%
	AdminHelper helper = (AdminHelper) request.getAttribute("helper");
	Long guestId = (Long) request.getAttribute("guestId");
	Guest guest = helper.guestService.getGuestById(guestId);
	helper.guestService.getApiKeys(guest.getId());
%><div class="row-fluid">
	<div class="span3">Username:</div>
	<div class="span13"><%= guest.username %></div>
</div>
<div class="row-fluid">
	<div class="span3">Id:</div>
	<div class="span13"><%= guest.getId() %></div>
</div>
<div class="row-fluid">
	<div class="span3">E-mail:</div>
	<div class="span13"><%= guest.email %></div>
</div>
<div class="row-fluid">
	<div class="span3">Number of days with data:</div>
	<div class="span13"><%=helper.apiDataService.getNumberOfDays(guestId) %></div>
</div>
<div class="row-fluid">
	<div class="span3">Last login from:</div>
	<% DayMetadataFacet lastDay = helper.metadataService.getLastDayMetadata(guestId); %>
	<% String city = null;
	   if (lastDay!=null) {
	   NavigableSet<DayMetadataFacet.VisitedCity> visitedCities = lastDay.getOrderedCities();
	   if (visitedCities!=null&&visitedCities.size()>0) 
	       city = visitedCities.last().name;
	   else
	       city = "["+lastDay.cities+"]";
	%>
	<div class="span13"><%=lastDay.date%>/<%=lastDay.timeZone%>/<%=city!=null?city:"unknown city!"%></div>
	<% } else { %>
	<div class="span13">Nothing to display here - no day metadata present?</div>
	<% } %>
</div>

<div class="row-fluid">
	<div class="span16">
		<a href="/admin/guests/${guestId}/erase" class="btn primary">Erase this guest</a>
	</div>
</div>
