<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"
%><%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"
%><%@ page isELIgnored="false"
%><%@ page import="com.fluxtream.*"
%><%@ page import="com.fluxtream.mvc.models.*"
%><%@ page import="com.fluxtream.mvc.controllers.*"
%><%@ page import="com.fluxtream.domain.*"
%><%@ page import="com.fluxtream.connectors.vos.*"
%><%@ page import="com.fluxtream.utils.Utils"
%><%@ page import="java.text.*"
%><%@ page import="java.util.*"
%><%@ page import="org.joda.time.format.*"
%><%@ page import="com.ocpsoft.pretty.time.PrettyTime"
%><div class="listView">
<%
	FacetsHelper facetsHelper = (FacetsHelper) request.getAttribute("helper");
	GuestSettings settings = (GuestSettings) request.getAttribute("settings");
	
	TimeZone timezone = ((HomeModel)request.getSession().getAttribute("homeModel")).getTimeZone();
	DateTimeFormatter format = (DateTimeFormatter) request.getAttribute("format");
	DateTimeFormatter shortFormat = DateTimeFormat.forPattern("HH:mm");
	TimeInterval timeInterval = ((HomeModel)request.getSession().getAttribute("homeModel")).getTimeInterval();
	DateTimeFormatter zeoTimeFormat = DateTimeFormat.forPattern("yyyyMMddHHmm");
	List<AbstractInstantFacetVO> facets = (List<AbstractInstantFacetVO>)request.getAttribute("facets");
    for (AbstractInstantFacetVO facet : facets) {
    %>
      <div class="listViewItem facet-<%=facet.type%>" id="<%=facet.type%>_<%=facet.id%>">
      	<a class="listViewAddCom"><div class="iconComment"></div></a>
      	
		  <%@ include file="facetDetails.jsp" %>
		  
	      <div class="listViewItemComm" <% if (facet.comment!=null) out.println("style=\"display:block\""); %>>
		    
		    <div class="listViewItemCommNew" <% if (facet.comment!=null) out.println("style=\"display:none\""); %>>
					<div class="iconCommDelete"></div>
					<div class="iconCommCont"><div class="iconComm"></div></div>
	      		<input onkeypress="if(event.which==13) setEventComment(this)"
	      			class="commentList placeholder empty" title="Comment this event..."
	      			type="text" id="" value="Comment this event...">
	      	</div>
      	    <article <% if (facet.comment==null) out.println("style=\"display:none\""); %> onclick="editEventComment(this)"><%=facet.comment%></article>
	      </div>
      </div>
<% } %>
</div>
