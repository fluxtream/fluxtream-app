<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"
%><%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"
%><%@ page isELIgnored="false"
%><%@ page import="org.fluxtream.*" %>
<html><head>
  	<link type="text/css" rel="stylesheet" href="/${release}/css/flx.css">
	<script>
	
	function toggleMessageDetails() {
		var errorMessage = document.getElementById("errorMessage");
		var messageToggle = document.getElementById("messageToggle");
		if (errorMessage.style.display=="none") {
			messageToggle.innerHTML = "Hide error details";
			errorMessage.style.display="block";
		} else {
			messageToggle.innerHTML = "Show error details";
			errorMessage.style.display="none";
		}
	}
	
	</script>
</head>
<body>
<header class="errorPage">
	<nav class="topMenu">
	</nav>
	<figure></figure>
	<div class="clear"></div>
	
</header>
<section class="errorPage">
<h4>${errorMessage}</h4>

<a href="/" class="btn primary">Back to home page &raquo;</a>
<% if (request.getAttribute("stackTrace")!=null) {%>

	<a id="messageToggle" href="javascript:toggleMessageDetails()">Show error details:</a>
	
	<div id="errorMessage" style="display:none">
		<p>${stackTrace}</p>
	</div>
<% } %>
</section>

<div style="padding-left:20px;">
<%@ include file="footer.jsp" %>
</div>
</body></html>

