<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%><%@ taglib
	prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%><%@ page
	isELIgnored="false"%><%@ page import="com.fluxtream.*"
%><%@ page import="java.util.*"
%><%@ page import="com.fluxtream.connectors.vos.*"%><ul class="thumbnails">
<%
List<AbstractFacetVO> facets = (List<AbstractFacetVO>)request.getAttribute("facets");

for (AbstractFacetVO facet : facets) {%>
	<%@ include file="imageFacetDetails.jsp"%>
<%}%>
</ul>