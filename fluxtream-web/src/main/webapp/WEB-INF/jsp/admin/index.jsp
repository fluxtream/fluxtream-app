<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"
%><%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"
%><%@ page isELIgnored="false"%><!DOCTYPE html>
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
<link href="/${release}/css/bootstrap/bootstrap.min.css" rel="stylesheet">
<link href="/${release}/admin/css/admin.css" rel="stylesheet">

<script src="/${release}/admin/js/jquery-1.6.4.js"></script>
<script src="/${release}/admin/js/admin.js"></script>

</head>

<body>

	<div class="topbar">
		<div class="fill">
			<div class="container">
				<a class="brand" href="/admin/index">Fluxtream Admin</a>
				<ul class="nav">
					<li><a href="/admin/general">General</a></li>
					<li><a href="/admin/guests">Guests</a></li>
				</ul>
				<form class="pull-right" method="GET" action="../logout">
					<a href="/home">[&larr; App]&nbsp;&nbsp;</a>
					<button class="btn" type="submit">Logout</button>
				</form>
			</div>
		</div>
		<!-- /topbar-inner -->
	</div>
	<div class="container" style="padding-top:50px">
		
		<jsp:include page="${childView}">
			<jsp:param name="activeTab" value="${activeTab}"/>
			<jsp:param name="helper" value="${helper}"/>
		</jsp:include>
		
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
