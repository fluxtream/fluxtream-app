<%@ page import="com.fluxtream.mvc.controllers.AuthHelper" %>
<%@ page pageEncoding="utf-8" contentType="text/html; charset=UTF-8"%><%@ taglib
    prefix="c" uri="http://java.sun.com/jsp/jstl/core"%><%@ taglib
    prefix="sec" uri="http://www.springframework.org/security/tags"%><%@ taglib
    uri="http://granule.com/tags" prefix="g"%><!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">
<meta name="apple-mobile-web-app-capable" content="yes" />
<meta name="apple-mobile-web-app-status-bar-style"
    content="black-translucent" />
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Fluxtream - Personal Analytics</title>
<meta name="description" content="">
<meta name="author" content="">

<g:compress>
    <link rel="stylesheet" href="/static/css/bootstrap-2.0.3.min.css">
    <link rel="stylesheet"
        href="/static/css/bootstrap-responsive-2.0.3.min.css">
    <link rel="stylesheet" href="/css/flx.css">
    <link rel="stylesheet" href="/css/bodytrack.css">
    <link rel="stylesheet" href="/css/datepicker.css">
    <link rel="stylesheet" href="/static/css/jquery-ui/jquery-ui-1.8.18.custom.css">
    <link rel="stylesheet"
        href="/static/css/jquery-colorPicker/jquery.colorPicker.css">
    <link rel="stylesheet" href="/static/css/msdropdown/dd.css">
    <link rel="stylesheet" href="/static/css/tagedit/css/jquery.tagedit.css">
</g:compress>

<link rel="stylesheet" href="/static/css/font-awesome.css">

<script
    src="https://maps-api-ssl.google.com/maps/api/js?libraries=geometry&v=3&sensor=false"
    type="text/javascript"></script>

<link rel="shortcut icon" href="/favicon.ico">

</head>

<body style="background:#fff;">

    <div id="content">
        
    API Explorer
    
    </div>


    <jsp:include page="footer.jsp" />

    <script>
        window.FLX_RELEASE_NUMBER = "${release}";
    </script>

    <script
        src="//ajax.googleapis.com/ajax/libs/jquery/1.7.1/jquery.min.js"></script>
    <script>
        window.jQuery
                || document
                        .write('<script src="/static/js/jquery-1.7.1.min.js"><\/script>')
    </script>
    <g:compress>
        <script src="/static/js/jquery-ui-1.8.18.custom.min.js"></script>
        <script src="/static/js/jquery.ui.sortable-1.8.2-bt-1.0.0.js"></script>
        <script src="/static/tiny_mce/jquery.tinymce.js"></script>
        <script src="/static/js/json2-2011.10.19.js"></script>
        <script src="/static/js/hogan-2.0.0.js"></script>
        <script src="/static/js/jquery.autoGrowInput-1.0.0.js"></script>
        <script src="/static/js/jquery.colorPicker-2012.01.27.js"></script>
        <script src="/static/js/jquery.dd-2.37.5-uncompressed.js"></script>
        <script src="/static/js/jquery.tagedit-1.2.1.js"></script>
        <script src="/static/js/jquery.shorten-1.0.0.js"></script>
        <script src="/static/js/jquery.jqplot-1.0.0b2.min.js"></script>
        <script src="/static/js/jqplot-plugins/jqplot.categoryAxisRenderer.min.js"></script>
        <script src="/static/js/jqplot-plugins/jqplot.pointLabels.min.js"></script>
        <script src="/static/js/jqplot-plugins/jqplot.barRenderer.min.js"></script>
        <script src="/static/js/bootstrap-2.0.2.min.js"></script>
        <script src="/static/js/raphael-2.1.0.js"></script>
        <script src="/static/js/underscore-1.3.3-min.js"></script>
        <script src="/static/js/backbone-0.9.2-min.js"></script>
    </g:compress>
</body>
</html>
