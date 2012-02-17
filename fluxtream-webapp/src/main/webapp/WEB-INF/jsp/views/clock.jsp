<%@ page pageEncoding="utf-8" contentType="text/html; charset=UTF-8"
%><%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"
%><%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"
%><%@ page isELIgnored="false"
%><%@ page import="java.util.*"
%><%@ page import="com.fluxtream.mvc.models.*"
%><%@ page import="com.fluxtream.*"
%><%@ page import="com.fluxtream.domain.*"
%><%@ page import="com.fluxtream.connectors.picasa.*" %>


<div id="tooltip" style="position:absolute">&nbsp;</div>
<section class="donutsCont">
	<div class="clockHelpIcon"><a href="javascript:showClockHelp()"><i>&nbsp;</i></a></div>
	<div style="margin-top:6px"><h4 id="mainCity"></h4></div>
<ul id="selectedConnectors">
	<%
    List<ApiKey> userConnectors = (List<ApiKey>) request.getAttribute("userConnectors");
    List<ConnectorModel> userConnectorModels = (List<ConnectorModel>) request.getAttribute("userConnectorModels");
	for (ConnectorModel connector : userConnectorModels) {
		%>
		<li><a class="btnList <%="btn-"+connector.connectorName%> btnListChecked"
		value="<%=connector.connectorName%>"><%=connector.prettyName %></a></li>
	<% } %>
</ul>

	<div id="graphics" class="donut"></div>
</section><%
boolean hasFitbit = (Boolean)request.getAttribute("hasFitbit");
boolean hasWeight = (Boolean)request.getAttribute("hasWeight");
boolean hasPictures = (Boolean)request.getAttribute("hasPictures");
%><div class="sideBarCont"><% if (hasFitbit||hasWeight) {%>
<section class="sideBarBlock" id="bodyPanel">
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
	</div><% } %></section><% } %>
<section class="sideBarBlock mapSbar">
	<h4 style="float:left">Map</h4><div id="geotime" style="float:right;padding-top:7px"></div>
	<div id="the_map" style="width:100%; height:220px;border:1px solid #B2B2B2;"></div>
	<div id="setAddressLink" style="display:none;margin-top:10px">
		<div style="display:inline;background-image:url(/${release}/images/icon-warning-small.gif);width:32px;height:32px">&nbsp;&nbsp;&nbsp;&nbsp;</div>
		<a style="font-size:80%" href="javascript:setAddress()">Your home address is not set...</a>
	</div>
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

<section class="sideBarBlock" id="picturePanel" style="display:none">
	<h4>Pictures</h4>
	<div id="pictureDiv">
	</div>
</section>

<div style="clear:both;"></div>
</div>

<div id="tooltips" style="display:none">
</div>
<c:if test="${userConnectors.size()==1}">
	<script>connectors()</script>
</c:if>
<c:if test="${userConnectors.size()==2}">
	<script>setTimeout("connectorstip()", 400);</script>
</c:if>
<script>$(".contMainComment").show()</script>