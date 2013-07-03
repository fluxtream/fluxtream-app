<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"
%><%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"
%><%@ page isELIgnored="false"%>
<html>
<head>
	<meta charset="utf-8">
  <%/* Always force latest IE rendering engine (even in intranet) & Chrome Frame
       Remove this if you use the .htaccess 
  <meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">*/%>
   <meta name="viewport" content="width=device-width, initial-scale=1"/>
	<title>Fluxtream | Password forgotten</title>
		<link type="text/css" rel="stylesheet" href="/${release}/css/flux.css">
 
		<!-- Adding "maximum-scale=1" fixes the Mobile Safari auto-zoom bug: http://filamentgroup.com/examples/iosScaleBug/ -->

  <!-- Place favicon.ico & apple-touch-icon.png in the root of your domain and delete these references -->
  <link rel="shortcut icon" href="/favicon.ico">
  <link rel="apple-touch-icon" href="/apple-touch-icon.png">
  
</head>
<body>
<header class="errorPage">
	<nav class="topMenu">
	</nav>
	<figure></figure>
	<div class="clear"></div>
	
</header>
<section class="errorPage">
<h4>Password forgotten? Get a new one.</h4>
<% if (request.getAttribute("error")!=null) { %>
								<p><%=request.getAttribute("error") %></p>
							<% } else { %>
								<p>Please give us your email address.<br> We will send you a message with a link that will let you create a new password.</p>
							<% } %>
							<br>
								<form action="sendResetRequest">
                                    <input autocorrect="off" autocapitalize="off" ="email" type="text" class="placeholder focushere" style="font-size:0.905em; width:300px; margin-right:10px;" name="recover[email]" id="recover_email" title="Your email address"  placeholder="Your email address">
                           			<input type="submit"  class="btn primary" value="Reset my Password"></input><br>
                           			<input type="hidden" name="recover[_csrf_token]" value="ebb981ef2ee1ad730d0e676d2af2336c" id="recover__csrf_token" />
                           			<a href="/" class="btn floatR">Back to home page &raquo;</a>
								</form>



</section>

<div style="padding-left:20px;">
    <% int currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR); String until = ""; if (currentYear>2011) until = " - " + currentYear; %>
    <% String release = ""; if (request.getAttribute("release")!=null) release = "Release " + request.getAttribute("release") + "";%>
    	<div>
    		<hr/>
    		<small class="flx-credits">Fluxtream - <%=release %> &copy; 2011<%=until %> <br/>The BodyTrack Team @ CMU CREATE Lab &amp; Candide Kemmler</small>
    	</div>
</div>
<script src="//ajax.googleapis.com/ajax/libs/jquery/1.6.1/jquery.min.js" type="text/javascript"></script>
<script type="text/javascript" src="/${release}/js/fluxtream.js"></script>


</body>
</html>