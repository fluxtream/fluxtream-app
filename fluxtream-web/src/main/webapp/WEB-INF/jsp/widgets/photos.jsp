<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%><%@ taglib
	prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%><%@ page
	isELIgnored="false"%><%@ page import="com.fluxtream.domain.*"%>
<%@ page import="com.fluxtream.connectors.vos.*"
%><%@ page import="com.fluxtream.connectors.picasa.*"
%><%@ page import="com.fluxtream.connectors.flickr.*"
%><%@ page import="com.fluxtream.*"
%><%@ page import="java.util.*"
%><%@ page import="com.fluxtream.connectors.vos.*"%>
<%
List<AbstractPhotoFacetVO> facets = (List<AbstractPhotoFacetVO>)request.getAttribute("facets");
if (facets.size()>0) {
%>

<ul class="thumbnails">
<% for (int i=0; i<facets.size(); i++) {
	AbstractPhotoFacetVO facet = facets.get(i);
%>


<li class="span2"><a href="#" class="thumbnail"> <%
			if (facet instanceof PicasaPhotoFacetVO) {
				/* PICASA ------------------------------------------------------------ */
				PicasaPhotoFacetVO pf = (PicasaPhotoFacetVO) facet;
		%><img style="cursor: pointer"
		onclick="App.showCarousel(<%=i%>)" src="<%=pf.getThumbnail(1)%>" />
		<%
			} else if (facet instanceof FlickrPhotoFacetVO) {
				/* FLICKR ------------------------------------------------------------ */
				FlickrPhotoFacetVO pf = (FlickrPhotoFacetVO) facet;
		%><img style="cursor: pointer" src="<%=pf.photoUrl%>" /> <%
 	}
 %>
</a></li>


<%}%>
</ul>
<% } else { %>
<div class="emptyList">(empty list)</div>
<% } %>

<div class="modal fade" style="display:none" id="carouselModal">
	<div class="modal-header">
		<a href="#" class="close" data-dismiss="modal">&times;</a>
	</div>
	<div class="modal-body" style="overflow:hidden">
		<div id="photosCarousel" class="carousel">
			<div class="carousel-inner">
			<% for (int i=0; i<facets.size(); i++) {
				AbstractPhotoFacetVO facet = facets.get(i);
			%>
				<div id="photo-<%=i%>" class="item<% if (i==0) out.print(" active");%>" style="overflow:none">
					<img src="<%=facet.photoUrl%>"/>
				</div>
		  	<% } %></div>
		  <!-- Carousel nav -->
		  <% if (facets.size()>1) { %>
		  <a class="carousel-control left" href="#photosCarousel" data-slide="prev">&lsaquo;</a>
		  <a class="carousel-control right" href="#photosCarousel" data-slide="next">&rsaquo;</a>
		  <% } %>
		</div>
	</div>
	<div class="modal-footer">
	</div>
</div>