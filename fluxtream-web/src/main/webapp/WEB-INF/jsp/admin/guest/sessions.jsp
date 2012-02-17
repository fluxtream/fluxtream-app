<%@ page isELIgnored="false"
%><%@ page import="com.fluxtream.mvc.admin.controllers.*"
%><%@ page import="com.fluxtream.domain.*"
%><%
	AdminHelper helper = (AdminHelper) request.getAttribute("helper");
	Long guestId = (Long) request.getAttribute("guestId");
	Guest guest = helper.guestService.getGuestById(guestId);
	helper.guestService.getApiKeys(guest.getId());
%><div class="row">
	<div class="span3">Last session:</div>
</div>
