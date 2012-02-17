<%@ page isELIgnored="false"
%><%@ page import="com.fluxtream.mvc.admin.controllers.*"
%><%@ page import="java.util.*"
%><%@ page import="java.text.*"
%><%@ page import="com.fluxtream.domain.*"
%><%@ page import="org.joda.time.format.*"
%><%@ page import="com.fluxtream.connectors.Connector"
%><%@ page import="com.fluxtream.connectors.picasa.*" %>

<%
	AdminHelper helper = (AdminHelper) request.getAttribute("helper");
	Long guestId = (Long) request.getAttribute("guestId");
	Guest guest = helper.guestService.getGuestById(guestId);
	List<PicasaPhotoFacet> photos = helper.jpaDaoService.find("picasa.photo.all", PicasaPhotoFacet.class, guest.getId());
	request.setAttribute("api", Connector.getConnector("picasa").value());
	DateTimeFormatter format = DateTimeFormat.forPattern("dd/MM/yyy, H:mm");
%>

<div class="row">
	<div class="span4">Recent photos</div>
	<div class="span12">
		<ul class="media-grid">
	<%
		for(PicasaPhotoFacet photo:photos) {
	%>
			<li>
				<a href="#">
					<img class="thumbnail" title="<%= format.print(photo.start) %>" src="<%= photo.thumbnailUrl %>"/>
					<div><%= format.print(photo.start) %></div>
				</a>
			</li>
	<% } %>
		</ul>
	</div>
</div>