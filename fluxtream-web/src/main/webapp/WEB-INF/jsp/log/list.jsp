<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"
%><%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"
%><%@ page isELIgnored="false"
%><%@ page import="com.fluxtream.*"
%><%@ page import="com.fluxtream.mvc.models.*"
%><%@ page import="com.fluxtream.domain.*"
%><%@ page import="com.fluxtream.connectors.vos.*"
%><%@ page import="com.fluxtream.utils.Utils"
%><%@ page import="org.joda.time.format.*"
%><%@ page import="java.util.*"
%><%@ page import="com.ocpsoft.pretty.time.PrettyTime"
%><%
	List<AbstractInstantFacetVO<?>> facets = (List<AbstractInstantFacetVO<?>>)request.getAttribute("facets");
	FacetsHelper facetsHelper = (FacetsHelper) request.getAttribute("helper");
	GuestSettings settings = (GuestSettings) request.getAttribute("settings");

	TimeZone timezone = ((HomeModel)request.getSession().getAttribute("homeModel")).getTimeZone();
	TimeInterval timeInterval = ((HomeModel)request.getSession().getAttribute("homeModel")).getTimeInterval();
	
	DateTimeFormatter format = (DateTimeFormatter) request.getAttribute("format");
	DateTimeFormatter shortFormat = DateTimeFormat.forPattern("HH:mm");
	DateTimeFormatter dateFormat = DateTimeFormat.forPattern("yyyy-MM-dd");
	DateTimeFormatter zeoTimeFormat = DateTimeFormat.forPattern("yyyyMMddHHmm");
	
    int from = (Integer) request.getAttribute("from");
    int to = (Integer) request.getAttribute("to");
    int pageNumber = (Integer) request.getAttribute("page");
    int pageSize = (Integer) request.getAttribute("pageSize");
    int manyPages = (Integer) request.getAttribute("manyPages");
    String nextPrevFunction = request.getAttribute("searchTerms")!=null?"getSearchResults":"getFacets";
    int total = (Integer) request.getAttribute("total");%>


<% if (request.getAttribute("searchTerms")!=null) { %>
<p><h5><%=total %> events</h5> match with your search terms : <strong><%=request.getAttribute("searchTerms") %></strong></p>
<% } else { %>
<h5><%=total %> events</h5>
<% } %>

<%if (total>pageSize) { %>
<div class="pagination"><ul>
	<li><a class="paginationLink" pageNumber="<%=pageNumber-1%>">&laquo; Previous</a></li>

<% if (pageSize<total) {
	for (int i=0; i<(total/pageSize)+1; i++) {
		if (pageNumber!=i) {%>
<li><a class="paginationLink" pageNumber="<%=i%>"><%=i+1 %></a></li>
		<% } else { %>
<li class="active"><a href=""><%=i+1 %></a></li>	
<% }}} %>

<% if (to%pageSize==0) { %>
	<li><a class="paginationLink" pageNumber="<%=pageNumber+1%>">Next &raquo;</a></li>
<% } else if(total>pageSize) { %>
	<li><a class="paginationLink disabled" pageNumber="<%=pageNumber%>">Next &raquo;</a></li>
<% } %>
</ul></div>
<% } %>
<% if (request.getAttribute("userConnectors")!=null) {
    Set<String> haveDataConnectors = (Set<String>) request.getAttribute("haveDataConnectors");
    List<String> selectedConnectors = (List<String>) request.getAttribute("selectedConnectors");
    List<ConnectorModel> userConnectors = (List<ConnectorModel>) request.getAttribute("userConnectors"); %>
<div id="selectedConnectors">
	<% for (ConnectorModel connector : userConnectors) {
		boolean hasData = haveDataConnectors.contains(connector.connectorName);
		boolean isSelected = selectedConnectors.contains(connector.connectorName);%>
		<button class="btnList btn <% if (isSelected) out.print("btnListChecked"); else out.print("btn-inverse"); %> <% if (!hasData) out.print("disabled"); else out.print("enabled"); %>"
		value="<%=connector.connectorName%>"><%=connector.prettyName %></button>
	<% } %>
</div>
<% } %>

<div id="listView">

<% if (facets.size()>0) { %>
		<% for (AbstractInstantFacetVO<?> facet : facets) { %>
      <div class="listViewItem facet-<%=facet.type%>" type="<%=facet.type%>" id="<%=facet.id%>">
      	<a class="listViewAddCom"><div class="iconComment"></div></a>
      	  
		  <%@ include file="facetDetails.jsp" %>
		  
	      <div class="listViewItemComm" <% if (facet.comment!=null) out.println("style=\"display:block\""); %>>
		    
		    <div class="listViewItemCommNew" <% if (facet.comment!=null) out.println("style=\"display:none\""); %>>
					<div class="iconCommArrow"></div>
					<div class="iconCommDelete"></div>
					<div class="iconCommCont"><div class="iconComm"></div></div>
	      	<input onkeypress="if(event.which==13) setEventComment(this)"
	      			class="commentList placeholder empty" title="Comment this event..."
	      			type="text" id="" pageNumber="Comment this event...">
	      </div>
      	<article <% if (facet.comment==null) out.println("style=\"display:none\""); %> onclick="editEventComment(this);" >
      	  		<div class="listCommArrow"></div>
      	  		<div class="listCommArrow-border"></div>
      	  		<div class="iconComment"></div>
      	    	<div class="CommContent"><%=facet.comment%></div>
      	</article>
	      </div>
      </div>
      <div class="flx-spacer"></div>
	<% } %>
	
<% } else if (request.getAttribute("searchTerms")==null) { %>
	<span class="nodata">Sorry, no data today</span>
<% } %>
</div>
<%if (total>pageSize) { %>

<div class="pagination"><ul>
<li><a class="paginationLink" pageNumber="<%=pageNumber-1%>">&laquo; Previous</a></li>

<% if (pageSize<total) {
	for (int i=0; i<(total/pageSize)+1; i++) {
		if (pageNumber!=i) {%>
<li><a class="paginationLink"pageNumber="<%=i%>"><%=i+1 %></a></li>
		<% } else { %>
<li class="active"><a href=""><%=i+1 %></a></li>	
<% }}} %>

<% if (to%pageSize==0) { %>
	<li><a class="paginationLink" pageNumber="<%=pageNumber+1%>">Next &raquo;</a></li>
<% } else if(total>pageSize) { %>
	<li><a class="paginationLink disabled" pageNumber="<%=pageNumber%>">Next &raquo;</a></li>
<% }} %>
</ul></div>
