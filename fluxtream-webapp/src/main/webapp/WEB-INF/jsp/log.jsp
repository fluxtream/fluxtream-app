<%@ page pageEncoding="utf-8" contentType="text/html; charset=UTF-8"
%><%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"
%><%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"
%><%@ page isELIgnored="false"
%><%@ page import="com.fluxtream.*"
%><%@ page import="com.fluxtream.mvc.models.HomeModel"
%><%@ page import="org.joda.time.format.*"
%><%@ page import="org.joda.time.*"
%><%@ page import="java.util.TimeZone"
%><%@ page import="java.util.Date"
%><%@ page import="java.util.Calendar"
%><%
  Boolean prod = (Boolean)request.getAttribute("prod");
  String guestName = (String)request.getAttribute("guestName");
  String testerHash = (String)request.getAttribute("testerHash");
  HomeModel homeModel = (HomeModel) request.getSession().getAttribute("homeModel");
  DateTimeFormatter format = DateTimeFormat.forPattern("yyyy-MM-dd");
  Calendar calendar = Calendar.getInstance(homeModel.getTimeZone());
  String today = format.withZone(DateTimeZone.forTimeZone(homeModel.getTimeZone())).print(System.currentTimeMillis());%><!doctype html>
<!-- paulirish.com/2008/conditional-stylesheets-vs-css-hacks-answer-neither/ -->
<!--[if lt IE 7]> <html class="no-js ie6" lang="en"> <![endif]-->
<!--[if IE 7]>    <html class="no-js ie7" lang="en"> <![endif]-->
<!--[if IE 8]>    <html class="no-js ie8" lang="en"> <![endif]-->
<!--[if gt IE 8]><!--> <html class="" lang="en"> <!--<![endif]-->
<head>

	<meta charset="utf-8">
	<!-- Always force latest IE rendering engine (even in intranet) & Chrome Frame
	     Remove this if you use the .htaccess -->
	<meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">
	
	<title>Fluxtream | Personal Analytics</title>
	<!--[if lt IE 9]>
		<script src="http://html5shim.googlecode.com/svn/trunk/html5.js"></script>
	<![endif]-->
	<meta name="description" content="">
	<meta name="author" content="">
	<meta name="viewport" content="width=device-width, initial-scale=1"/>
	<!-- Adding "maximum-scale=1" fixes the Mobile Safari auto-zoom bug: http://filamentgroup.com/examples/iosScaleBug/ -->
	<!-- Place favicon.ico & apple-touch-icon.png in the root of your domain and delete these references -->
	<link rel="shortcut icon" href="/favicon.ico">
	<link rel="apple-touch-icon" href="/apple-touch-icon.png">
	<link type="text/css" rel="stylesheet" href="/${release}/css/jquery.wysiwyg.css" />
	<link type="text/css" rel="stylesheet" href="/${release}/css/qtip/jquery.qtip.css" />
	<link type="text/css" rel="stylesheet" href="/${release}/css/qtip/myqtip.css" />
	<link type="text/css" rel="stylesheet" href="/${release}/css/datepicker.css" />
	<link type="text/css" rel="stylesheet" href="/${release}/css/flx.css">
	<!-- Uncomment if you are specifically targeting less enabled mobile browsers
	<link rel="stylesheet" media="handheld" href="css/handheld.css">  -->
	<!-- All JavaScript at the bottom, except for Modernizr which enables HTML5 elements & feature detects -->
	<script src="/static/scripts/modernizr-1.7.min.js"></script>
	<script src="//maps-api-ssl.google.com/maps/api/js?libraries=geometry&v=3&sensor=false"></script>

</head>
<body>

