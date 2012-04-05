<%@ page isELIgnored="false"%><%@ page import="com.fluxtream.domain.*"%><%@ page
	import="com.fluxtream.connectors.vos.*"%><%@ page
	import="com.fluxtream.connectors.picasa.*"%><%@ page
	import="com.fluxtream.connectors.flickr.*"%>
<li class="span2"><a href="#" class="thumbnail"> <%
			if (facet instanceof PicasaPhotoFacetVO) {
				/* PICASA ------------------------------------------------------------ */
				PicasaPhotoFacetVO pf = (PicasaPhotoFacetVO) facet;
		%><img style="cursor: pointer"
		onclick="App.showPicture('<%=pf.photoUrl%>')" src="<%=pf.getThumbnail(1)%>" />
		<%
			} else if (facet instanceof FlickrPhotoFacetVO) {
				/* FLICKR ------------------------------------------------------------ */
				FlickrPhotoFacetVO pf = (FlickrPhotoFacetVO) facet;
		%><img style="cursor: pointer" src="<%=pf.photoUrl%>" /> <%
 	}
 %>
</a></li>
