<%--
  Created by IntelliJ IDEA.
  User: candide
  Date: 11/04/14
  Time: 14:14
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<html ng-app="developerWebsiteApp">
<head>
    <meta charset="utf-8">
    <title>Fluxtream Developer Home</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link href="//netdna.bootstrapcdn.com/font-awesome/4.0.1/css/font-awesome.css" rel="stylesheet">
    <link href='https://fonts.googleapis.com/css?family=Droid+Sans:400,700' rel='stylesheet' type='text/css'/>

    <!-- developer website css (compiled from bootstrap less source) -->
    <link href="${release}/css/styles.css" rel="stylesheet">

    <!-- developer website js and dependencies-->
    <script src="static/js/angular-1.2.16.min.js"></script>
    <script src="static/js/angular-route-1.2.16.min.js"></script>
    <script src="static/js/angular-resource-1.2.16.min.js"></script>
    <script src="${release}/js/app.js"></script>
    <script src="${release}/js/controllers.js"></script>
    <script src="${release}/js/services.js"></script>

</head>
<body>

<div id="wrapper">

    <!-- Sidebar -->
    <div id="sidebar-wrapper">
        <ul class="sidebar-nav">
            <li class="sidebar-brand"><a href="#">&nbsp;</a>
            </li>
            <li class="active"><a href="#/apps">Applications</a>
            </li>
            <li><a href="#/api-docs">Api Docs</a>
            </li>
            <li><a href="logout">Sign Out</a>
            </li>
        </ul>
    </div>

    <!-- Page content -->
    <div id="page-content-wrapper" ng-view class="container">
    </div>

</div>


<jsp:include page="footer.jsp"/>
