<%@ page pageEncoding="utf-8" contentType="text/html; charset=UTF-8"
%><%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"
%><%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags"
%><!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<title>Fluxtream - Personal Analytics</title>
<meta name="viewport" content="width=device-width, initial-scale=1" />
<meta name="description" content="">
<meta name="author" content="">

<link rel="stylesheet" href="/static/css/bootstrap-2.0.2.min.css">
<link rel="stylesheet" href="/static/css/bootstrap-responsive-2.0.2.min.css">
<link rel="stylesheet" href="/${release}/css/flx.css">
<link rel="stylesheet" href="/${release}/css/bodytrack.css">
<link rel="stylesheet" href="/${release}/css/font-awesome.css">
<link rel="stylesheet" href="/${release}/css/qtip/jquery.qtip.min.css">
<link rel="stylesheet" href="http://ajax.googleapis.com/ajax/libs/jqueryui/1.8/themes/base/jquery-ui.css">

<link rel="stylesheet" href="/static/css/jquery-colorPicker/jquery.colorPicker.css">
<link rel="stylesheet" href="/static/css/msdropdown/dd.css">
<link rel="stylesheet" href="/static/css/tagedit/css/jquery.tagedit.css">

<script
	src="https://maps-api-ssl.google.com/maps/api/js?libraries=geometry&v=3&sensor=false"
	type="text/javascript"></script>

<link rel="shortcut icon" href="/favicon.ico">

</head>

<body>

	<div class="navbar navbar-fixed-top">
		<div class="navbar-inner">
			<div class="container-fluid">
				<a class="btn btn-navbar" data-toggle="collapse"
					data-target=".nav-collapse"> <span class="icon-bar"></span> <span
					class="icon-bar"></span> <span class="icon-bar"></span>
				</a> <a class="brand" href="/app"><img src="/${release}/images/header-logo-v4.png"/></a>
				<div class="nav-collapse">
					<form class="navbar-search">
						<input type="text" class="search-query" placeholder="Search">
					</form>
					<ul class="nav pull-right">
						<li><div class="btn-group" id="apps-menu"
								data-toggle="buttons-radio"></div></li>
						<li class="divider-vertical"></li>
						<li class="dropdown"><a href="#" class="dropdown-toggle"
							data-toggle="dropdown"><%=request.getAttribute("fullname")%> <i class="icon-user icon-large"></i>
								<b class="caret"></b></a>
							<ul class="dropdown-menu">
								<li><a href="javascript:App.settings()">Settings <i class="icon-cog icon-large" style="float:right;"></i></a></li>
								<li><a href="javascript:App.connectors()">Connectors</a></li>
								<sec:authorize access="hasRole('ROLE_ADMIN')"><li><a href="/admin/index">Admin <i class="icon-key icon-large" style="float:right;"></i></li></sec:authorize>
								<li class="divider"></li>
								<li><a href="/logout">Logout <i style="float:right" class="icon-off icon-large"></i></a></li>
							</ul></li>
					</ul>
				</div>
			</div>
		</div>
	</div>

	<div class="container-fluid application">

		<!-- here is where fluxtream apps go -->

	</div>

	<c:catch var="e">
		<jsp:include page="footer_custom.jsp" />
	</c:catch>
	<c:if test="${!empty e}">
		<jsp:include page="footer.jsp" />
	</c:if>

	<div id="modal"></div>

	<script>window.FLX_RELEASE_NUMBER="${release}";</script>

	<script
		src="//ajax.googleapis.com/ajax/libs/jquery/1.7.1/jquery.min.js"></script>
	<script>window.jQuery || document.write('<script src="/static/js/jquery-1.7.1.min.js"><\/script>')</script>
	<script src="/static/tiny_mce/jquery.tinymce.js"></script>
	<script src="/static/js/json2.js"></script>
	<script src="/static/js/jquery.mustache-1.0.js"></script>

	<!--  TODO: validate version numbers for these libs -->
	<script src="http://ajax.googleapis.com/ajax/libs/jqueryui/1.8.16/jquery-ui.min.js"></script>
	<script src="/static/js/jquery.autoGrowInput.js"></script>
	<script src="/static/js/jquery.colorPicker.js"></script>
	<script src="/static/js/jquery.dd-uncompressed.js"></script>
	<script src="/static/js/jquery.shorten.js"></script>
	<script src="/static/js/jquery.tagedit.js"></script>
	<script src="/static/grapher4/grapher2.nocache.js"></script>

	<script src="/static/js/bootstrap-2.0.2.min.js"></script>
	<script src="/static/js/raphael-2.0.2.js"></script>
	<script src="/${release}/js/libs/underscore-1.3.1-min.js"></script>
	<script src="/${release}/js/libs/backbone-0.9.1-min.js"></script>
	<script data-main="/${release}/js/main.js" src="/static/js/require.js"></script>

</body>
</html>
