<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%><%@ taglib
	prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%><%@ page
	isELIgnored="false"%><%@ page
	import="com.fluxtream.mvc.admin.controllers.*"%><%
	AdminHelper helper = (AdminHelper) request.getAttribute("helper");
%><!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<title>Fluxtream Admin Console</title>
<meta name="description" content="">
<meta name="author" content="">

<!-- Le HTML5 shim, for IE6-8 support of HTML elements -->
<!--[if lt IE 9]>
      <script src="http://html5shim.googlecode.com/svn/trunk/html5.js"></script>
    <![endif]-->

<!-- Le styles -->
<link href="/static/css/bootstrap-2.0.3.min.css" rel="stylesheet">
<link href="/${release}/admin/css/admin.css" rel="stylesheet">
<link rel="stylesheet"
	href="/static/css/bootstrap-responsive-2.0.3.min.css">
<link rel="stylesheet" href="/${release}/css/flx.css">
<link rel="stylesheet" href="/${release}/css/font-awesome.css">

<script src="/static/js/jquery-1.7.1.min.js"></script>
<script src="/static/js/bootstrap-2.0.2.min.js"></script>
<script src="/${release}/admin/js/admin.js"></script>

</head>

<body>

	<div class="navbar navbar-fixed-top">
		<div class="navbar-inner">
			<div class="container-fluid">
				<a class="btn btn-navbar" data-toggle="collapse"
					data-target=".nav-collapse"> <span class="icon-bar"></span> <span
					class="icon-bar"></span> <span class="icon-bar"></span>
				</a> <a class="brand" href="/app"><img
					src="/${release}/images/header-logo-v4.png" /></a>
				<div class="nav-collapse">
					<ul class="nav pull-right">
						<li><div class="btn-group" id="apps-menu"
								data-toggle="buttons-radio"></div></li>
						<li><a href="/admin/general">General</a></li>
						<li><a href="/admin/guests">Guests</a></li>
						<li class="divider-vertical"></li>
						<li class="dropdown"><a href="#" class="dropdown-toggle"
							data-toggle="dropdown"><%=helper.getGuestname()%>
								<i class="icon-user icon-large"></i> <b class="caret"></b></a>
							<ul class="dropdown-menu">
								<li><a href="/app">Go to app <i style="float: right"
										class="icon-home icon-large"></i></a></li>
								<li class="divider"></li>
								<li><a href="/logout">Logout <i style="float: right"
										class="icon-off icon-large"></i></a></li>
							</ul></li>
					</ul>
				</div>
			</div>
		</div>
	</div>


	<div class="container-fluid">

		<div style="min-height: 300px">

			<jsp:include page="${childView}">
				<jsp:param name="activeTab" value="${activeTab}" />
				<jsp:param name="helper" value="${helper}" />
			</jsp:include>
		</div>

		<footer>
			<%
				int currentYear = java.util.Calendar.getInstance().get(
						java.util.Calendar.YEAR);
				String until = "";
				if (currentYear > 2010)
					until = " - " + currentYear;
			%>
			<p>
				&copy; Palacehotel Software sprl, 2010<%=until%></p>
		</footer>
	</div>
	<!-- /container -->

</body>
</html>