<header>
	<nav class="topMenu">
		<div style="float: left; margin: 6px 35px 0 0; opacity: 1; position: relative;">
				<span class="searchBoxBt"><i></i></span>
				<input onkeypress="if(event.which==13) search()" id="searchBox" type="text" class="placeholder" title="Search..."  value="Search...">	
		</div>
		<ul style="topMenuIconBt">
			<li><div class="iconHello"></div> <%= guestName %></li>
			<li class="connectors_menu" data-dropdown="dropdown"><a href="#" class="dropdown-toggle"><div class="iconPref"></div></a>
				<div class="dropdown-menu">
  					<ul>
						<li><a href="javascript:connectors()">Connectors</a></li>
  						<li><a href="javascript:settings()">Settings</a></li>
  						
  						<li><a href="/logout">Logout</a></li>
  					</ul>
				</div>
			</li>	
		</ul>	
	</nav>
	<figure></figure>
	<div id="navCont">
		<nav class="menuView">
			<a class="btn btnLeft"><i></i>Donuts</a><a class="btn btnRight"><i></i>List</a>
		</nav>
		
		<nav class="MenuTime">
			<div class="navMenuIconToday"><div><%=calendar.get(Calendar.DATE)%></div></div>
			<div class="v-button v-button-menuTodayButton menuTodayButton" tabindex="0">
					<span class="v-button-wrap">
						<span class="v-button-caption">Today</span>
					</span>
				</div>
			
			<div class="navMenuIconHistory">&nbsp;</div>

			<div class="v-button menuPrevButton" tabindex="0">
				<span class="v-button-wrap">
					<span class="v-button-caption">prev</span>
				</span>
			</div>
			
			<div class="navMenuDate">
				<div  class="v-label" id="currentTimespanLabel"></div>
			</div>
			
			<div class="v-button menuNextButton" tabindex="0">
				<span class="v-button-wrap">
					<span class="v-button-caption">next</span>
				</span>
			</div>			
		</nav>
		
		<div class="clear"></div>
	</div>
	<div class="contMainComment">
		<div class="pointerComm belowDate"></div>
		<span class="mainCommentIcon"><i></i></span>
		<div class="mainCommentContainer">
			<div class="mainComment collapsed" title="This day..." id="commentMain" >
				<div id="commentEditorPanel"></div>
				<div id="commentEditorContent"></div>
			</div>
 		<span class="iconExpandCollapse collapsed"><a><i>&nbsp;</i></a></span>
		</div>
	</div>
	<% if (request.getAttribute("demo")!=null) {%>
	<div class="alert-message fade in" id="demoNotification">
	  <a onclick="$('#demoNotification').hide()" class="close">x</a>
	  <p>You are logged in as user 'demo' - you can view/see almost everything 
(private messages are hidden), writing comments is disabled and you can't change 
settings or add/remove connectors.</p>
	</div>
	<% }%>
	<div class="alert-message fade in" style="display:none">
	  <a onclick="discardNotifications()" class="close">x</a>
	  <p id="notificationIds" style="display:none"></p>
	</div>
	
</header>



<div id="mainCont"></div>
<%@ include file="footer.jsp" %>

<div id="loading" style="display:none;min-height:300px"><img style="border:none" src="/${release}/images/loading.gif"/></div>

<a href='#' id='prefinery_feedback_link'>Give feedback!</a>

<script src="/${release}/scripts/libs/jquery-1.7.1.js"></script>
<script src="/${release}/scripts/libs/underscore-1.2.4.js"></script>
<script src="/${release}/scripts/libs/backbone-0.5.3.js"></script>
<script data-main="/${release}/scripts/main.js" src="/static/scripts/require.js"></script>

<script type="text/javascript">
  var prefineryJsHost = ("https:" == document.location.protocol) ? 'https://www.prefinery.com/' : 'http://www.prefinery.com/';
  document.write(unescape("%3Cscript src='" + prefineryJsHost + "javascripts/widget.js' type='text/javascript'%3E%3C/script%3E"))
</script>
<script type='text/javascript' charset='utf-8'>
 var prefinery_feedback_options = {
   // Your company subdomain
   account: "fluxtream",

   // ID of your beta
   beta_id: "1641",

   // Show the Feedback tab on the screen (true/false)
   show_tab: true,

   // If you don't want to install the tab, you can use your own link to open the widget.
   link_id: "prefinery_feedback_link",

   // The tester_hash is simply a SHA1 hex digest of your secret invitation decoder key 
   // followed by the logged in user's e-mail address.
   tester_hash: "<%=testerHash%>"
 };
 Prefinery.feedback(prefinery_feedback_options);
</script>
</body>
</html>
