<%--
  Created by IntelliJ IDEA.
  User: candide
  Date: 10/04/14
  Time: 12:28
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1" />
    <meta name="apple-mobile-web-app-capable" content="yes" />
    <meta name="viewport" content="user-scalable=no, initial-scale=1.0, maximum-scale=1.0" />
    <meta name="apple-mobile-web-app-status-bar-style" content="yes" />
    <title>Fluxtream oAuth2 Authorization</title>
    <link href="/static/css/bootstrap-3.1.1.min.css" rel="stylesheet">
    <link href="/static/css/bootstrap-theme-3.1.1.min.css" rel="stylesheet">
    <link href="/${release}/css/mobile.css" rel="stylesheet">
    <link href='//fonts.googleapis.com/css?family=Open+Sans:400,700,600' rel='stylesheet' type='text/css'>
    <script src="//ajax.googleapis.com/ajax/libs/jquery/1.7.2/jquery.min.js"></script>
    <!-- <script src="/static/js/bootstrap-2.3.2.min.js"></script> -->
    <!-- fav icons -->
    <link rel="shortcut icon" href="/favicon.ico">
</head>
<body>
    <div class="site-wrapper">
        <div class="site-wrapper-inner">
            <div class="cover-container">
                <div class="innner cover">
                    <div class="container">
                        <div class="row">
                            <div class="col-md-4 col-md-offset-4">
                                   <div class="logo text-hide">Fluxtream</div>
                            </div>
                        </div>
                         <div class="row">
                            <div class="col-md-4 col-md-offset-4">
                                <p>A Third-Party application is requesting access to your Fluxtream data.</p>
                                <dl class="dl-horizontal">
                                    <dt>App Name</dt>
                                    <dd>${name}</dd>
                                    <dt>App Description</dt>
                                    <dd>${description}</dd>
                                </dl>
                            </div>
                        </div>
                        <div class="row">
                            <div class="col-md-4 col-md-offset-4">
                                <form class="form-horizontal" action="/auth/oauth2/authorization" method="POST">
                                    <input type="hidden" name="redirectUri" value="${redirectUri}">
                                    <input type="hidden" name="code" value="${code}">
                                    <div class="control-group">
                                        <div class="controls">
                                            <label class="checkbox">
                                                <input name="granted" type="checkbox"> Grant Access
                                            </label>
                                            <span class="help-block">Note: you can revoke access to the Third-Party at any time.</span>
                                            <button type="submit" class="btn btn-primary">Send</button>
                                        </div>
                                    </div>
                                </form>
                            </div>
                        </div>
                    </div>
                 </div>
             </div>
        </div>
    </div>
</body>
</html>