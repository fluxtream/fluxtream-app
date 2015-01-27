<%@ page import="org.fluxtream.core.auth.AuthHelper" %>
<%@ page pageEncoding="utf-8" contentType="text/html; charset=UTF-8"%><%@ taglib
        uri="http://granule.com/tags" prefix="g"
        %><%@ page import="java.util.List"
        %><%@ page import="org.fluxtream.core.domain.Guest"
        %><%
    List<Guest> allGuests = (List<Guest>)request.getAttribute("allGuests");%><!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">
    <meta name="apple-mobile-web-app-capable" content="yes" />
    <meta name="apple-mobile-web-app-status-bar-style"
          content="black-translucent" />
    <meta name="viewport" content="width=1000, initial-scale=1.0">
    <title>Fluxtream</title>
    <meta name="description" content="">
    <meta name="author" content="">

    <link rel="stylesheet" href="/static/css/bootstrap-2.3.2.min.css">
    <link rel="stylesheet"
          href="/static/css/bootstrap-responsive-2.3.2.min.css">
    <g:compress>
        <link rel="stylesheet" href="/static/css/jquery-ui/jquery-ui-1.10.3.custom.css">
        <link rel="stylesheet"
              href="/static/css/jquery-colorPicker/jquery.colorPicker.css">
        <link rel="stylesheet" href="/static/css/msdropdown/dd.css">
        <link rel="stylesheet" href="/static/css/tagedit/css/jquery.tagedit.css">
    </g:compress>

    <link rel="stylesheet" href="/static/css/font-awesome-3.2.1.css">
    <link href='//fonts.googleapis.com/css?family=Roboto' rel='stylesheet' type='text/css'>
    <link rel="stylesheet" href="/${release}/css/admin.css">

    <script
            src="https://maps-api-ssl.google.com/maps/api/js?libraries=geometry&v=3&sensor=false"
            type="text/javascript"></script>
    <script src="/static/js/hogan-2.0.0.js"></script>

    <link rel="shortcut icon" href="/favicon.ico">
    <script
            src="//ajax.googleapis.com/ajax/libs/jquery/1.10.2/jquery.min.js"></script>
    <script>
        window.jQuery
        || document
                .write('<script src="/static/js/jquery-1.10.2.min.js"><\/script>')
    </script>

    <script src="/static/js/bootstrap-2.3.2.min.js"></script>
    <script src="/static/js/jquery-ui-1.10.3.custom.min.js"></script>
    <script src="/static/js/jquery.ui.sortable-1.8.2-bt-1.0.1.js"></script>
    <script src="/static/js/json2-2011.10.19.js"></script>
    <script src="/static/js/jquery.autoGrowInput-1.0.0.js"></script>
    <script src="/static/js/jquery.colorPicker-2012.01.27.js"></script>
    <script src="/static/js/jquery.dd-2.37.5-uncompressed.js"></script>
    <script src="/static/js/jquery.tagedit-1.2.1.js"></script>
    <script src="/static/js/jquery.shorten-1.0.0.js"></script>
    <script src="/static/js/underscore-1.3.3-min.js"></script>
    <script src="/static/js/backbone-1.0.0-custom.1-min.js"></script>
    <script src="/static/js/jquery.ba-dotimeout-1.0.0.min.js"></script>
    <script src="/static/js/jquery.masonry-2.1.03.min.js"></script>
    <script src="/static/js/jquery.xcolor-1.8.js"></script>
    <script src="/static/js/jquery.outerHTML-1.0.0.js"></script>

    <script>
        window.FLX_RELEASE_NUMBER = "${release}";
    </script>

    <script data-main="/${release}/js/admin/main.js" src="/static/js/require-1.0.3.js"></script>

</head>

<body>
<div id="flxUsername" style="display:none;"><%=AuthHelper.getGuest().username%></div>
<div id="flxUID" style="display:none;"><%=AuthHelper.getGuest().getId()%></div>

<div id="content">

    <div class="navbar">
        <div class="navbar-inner">
            <div class="container-fluid">
                <a class="btn btn-navbar" data-toggle="collapse"
                   data-target=".nav-collapse"> <span class="icon-bar"></span> <span
                        class="icon-bar"></span> <span class="icon-bar"></span>
                </a>
                <a class="brand" href="/admin"><img
                        src="/${release}/images/header-logo-v4.png" width=94 height=20/></a>
            </div>
        </div>
    </div>

    <div  class="container-fluid">

        <p><span class="label label-info">Live serverUUID's: ${serverUUID}</span></p>

        <div class="dropdown">
            <a class="dropdown-toggle" data-toggle="dropdown" href="javascript:void(0)">&lt;Please choose a Guest&gt;</a>
            <ul class="dropdown-menu" role="menu" aria-labelledby="dLabel">
                <%if (allGuests!=null) {for (Guest guest : allGuests){ %>
                <li><a href="/admin/<%=guest.getId()%>"><%=guest.username%> (<%=guest.getGuestName()%>)</a></li>
                    <%} } else {%>
                <li>No guests</li>
                <% } %>
            </ul>
        </div>

        <% final String subview = (String)request.getAttribute("subview");
           if (subview !=null) {%>
        <jsp:include page="${subview}.jsp" flush="true" />
        <% } %>
    </div>

</div>

</body>
</html>
