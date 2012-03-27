<%@ page isELIgnored="false"
%><%@ page import="com.fluxtream.mvc.admin.controllers.*"
%><%@ page import="com.fluxtream.domain.*"
%><%@ page import="java.util.*"
%><%
	AdminHelper helper = (AdminHelper) request.getAttribute("helper");

	List<Guest> guests = helper.guestService.getAllGuests();
	long guestId=0;
	if (request.getAttribute("guestId")!=null)
		guestId = (Long) request.getAttribute("guestId");
%><div class="row-fluid">
	<div class="span16">
		<select name="guestSelect" id="guestSelect" onchange="selectGuest()">
			<option value="">Please choose a guest...</option>
			<%
				for (Guest nextGuest : guests) {
					List<ApiKey> keys = helper.guestService.getApiKeys(nextGuest.getId());
					out.println("<option ");
					if (guestId>0 && nextGuest.getId()==guestId)
						out.println("selected=\"selected\"");
					out.println("value=\"" + nextGuest.getId() + "\">"
							+ nextGuest.username + " (" + keys.size() + ")</option>");
				}
			%>
		</select>
	</div>
</div>

<% if (guestId>0) { %>

<jsp:include page="guest.jsp">
	<jsp:param name="guestId" value="${guestId}"/>
	<jsp:param name="helper" value="${helper}"/>
</jsp:include>

<% } %>
