<%@ page import="org.fluxtream.core.auth.AuthHelper" %>
<%@ page import="java.util.List"
%><%@ page import="org.fluxtream.core.domain.Guest"
%><!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">
<meta name="apple-mobile-web-app-capable" content="yes" />
<meta name="apple-mobile-web-app-status-bar-style"
        content="black-translucent" />
<meta name="viewport" content="width=1000, initial-scale=1.0">
<title>Fluxtream Server Configuration Error</title>

<link rel="stylesheet" href="/static/css/bootstrap-2.0.3.min.css">
<link rel="stylesheet"
      href="/static/css/bootstrap-responsive-2.0.3.min.css">
<link rel="stylesheet" href="/static/css/font-awesome-3.2.1.css">

<link rel="shortcut icon" href="/favicon.ico">

</head>

<body>
        <div id="content">
                <div id="applications" class="container-fluid">
                    <b>Sorry, a server configuration has occurred</b>

                    <% String userMessage = (String) request.getAttribute("userMessage"); %>
                    <% if(userMessage!=null) {%>
                    :<br>
                    &nbsp;&nbsp;&nbsp;${userMessage}<br>
                    <% } else { %>
                    <br>
                    <% } %>
                    <br>

                    Please contact the administrator for this server
                    <% String adminMessage = (String) request.getAttribute("adminMessage"); %>
                    <%if(adminMessage!=null) {%>
                     with the message:<br> <i>
                    &nbsp;&nbsp;&nbsp;${adminMessage}<br></i>
                    <% } %>
                    <br>
                    <a href="/" class="btn floatR">Back to home page &raquo;</a>


                </div>

    </div>

    <script>
                window.FLX_RELEASE_NUMBER = "${release}";
        </script>

        <script
                src="//ajax.googleapis.com/ajax/libs/jquery/1.10.2/jquery.min.js"></script>
        <script>
                window.jQuery
                                || document
                                                .write('<script src="/static/js/jquery-1.10.2.min.js"><\/script>')
        </script>
    <script src="/static/js/bootstrap-2.0.2.min.js"></script>
</body>
</html>