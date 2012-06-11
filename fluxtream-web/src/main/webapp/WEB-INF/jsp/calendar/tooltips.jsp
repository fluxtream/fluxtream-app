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
	DateTimeFormatter shortFormat = DateTimeFormat.forPattern("hh:mm a");
	TimeInterval timeInterval = ((HomeModel)request.getSession().getAttribute("homeModel")).getTimeInterval();
	DateTimeFormatter zeoTimeFormat = DateTimeFormat.forPattern("yyyyMMddHHmm");
	List<AbstractInstantFacetVO> facets = (List<AbstractInstantFacetVO>)request.getAttribute("facets");
    for (AbstractInstantFacetVO facet : facets) {
    %>
      <div class="listViewItem facet-<%=facet.type%>" id="<%=facet.type%>_<%=facet.id%>">
          <div class="flx-toolTipData">
              <%@ include file="facetDetails.jsp" %>
              <ul class="flx-toolTipLinks">
                  <li><a href="#">List <i class="icon-list"></i></a></li>
                  <li><a href="#">Timeline <i class="icon-film"></i></a></li>
                  <li><a href="#">Bodytrack <i class="icon-bookmark"></i></a></li>
              </ul>
          </div>

      </div>
<% } %>
</div>
