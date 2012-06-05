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
	<link rel="stylesheet" href="/css/qtip/jquery.qtip.min.css">
	<link rel="stylesheet" href="/css/qtip/jquery.jqplot.min.css">
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

<body style="padding: 50px">

	<div id="content">
		<div class="container-fluid">
			<div class="row"> 
				<div class="span12">
			<!-- Remove when added to the clock tab -->
					<div class="flx-clockToolTipContainer">
						<div class="flx-toolTipBody strongBorder-Right"> <!-- JS : Append strongBorder class with -Top, -Left, -Right, Bottom according to toolTip position / Append the right color class to override default -->
							<span class="flx-toolTipTail-Right" style="top:50px"></span> <!-- JS : Append class with -Top, -Left or -Right to change tail direction + define top/bottom/right/left arguments in the style attribute to give the tail it's position -->
							<div class="flx-toolTipData">
								<h3 class="flx-dataType">Weight</h3>
									<ul class="flx-toolTipLinks">
										<li><a href="#">List <i class="icon-list"></i></a></li>
										<li><a href="#">Timeline <i class="icon-film"></i></a></li>
										<li><a href="#">Bodytrack <i class="icon-lemon"></i></a></li>
									</ul>
									<span class="flx-deviceIcon withings"></span>
								<div class="flx-deviceData">
									<span class="flx-tTime">9:43</span>
									<span class="flx-data">71.05 kg</span>
								</div>
								<span class="flx-miniGraph"><img src="/css/dashboardImgDummies/w8.png" alt="w8" width="" height="" /></span>
							</div>
							
							<div class="flx-toolTipContext">
								<div class="flx-toolTipLocation">
									<h4><span>9:43</span> last seen here: <strong>RVB Studio, Brussels</strong></h4>
									<div>
										<img src="/css/dashboardImgDummies/w7.png" alt="w7" width="" height="" />
									</div>
								</div>
								<div class="flx-toolTipWeather">
									<h4><span>15.C</span> Variable with showers</h4>
									<div class="weatherIcon">
										<img src="/css/dashboardImgDummies/sun%26rain.png" alt="sun-and-rain" width="" height="" />
									</div>
									<ul class="flx-weatherDetails">
										<li>Windspeed <span>7km/h</span></li>
										<li>Humidity <span>54%</span></li>
										<li>Precipitation <span>2mm</span></li>
									</ul>
								</div>
							</div>
						</div>
					</div>
				</div>
			</div>
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
		<script src="/static/js/hogan-2.0.0.js"></script>
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
	</g:compress>
</body>
</html>
