<%@ page contentType="text/html; charset=UTF-8" import="org.springframework.security.web.WebAttributes"
%><%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"
%><%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"
%><%@ page isELIgnored="false"
%><%@ page import="com.fluxtream.*" 
%><%@ page import="java.util.*" 
%><%@ page import="org.springframework.web.context.request.RequestContextListener"
%><%
  	Boolean prod = (Boolean)request.getAttribute("prod");
%><!doctype html><!--[if lt IE 7]> <html class="no-js ie6" lang="en"> <![endif]-->
<!--[if IE 7]>    <html class="no-js ie7" lang="en"> <![endif]-->
<!--[if IE 8]>    <html class="no-js ie8" lang="en"> <![endif]-->
<!--[if gt IE 8]><!--> <html class="" lang="en"> <!--<![endif]-->
<head>
  <meta charset="utf-8">
  <meta http-equiv="X-UA-Compatible" content="chrome=1">

  <title>Fluxtream | Personal Analytics | Welcome</title>
  <!-- allisok -->
  <meta name="description" content="">
  <meta name="author" content="">

  <meta name="viewport" content="width=device-width, initial-scale=1"/>
		<!-- Adding "maximum-scale=1" fixes the Mobile Safari auto-zoom bug: http://filamentgroup.com/examples/iosScaleBug/ -->

  <!-- Place favicon.ico & apple-touch-icon.png in the root of your domain and delete these references -->
  <link rel="shortcut icon" href="/favicon.ico">
  <link rel="apple-touch-icon" href="/apple-touch-icon.png">
    
  <!-- CSS: implied media="all" -->
  <% if (false) { %>
  <link rel="stylesheet" href="/${release}/css/flx-min.css">
  <% } else { %>
  <link rel="stylesheet" href="/${release}/css/flx.css">
  <% } %>
  <!-- Uncomment if you are specifically targeting less enabled mobile browsers
  <link rel="stylesheet" media="handheld" href="css/handheld.css">  -->

  <!-- All JavaScript at the bottom, except for Modernizr which enables HTML5 elements & feature detects -->
  <script src="/${release}/js/15-flx-browserDetect.js"></script>
  <script>
	if (BrowserDetect.browser=="Explorer")
		window.location="/notSupported";
  </script>
  <script src="/${release}/js/libs/modernizr-1.7.min.js"></script>
  
</head>

<body style="background-color:#fff;">

<header>
	<img src="/${release}/css/header-logo-splash.png" WIDTH=859 HEIGHT=78 alt="Fluxtream - Personal Analytics"></header>

<div id="login">
	<section class="screenshot">
		
	<article class="floatR">
		<article class="register">
			<h3>Sign in</h3>
			<form action="signIn" method="POST">
				
				<label for="user_email">Username</label><br>
				<input autocomplete="on" onkeypress="if(event.which==13) document.forms[0].submit();"
					class="title" id="f_username" name="f_username"
					value="<%=request.getParameter("username")!=null?request.getParameter("username"):"" %>"
				type="text"><br>

				<label for="user_password">Password</label><br>
				<input value="" onkeypress="if(event.which==13) document.forms[0].submit();"
					class="title" id="f_password" name="f_password"
					type="password" style="margin-bottom:20px;">
					
				<input type="submit" id="signinButton"  class="btn primary large" style="width:100px;" value="&raquo; Sign In"/> &nbsp;
				<a href="support/lostPassword">&raquo; Password forgotten?</a>
			</form>
		</article>
		<article class="register" class="clear:both;">
			<h3>New Users, get in!</h3>
			<p style="margin-bottom:10px;">It's free and only takes a few seconds</p>
			<a id="createAccountButton" class="btn primary large">&raquo; Create your account</a>
		</article>
	</article>
		<article>
			<h3>Own your data<br/>And make sense of it</h3>
			<p>Track your daily habits, identify your<br>
			strengths and weaknesses<br>
			and become a better person</p>
			<p>Get a comprehensive view on your self-tracking devices and the services that you use to organize your life</p>
			<p><a href="http://blog.fluxtream.com" class="btn large secondary">Check our blog for updates &raquo;</a></p>
		</article>
		 
	</section>
	
</div>

<div id="createAccount">
	<h3>Sign up</h3>
	<p>We are currently running private invitation-only alpha tests:</p>
	<a target="_blank" href="get-invitation" class="btn">Request an invitation</a> &nbsp;
	<a href="javascript:createAccount()" class="btn primary">I have an invitation code, let me in</a>
	<p>&nbsp;</p>
</div>

<footer style="text-align:center;">
<div class="partners">
	<a class="quantifiedSelf" href="http://www.quantifiedself.com/conference/" target="_blank">Quantified Self</a>
	<a class="foundersInst" href="http://www.founderinstitute.com/" target="_blank">Founders Institute</a>
	<a class="microsoftInno" href="http://www.microsoft.com/mic/" target="_blank">Microsoft Innovation Center</a>
	<a class="standingCloud" href="http://www.standingcloud.com/" target="_blank">Standing Cloud</a>
</div>
<% int currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR); String until = ""; if (currentYear>2011) until = " - " + currentYear; %>
<p><small>&copy; Candide Kemmler, 2011<%=until %></small></p>
</footer>

<!-- JavaScript at the bottom for fast page loading -->

<script src="//ajax.googleapis.com/ajax/libs/jquery/1.7.1/jquery.min.js" type="text/javascript"></script>
<!-- <script src="https://www.google.com/recaptcha/api/js/recaptcha_ajax.js"></script> -->
<script src="/${release}/js/001-facebox.js"></script>
<script src="/${release}/js/welcome.js"></script>
<script>

  <% if (request.getParameter("username")!=null) { %>
alert("Bad username or password.\nPlease try again");
  <% } %>

</script>

  <% if (prod) { %>
<%@ include file="trackers/ga.jsp"%>
  <% } %>


</body>
</html>