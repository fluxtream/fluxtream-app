<%@ page import="org.fluxtream.connectors.lastfm.LastFmRecentTrackFacet" %>
<%@ page import="org.fluxtream.connectors.lastfm.LastFmRecentTrackFacetVO" %>
<%@ page pageEncoding="utf-8" contentType="text/html; charset=UTF-8"%><%@ taglib
        prefix="c" uri="http://java.sun.com/jsp/jstl/core"%><%@ taglib
        prefix="sec" uri="http://www.springframework.org/security/tags"%><%@ taglib
        uri="http://granule.com/tags" prefix="g"%><%
    LastFmRecentTrackFacetVO facetVO = (LastFmRecentTrackFacetVO)request.getAttribute("facet");
        %><!DOCTYPE html>
<html prefix="og: http://ogp.me/ns#">
<head>
    <title>Lastfm song</title>
    <meta property="og:type" content="music.song" />
    <% if (facetVO.description!=null) { %>
    <meta property="og:title" content="<%=facetVO.description%>" />
    <% }
       if (facetVO.url!=null) { %>
    <meta property="og:url" content="<%=facetVO.url%>" />
    <meta http-equiv="refresh" content="0; url=<%=facetVO.url%>">
    <% }
        if (facetVO.url!=null) { %>
    <meta property="og:image" content="<%=facetVO.imgUrls[0]%>" />
    <% }
        if (facetVO.comment!=null) { %>
    <meta property="og:description" content="<%=facetVO.comment%>"/>
    <% }
        if (facetVO.description!=null) { %>
    <meta property="og:audio:title" content="<%=facetVO.title%>"/>
    <% }
        if (facetVO.artist!=null) { %>
    <meta property="og:audio:artist" content="<%=facetVO.artist%>"/>
    <% } %>
</head>
</html>
