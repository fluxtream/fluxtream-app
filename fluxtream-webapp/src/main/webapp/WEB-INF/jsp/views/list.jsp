<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"
%><%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"
%><%@ page isELIgnored="false"
%><%@ page import="com.fluxtream.*"
%><%@ page import="com.fluxtream.domain.*"
%><%@ page import="com.fluxtream.mvc.models.*"
%><%@ page import="java.util.List" %>
<h5 style="padding:0px 0 0 10px;">Timeline</h5>	
<div id="tooltip" style="position:absolute">&nbsp;</div><section class="timelineCont">
	<div style="display:none" id="graphics" class="timeline"></div>
</section>

<section class="listViewCont" id="facetsList" style="min-height:600px;">
</section>
<%
boolean hasFitbit = (Boolean)request.getAttribute("hasFitbit");
boolean hasWeight = (Boolean)request.getAttribute("hasWeight");
List<ApiKey> userConnectors = (List<ApiKey>) request.getAttribute("userConnectors");
%><% if (hasFitbit||hasWeight) {%>
<section class="sideBarBlock">
	<h4>Body</h4>
	<% if (hasFitbit) {%>
	<article id="fitbitSteps">
		<div class="detail"><small>Steps<br>taken</small></div>
		<span id="stepsTaken">&nbsp;</span>
	</article>
	<article id="fitbitCalories" class="noRmargin">
		<div class="detail"><small>Calories<br>burned</small></div>
		<span id="caloriesOut">&nbsp;</span>
	</article><% } %><% if (hasWeight) {%>
	<div>
		<div class="detail"><small>Last<br>Weight</small></div>
		<span id="lastWeight">&nbsp;</span>
	</div><% } %>
</section><% } %>
<section class="sideBarBlock mapSbar noRmargin">
	<h4 style="float:left">Map</h4><div id="geotime" style="float:right;padding-top:7px"></div>
	<div id="the_map" style="width:100%; height:220px;border:1px solid #B2B2B2;"></div>
</section>
<section class="sideBarBlock" id="weatherPanel" style="display:none">
	<h4>Weather</h4>
	<article style="width:60%">
		<div class="bigger bold"><span id="weatherTemp" ></span></div>
		<span style="line-height:8px;font-size:80%" id="weatherDesc" class="bold lightgrey"></span>
	</article>
	<article>
		<div class="detail"><img id="weatherIcon"></img></div>
	</article>
	<hr class="space">
	<article>
		<div class="detail"><small>Wind Speed</small></div>
		<span id="windSpeed"></span>
	</article>
	<article>
		<div class="detail"><small>Humidity</small></div>
		<span id="weatherHumidity"></span>
	</article>
	<article class="noRmargin">
		<div class="detail"><small>Precipitation</small></div>
		<span id="weatherPrecipitation"></span>
	</article>
</section>
<div style="clear:both;"></div>
<div id="tooltips" style="display:none">
</div>
<c:if test="${userConnectors.size()==1}">
	<script>connectors()</script>
</c:if>
<c:if test="${userConnectors.size()==2}">
	<script>setTimeout("connectorstip()", 400);</script>
</c:if>
<script>$(".contMainComment").show()</script>