<%@ page isELIgnored="false"%>
<%@ page import="com.fluxtream.mvc.admin.controllers.*" %>

<% AdminHelper helper = (AdminHelper) request.getAttribute("helper"); %>

<div class="row">
	<div class="span3">Number of Guests:</div>
	<div class="span13"><%= helper.guestService.getAllGuests().size() %></div>
</div>
<div class="row">
	<div class="span3">Environment:</div>
	<div class="span13"><%= helper.env.get("environment") %></div>
</div>
