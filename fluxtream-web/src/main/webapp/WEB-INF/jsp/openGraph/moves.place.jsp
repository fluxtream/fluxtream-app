<%@ page import="org.fluxtream.connectors.moves.MovesPlaceFacetVO" %>
<%@ page import="org.fluxtream.core.services.MetadataService" %>
<%@ page import="net.sf.json.JSONArray" %>
<%@ page import="net.sf.json.JSONObject" %>
<%@ page pageEncoding="utf-8" contentType="text/html; charset=UTF-8"%><%@ taglib
        prefix="c" uri="http://java.sun.com/jsp/jstl/core"%><%@ taglib
        prefix="sec" uri="http://www.springframework.org/security/tags"%><%@ taglib
        uri="http://granule.com/tags" prefix="g"%><%
    MovesPlaceFacetVO facetVO = (MovesPlaceFacetVO)request.getAttribute("facet");
    MetadataService metadataService = (MetadataService)request.getAttribute("metadataService");
    String type = "unknown";
    String name = "N/A";
    String latitude = null, longitude = null;
    String url = "https://foursquare.com/v/" + facetVO.foursquareId;
    final JSONObject foursquareVenue = metadataService.getFoursquareVenueJSON(facetVO.foursquareId);
    if (foursquareVenue!=null) {
        final JSONObject jsonResponse = foursquareVenue.getJSONObject("response");
        if (jsonResponse!=null) {
            final JSONObject jsonVenue = jsonResponse.getJSONObject("venue");
            if (jsonVenue!=null) {
                final JSONObject jsonLocation = jsonVenue.getJSONObject("location");
                if (jsonLocation!=null) {
                    if (jsonLocation.has("lat"))
                        latitude = String.valueOf(jsonLocation.getDouble("lat"));
                    if (jsonLocation.has("lng"))
                        longitude = String.valueOf(jsonLocation.getDouble("lng"));
                }
                url = jsonVenue.getString("canonicalUrl");
                name = jsonVenue.getString("name");
                final JSONArray categories = jsonVenue.getJSONArray("categories");
                if (categories.size()>0) {
                    JSONObject jsonCategory = categories.getJSONObject(0);
                    type = jsonCategory.getString("shortName");
                }
            }
        }
    }

%><!DOCTYPE html>
<html prefix="og: http://ogp.me/ns#">
<head>
    <title>Moves place</title>
    <meta property="og:type" content="<%=type%>" />
    <% if (facetVO.description!=null) { %>
    <meta property="og:title" content="<%=name%>" />
    <% }
       if (url!=null) { %>
    <meta property="og:url" content="<%=url%>" />
    <meta http-equiv="refresh" content="0; url=<%=url%>">
    <% }
       if (facetVO.comment!=null) { %>
    <meta property="og:description" content="<%=facetVO.comment%>"/>
    <% }
       if (latitude!=null && longitude!=null) { %>
    <meta property="og:latitude" content="<%=latitude%>"/>
    <meta property="og:longitude" content="<%=longitude%>"/>
    <% }%>
</head>
</html>
