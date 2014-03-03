<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%><%@ taglib
	prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%><%@ page
	isELIgnored="false"%><%@ page import="org.fluxtream.domain.*"%>
<%@ page import="org.fluxtream.connectors.vos.*"
%><%@ page import="glacier.picasa.*"
%><%@ page import="org.fluxtream.connectors.flickr.*"
%><%@ page import="org.fluxtream.*"
%><%@ page import="java.util.*"
%>
<%
List<AbstractPhotoFacetVO> facets = (List<AbstractPhotoFacetVO>)request.getAttribute("facets");
if (facets.size()>0) {
%>
<div>
<ul class="thumbnails">
<% for (int i=0; i<facets.size(); i++) {
	AbstractPhotoFacetVO facet = facets.get(i);
%>


<li class="span2"><a class="thumbnail"> <%
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
</div>
<div class="flx-spacer"></div>
<% } else { %>
<div class="emptyList">(empty list)</div>
<% } %>