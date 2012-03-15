<!DOCTYPE html>
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
								<li class="divider"></li>
								<li><a href="/logout">Logout <i style="float:right" class="icon-signout icon-large"></i></a></li>
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
	<script>window.jQuery || document.write('<script src="/static/js/jquery-1.7.1.min.js"><\/script>')</script>
	<script>
		window.jQuery
				|| document
						.write('_$tag_________________________________________$tag_____')
	</script>
	<script src="/static/tiny_mce/jquery.tinymce.js"></script>
	<script src="/static/js/json2.js"></script>
	<script src="/static/js/bootstrap-2.0.2.min.js"></script>
	<script src="/static/js/raphael-2.0.2.js"></script>
	<script src="/${release}/js/libs/underscore-1.3.1-min.js"></script>
	<script src="/${release}/js/libs/backbone-0.9.1-min.js"></script>
	<script data-main="/${release}/js/main.js" src="/static/js/require.js"></script>

</body>
</html>
