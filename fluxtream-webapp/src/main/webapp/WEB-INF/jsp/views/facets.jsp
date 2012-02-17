<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"
%><%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"
%><%@ page isELIgnored="false"
%><%@ page import="com.fluxtream.*"
%><%@ page import="com.fluxtream.domain.*"
%><%@ page import="java.text.*"
%><%@ page import="org.joda.time.format.*"
%><%@ page import="com.fluxtream.connectors.vos.*"
%><%@ page import="java.util.*" %>

  <%
  	List<AbstractFacetVO<?>> facets = (List<AbstractFacetVO<?>>)request.getAttribute("facets");
    DateTimeFormatter format = DateTimeFormat.forPattern("yyyy/MM/dd 'at' HH:mm:ss z");
	DateTimeFormatter dateFormat = DateTimeFormat.forPattern("yyyy-MM-dd");
  %>

<ul>
<% for (AbstractFacetVO<?> facet : facets) { %>

	<li class="facet-<%=facet.type%>">
		<a href="dateFormat.print(ivo.start)"><% if (facet instanceof AbstractInstantFacetVO) {
			AbstractInstantFacetVO<? extends AbstractFacet> ivo = (AbstractInstantFacetVO<? extends AbstractFacet>) facet;
			out.print(format.print(ivo.start) + ", ");
		} %></a>
		<%= facet.description %>
	</li>

<% } %>
</ul>