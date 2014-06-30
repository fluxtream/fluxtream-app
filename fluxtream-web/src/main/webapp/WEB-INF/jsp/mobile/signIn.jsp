<%--
  Created by IntelliJ IDEA.
  User: candide
  Date: 27/05/14
  Time: 13:10
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1" />
    <meta name="apple-mobile-web-app-capable" content="yes" />
    <meta name="viewport" content="user-scalable=no, initial-scale=1.0, maximum-scale=1.0" />
    <meta name="apple-mobile-web-app-status-bar-style" content="yes" />
    <title>Sign In</title>
    <link href="/static/css/bootstrap-3.1.1.min.css" rel="stylesheet">
    <link href="/static/css/bootstrap-theme-3.1.1.min.css" rel="stylesheet">
    <link href="/${release}/css/mobile.css" rel="stylesheet">
</head>
<%
    final String redirect_uri = (String) request.getAttribute("redirect_uri");
    String signInURL = redirect_uri !=null
                     ? "/signIn?r=" + redirect_uri
                     : "/signIn";
%>
<body>
    <div class="site-wrapper">
        <div class="site-wrapper-inner">
            <div class="cover-container">
                <div class="innner cover">
                    <div class="row">
                        <div class="col-md-4 col-md-offset-4 sireg-form-wrapper">
                            <form role="form" class="form-horizontal" action="<%=signInURL%>" method="POST">
                                <div class="form-group">
                                    <label for="f_username" class="col-sm-4 control-label">Username (or email)</label>
                                    <div class="col-sm-8">
                                        <input class="form-control" type="text" autocorrect="off" autocapitalize="off"
                                               id="f_username" name="f_username" placeholder="Username/Email">
                                    </div>
                                </div>
                                <div class="form-group">
                                    <label for="f_password" class="col-sm-4 control-label">Password</label>
                                    <div class="col-sm-8">
                                        <input class="form-control" type="password" autocorrect="off" autocapitalize="off"
                                               id="f_password" name="f_password" placeholder="••••••••">
                                    </div>
                                </div>
                                <div class="form-group">
                                    <div class="col-sm-offset-4 col-sm-8">
                                        <button type="submit"
                                                onclick="if (typeof(ga)!='undefined') {ga('send', 'event', 'button', 'click', 'simpleLogin', 1);}"
                                                class="btn btn-primary">Sign In</button>
                                        <a href="register" class="btn btn-default">Register</a>
                                    </div>
                                </div>
                            </form>
                            
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>

</body>
</html>
