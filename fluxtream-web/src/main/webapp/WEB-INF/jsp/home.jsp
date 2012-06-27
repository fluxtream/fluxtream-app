<%@ page import="com.fluxtream.mvc.controllers.ControllerHelper" %>
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
    <script src="/static/js/hogan-2.0.0.js"></script>

<link rel="shortcut icon" href="/favicon.ico">

</head>

<body>
    <div id="flxUsername" style="display:none;"><%=ControllerHelper.getGuest().username%></div>

	<div id="content">

		<div class="navbar">
			<div class="navbar-inner">
				<div class="container-fluid">
					<a class="btn btn-navbar" data-toggle="collapse"
						data-target=".nav-collapse"> <span class="icon-bar"></span> <span
						class="icon-bar"></span> <span class="icon-bar"></span>
					</a>
                    <a class="brand" href="/app"><img
                            src="/${release}/images/header-logo-v4.png" width=94 height=20/></a>
					<div class="nav-collapse">
						<%--<form class="navbar-search" action="javascript:App.search()">--%>
							<%--<input onkeypress="if(event.which==13) App.search()" autocorrect="off" autocapitalize="off" type="text"--%>
								<%--class="search-query" placeholder="Search">--%>
						<%--</form>--%>

                            <ul class="nav">
                            <li><div class="btn-group" id="apps-menu"
                                     data-toggle="buttons-radio"></div></li>
                            </ul>
                            <ul class="nav pull-right">
                                <li class="divider-vertical"></li>
                                <li class="dropdown"><a href="#" class="dropdown-toggle"
                                                        data-toggle="dropdown">Connectors
                                    <i class="icon-random icon-large"></i> <b class="caret"></b></a>
                                    <ul class="dropdown-menu">
                                        <li><a href="javascript:App.connectors()">Add  <i
                                                class="icon-plus icon-large" style="float: right;"></i></a></li>
                                        <li><a href="javascript:App.manageConnectors()">Manage <i
                                                class="icon-list icon-large" style="float: right;"></i></a></li>
                                    </ul></li>
                                <li class="divider-vertical"></li>
							<li class="dropdown"><a href="#" class="dropdown-toggle"
								data-toggle="dropdown"><%=request.getAttribute("fullname")%>
									<i class="icon-user icon-large"></i> <b class="caret"></b></a>
								<ul class="dropdown-menu">
									<li><a href="javascript:App.settings()">Settings <i
											class="icon-cog icon-large" style="float: right;"></i></a></li>
                                    <li><a href="javascript:App.addresses()">Addresses <i style="float: right;" class="icon-home icon-large"></i></a></li>
									<sec:authorize access="hasRole('ROLE_ADMIN')">
										<li><a href="/admin/index">Admin <i
												class="icon-key icon-large" style="float: right;"></i></a></li>
									</sec:authorize>
									<li class="divider"></li>
									<li><a href="/logout">Logout <i style="float: right"
											class="icon-off icon-large"></i></a></li>
								</ul></li>
                            </ul>
					</div>
				</div>
			</div>
		</div>

		<div id="applications" class="container-fluid">

			<div id="notifications" class="alert alert-success fade in"
				style="display: none">
				<a onclick="App.discardNotifications()" class="close">&times;</a>
				<p id="notificationIds" style="display: none"></p>
			</div>

			<!-- here is where fluxtream apps go -->

		</div>

		<div id="modal"></div>

		<div id="loading" style="display: none; min-height: 300px">
			<img style="border: none" src="/${release}/images/loading.gif" />
		</div>

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
		<script src="/static/js/jquery.ui.sortable-bt.js"></script>
		<script src="/static/tiny_mce/jquery.tinymce.js"></script>
		<script src="/static/js/json2.js"></script>
		<script src="/static/js/jquery.autoGrowInput.js"></script>
		<script src="/static/js/jquery.colorPicker.js"></script>
		<script src="/static/js/jquery.dd-uncompressed.js"></script>
		<script src="/static/js/jquery.tagedit.js"></script>
		<script src="/static/js/jquery.shorten.js"></script>
		<script src="/static/js/jquery.jqplot.min.js"></script>
		<script src="/static/js/jqplot-plugins/jqplot.categoryAxisRenderer.min.js"></script>
		<script src="/static/js/jqplot-plugins/jqplot.pointLabels.min.js"></script>
		<script src="/static/js/jqplot-plugins/jqplot.barRenderer.min.js"></script>
		<script src="/static/js/bootstrap-2.0.2.min.js"></script>
		<script src="/static/js/raphael-2.1.0.js"></script>
		<script src="/static/js/underscore-1.3.3-min.js"></script>
		<script src="/static/js/backbone-0.9.2-min.js"></script>
        <script src="/static/js/jquery.ba-dotimeout.min.js"></script>
        <script src="/static/js/jquery.masonry.min.js"></script>
	</g:compress>
	
	<!--  TODO: validate version numbers for these libs -->
	<script src="/static/grapher4/grapher2.nocache.js"></script>
	
	<script data-main="/${release}/js/main.js" src="/static/js/require.js"></script>
</body>
</html>
