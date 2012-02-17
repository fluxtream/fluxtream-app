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
		<link type="text/css" rel="stylesheet" href="/${release}/css/flx.css">
 
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
		<iframe id='prefinery_iframe_inline' allowTransparency='true' width='500' height='300' scrolling='no' frameborder='0'
	src='http://fluxtream.prefinery.com/betas/1641/testers/new?display=inline'></iframe>



</section>

<div style="padding-left:20px;">
<%@ include file="../footer.jsp" %>
</div>
<script src="//ajax.googleapis.com/ajax/libs/jquery/1.6.1/jquery.min.js" type="text/javascript"></script>
<script type="text/javascript" src="/${release}/js/fluxtream.js"></script>


</body>
</html>


