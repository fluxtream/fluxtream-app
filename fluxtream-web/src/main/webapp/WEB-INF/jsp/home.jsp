<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<title>Fluxtream - Personal Analytics</title>
<meta name="viewport" content="width=device-width, initial-scale=1" />
<meta name="description" content="">
<meta name="author" content="">

<link rel="stylesheet/less" href="/${release}/css/flx.less">
<link rel="stylesheet/less" href="/${release}/css/qtip/jquery.qtip.css">
<link rel="stylesheet/less" href="/static/less/bootstrap.less">
<link rel="stylesheet/less" href="/static/less/responsive.less">
<script src="/static/js/less-1.2.1.min.js"></script>
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
				</a> <a class="brand" href="#">Fluxtream</a>
				<div class="nav-collapse">
					<form class="navbar-search">
						<input type="text" class="search-query" placeholder="Search">
					</form>
					<ul class="nav pull-right">
						<li><div class="btn-group" id="apps-menu"
								data-toggle="buttons-radio"></div></li>
						<li class="divider-vertical"></li>
						<li class="dropdown"><a href="#" class="dropdown-toggle"
							data-toggle="dropdown"><%=request.getAttribute("fullname")%>
								<b class="caret"></b></a>
							<ul class="dropdown-menu">
								<li><a href="javascript:App.settings()">Settings</a></li>
								<li><a href="javascript:App.connectors()">Connectors</a></li>
								<li class="divider"></li>
								<li><a href="/logout">Logout</a></li>
							</ul></li>
					</ul>
				</div>
			</div>
		</div>
	</div>

	<div class="container-fluid application">

		<!-- here is where fluxtream apps go -->

	</div>

	<%@ include file="footer.jsp"%>

	<div id="modal"></div>

	<script
		src="//ajax.googleapis.com/ajax/libs/jquery/1.7.1/jquery.min.js"></script>
	<script>
		window.jQuery
				|| document
						.write('_$tag_________________________________________$tag_____')
	</script>
	<script src="/static/js/bootstrap-button-2.0.1.js"></script>
	<script src="/static/js/bootstrap-dropdown-2.0.1.js"></script>
	<script src="/static/js/bootstrap-modal-2.0.1.js"></script>
	<script src="/static/js/bootstrap-transition-2.0.1.js"></script>
	<script src="/static/js/bootstrap-tab-2.0.1.js"></script>
	<script src="/static/js/raphael-2.0.2.js"></script>
	<script src="/${release}/js/libs/underscore-1.2.4.js"></script>
	<script src="/${release}/js/libs/backbone-0.5.3.js"></script>
	<script data-main="/${release}/js/main.js" src="/static/js/require.js"></script>

</body>
</html>
