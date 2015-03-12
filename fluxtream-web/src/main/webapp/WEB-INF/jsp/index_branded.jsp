<!DOCTYPE html>
<%
    Boolean tracker = (Boolean)request.getAttribute("tracker");
%><html>
<head>
    <meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1" />
    <meta name="apple-mobile-web-app-capable" content="yes" />
    <meta name="viewport" content="user-scalable=no, initial-scale=1.0, maximum-scale=1.0" />
    <meta name="apple-mobile-web-app-status-bar-style" content="yes" />
    <script type="text/javascript">var NREUMQ=NREUMQ||[];NREUMQ.push(["mark","firstbyte",new Date().getTime()]);</script>
    <link href="static/css/bootstrap-3.2.0.min.css" rel="stylesheet">
    <link href="${release}/css/index-2014.css" rel="stylesheet" type="text/css"/>
    <link href='//fonts.googleapis.com/css?family=Open+Sans:400,700,600' rel='stylesheet' type='text/css'>
    <script src="//ajax.googleapis.com/ajax/libs/jquery/1.7.2/jquery.min.js"></script>
    <script src="/static/js/jquery.fastbutton-1.0.0.js"></script>
    <script src="static/js/bootstrap-3.2.0.min.js"></script>
    <script type="text/javascript" src="/${release}/js/welcome.js"></script>
    <script>
        if (typeof window.devicePixelRatio !="undefined"){
            var retina = window.devicePixelRatio > 1;
            setCookie("retina", retina?"1":"0", 30);
        }
    </script>
    <!-- fav icons -->
    <link rel="shortcut icon" href="favicon.ico">
</head>
<div id="wrapper">
<div id="header" class="container logo-login-container">
    <a href="javascript:void(0)">
        <h1 class="logo text-hide">Fluxtream</h1>
    </a>
    <a class="blog_link" href="http://blog.fluxtream.org/"><span>Blog</span></a>
    <div class="xs-login visible-xs visible-sm">
        <button class="login_btn btn btn-primary form-control" onclick="enterCredentials()">Login</button>
    </div>
    <div class="login_container">
        <form method="post" action="signIn" id="loginForm" class="form-inline" role="form">
            <div class="form-group">
                <label class="username_txt sr-only" for="f_username">Username/email:</label>
                <input type="text" autocapitalize="off" onkeypress="if(event.which==13) document.forms[0].submit();" class="form-control username_input" id="f_username" name="f_username" placeholder="username/email">
            </div>
            <div class="form-group">
                <label class="password_txt sr-only" for="f_password">Password:</label>
                <input type="password" onkeypress="if(event.which==13) document.forms[0].submit();" class="password_input form-control" id="f_password" name="f_password" placeholder="password">
            </div>
            <div class="form-group">
                <button class="login_btn btn btn-primary form-control">Login</button>
                <a href="javascript:void(0)" class="forgotPassword" id="lostPasswdLink">Forgot password?</a>
            </div>
        </form>
        <form method="post" action="support/sendResetRequest" style="display:none" id="recoverForm" role="form" class="form-inline">
            <div class="form-group">
                <label class="recover_email_txt sr-only" for="recover_email">Your email address:</label>
                <input autocapitalize="off" class="recover_email_input form-control" title="Enter your email" type="email" name="recover[email]" id="recover_email" placeholder="Enter your email">
            </div>
            <div class="form-group">
                <button class="login_btn btn btn-primary form-control">Send</button>
                <a href="javascript:void(0)" class="forgotPassword " id="cancelRecoverPassword">cancel</a>
            </div>
        </form>
    </div> <!-- /.login_container/-->
</div><!-- /.container/-->
<div id="introBar" class="lasagna-layer-1">
    <div id="intro" class="container">
        <section class="row">
            <div class="col-xs-12 col-sm-5 col-sm-offset-1 col-md-5 col-lg-4 col-lg-offset-1">
                <iframe src="//www.youtube.com/embed/D85ICrW52e0?hd=1&controls=0&showinfo=0" frameborder="0" allowfullscreen></iframe>
            </div>
            <div class="intro_txt col-xs-12 col-sm-6 col-md-6 col-lg-5">
                <p>An <a href="https://github.com/fluxtream/fluxtream-app" title="github repo" target="_blank">open-source</a> non-profit personal data visualization framework to help you make sense of your life and compare hypotheses about what affects your well-being
                </p>
                <div class="row">
                    <div class="register_txt col-xs-6 col-md-6 col-lg-6">
                        <p>It's free!</p>
                    </div>
                    <div class="col-xs-6 col-md-6 col-lg-6">
                        <button style="cursor:pointer" id="registerShow" class="register_btn btn-primary btn btn-lg">Register</button>
                    </div>
                </div> <!-- /.row -->
            </div> <!-- /.col -->
        </section> <!-- /.row -->
        <div class="api-hire-row">
            <div class="api-advert-container">
                <a href="/dev" class="api-advert">We have an <span>API!</span></a>
            </div>
        </div> <!-- /.row -->
    </div> <!-- /.container -->
</div> <!-- /.lasagna-layer-1 -->
<div class="container-fluid hidden-xs hidden-sm">
    <section class="row connectors">
        <div class="expand-control hidden-xs">
            <a href="javascript:void(0)" class="handle please-expand">
                <span class="handle-bar"></span>
                <span class="handle-bar"></span>
                <span class="handle-bar"></span>
            </a>
        </div>
        <div class="connector-img-cloud connector-img-cloud-lg col-xs-12 col-sm-12 col-md-12 col-lg-12">
            <div class="connector-cloud"></div>
            <a href="mailto:info@fluxtream.org" class="hire-advert" title="contact">Need something custom? <strong>Hire us!</strong></a>

        </div>
    </section>
</div>
<div class="container">
    <section class="row take-control">
        <div class="col-xs-12 col-sm-5 col-sm-offset-1 col-md-4 col-md-offset-2 hidden-xs">
            <a href="javascript:void(0)" style="cursor:default" class="img">
                <img src="images/s1.png" class="screenshot_img_general screenshot_img_1"/>
            </a>
        </div>
        <div class="col-xs-12 col-sm-5 col-md-4">
            <h2 class="screenshot_txt_1">Take control of your data</h2>
            <p class="screenshot_txt_2">
                Choose from our growing list of supported devices and APIs. Importing your data is easy.
            </p>
        </div>
        <div class="col-xs-12 visible-xs">
          <span class="img">
            <img src="images/s1.png" class="screenshot_img_general screenshot_img_1" alt="*"/>
          </span>
        </div>
    </section>
</div>
<div class="container-fluid hidden-lg hidden-md">
    <section class="row connectors">
        <div class="expand-control hidden-xs">
            <a href="javascript:void(0)" class="handle please-expand">
                <span class="handle-bar"></span>
                <span class="handle-bar"></span>
                <span class="handle-bar"></span>
            </a>
        </div>
        <div class="connector-img-cloud-sm connector-img-cloud col-xs-12 col-sm-12 col-md-12 col-lg-12">
            <div class="connector-cloud"></div>
            <a class="hire-advert" href="#" title="contact">Need something custom? <strong>Hire us!</strong></a>
        </div>
    </section>
</div>
<div class="container">
    <section class="row the-whole">
        <div class="col-sm-5 col-sm-push-6 col-md-4 hidden-xs">
            <a href="javascript:void(0);" style="cursor:default" class="img">
                <img src="images/s2.png" class="screenshot_img_general screenshot_img_2"/>
            </a>
        </div>
        <div class="col-sm-5 col-sm-pull-4 col-md-4 col-md-pull-2">
            <h2 class="screenshot_txt_1">The whole is more than the sum of its parts</h2>
            <p class="screenshot_txt_2">
                As you keep adding more different data types, the picture of your life will unfold.
            </p>
        </div>
        <div class="col-xs-12 visible-xs">
          <span class="img">
            <img src="images/s2.png" class="screenshot_img_general screenshot_img_2" alt="*"/>
          </span>
        </div>
    </section>
</div>
<div class="container">
    <section class="row patterns">
        <div class="col-sm-5 col-sm-offset-1 col-md-4 col-md-offset-2 hidden-xs">
            <a href="javascript:void(0)" style="cursor:default" class="img">
                <img src="images/s3.png" class="screenshot_img_general screenshot_img_3"/>
            </a>
        </div>
        <div class="col-sm-5 col-md-4">
            <h2 class="screenshot_txt_1">Identify different patterns at different scales</h2>
            <p class="screenshot_txt_2">
                Long term and short term trends sometimes tell different stories.
            </p>
        </div>
        <div class="col-xs-12 visible-xs">
          <span class="img">
            <img src="images/s3.png" class="screenshot_img_general screenshot_img_3" alt="*"/>
          </span>
        </div>
    </section>
</div>
<div class="container">
    <section class="row explore">
        <div class="col-sm-5 col-sm-push-6 col-md-4 hidden-xs">
            <a href="javascript:void(0)" style="cursor:default" class="img">
                <img src="images/s4.png" class="screenshot_img_general screenshot_img_4"/>
            </a>
        </div>
        <div class="col-sm-5 col-sm-pull-4 col-md-4 col-md-pull-2">
            <h2 class="screenshot_txt_1">Explore your daily life,<br/>be curious</h2>
            <p class="screenshot_txt_2">
                Awareness is a muscle. Fluxtream gives you the tools you need to exercise it.
            </p>
        </div>
        <div class="col-xs-12 visible-xs">
          <span class="img">
            <img src="images/s4.png" class="screenshot_img_general screenshot_img_4" align="*"/>
          </span>
        </div>
    </section>
</div>
<!--
        <aside class="col-sm-12 col-md-3">
          <div class="connector_img">

          </div>
        </aside>
-->
<div class="lasagna-layer-3">
    <div class="container">
        <div class="row">
            <div class="about_txt col-sm-8 col-sm-offset-2 col-md-8 col-md-offset-2">
                <p>
                    Embarking upon the practice of self-tracking and self-reflection is like learning to cook or play a sport.  Leveraging the right tools is important, but they are only part of the process.  <span>It is richer and more enjoyable when sharing the journey with others.</span>
                </p>
            </div>
        </div>
    </div>
</div>

<div class="lasagna-layer-4">
    <div class="container">
        <div class="row">
            <div class="nav col-xs-12 col-sm-12">
                <ul>
                    <li>
                        <a href="html/QuickStartGuide.pdf" class="guideLink">Quick start guide [PDF]</a>
                    </li>
                    <li>
                        <a href="html/privacyPolicy.html" class="policyLink">Privacy policy and terms of use</a>
                    </li>
                    <li>
                        <a href="/dev/" class="developerLink">For Developers: API & Build Instructions</a>
                    </li>

                    <!-- Comment this out until we have something good to point a learn more link at.
                     Modify guideLink and developerLink in index.css when this is added back in -->
                    <!--                <li> -->
                    <!--                    <a href="javascript:void(0)" class="learnMoreLink">Learn more</a> -->
                    <!--                </li> -->

                </ul>
            </div> <!-- /.nav -->
        </div> <!-- /.row -->
        <div class="row">
          <div class="col-xs-12 col-sm-12">
            <a class="github" href="https://github.com/fluxtream/fluxtream-app"><span class="linkgithub">Fork me on </span><span class="text-hide">GitHub</span></a>
          </div>
        </div>
    </div> <!-- /.container -->
</div> <!-- /.lasagna-layer-4 -->

<% int currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR); String until = ""; if (currentYear>2011) until = " - " + currentYear; %>
<footer id="footerBar" role="footer" class="container-fluid">
    <div id="footer" class="footer-content row">
        <a href="http://www.cmucreatelab.org/" class="col-sm-4 col-md-4 col-md-offset-1 col-lg-3 col-lg-offset-2">
            <div class="createLab_logo text-hide">Create Lab</div>
        </a>
        <div class="creator col-sm-8 col-md-6 col-md-offset-1 col-lg-5 col-lg-offset-1">
            The BodyTrack Team @ CMU CREATE Lab & Candide Kemmler.
        </div>
        <div class="copyright col-sm-8 col-sm-offset-4 col-md-6 col-md-offset-6 col-lg-5 col-lg-offset-6">
            Copyright &copy; 2011<%=until %> - All rights reserved.
        </div>
    </div>
</footer>


</div>

<div class="modal fade" id="accessDeniedModal">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal">&times;</button>
                <h3>Login Failed</h3>
            </div>
            <div class="modal-body">
                <div class="alert alert-error">
                    <strong>Not so fast, Hot Shot!</strong> Login is required before you access this resource.
                </div>
            </div>
            <div class="modal-footer" style="height:65px">
                <a href="javascript:void(0)" class="login_btn btn btn-primary" data-dismiss="modal">Ok</a>
            </div>
        </div>
    </div>
</div>

<div class="modal fade" id="xsLoginDialog">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal">&times;</button>
                <h3 id="xsLoginFormTitle">Please log in</h3>
            </div>
            <div class="modal-body">
                <form method="post" action="signIn" id="xsLoginForm" role="form">
                    <div class="form-group">
                        <label class="username_txt sr-only" for="f_username_xs">Username/email:</label>
                        <input type="text" autocapitalize="off" onkeypress="if(event.which==13) document.forms[1].submit();" class="form-control username_input" id="f_username_xs" name="f_username" placeholder="username/email">
                    </div>
                    <div class="form-group">
                        <label class="password_txt sr-only" for="f_password_xs">Password:</label>
                        <input type="password" onkeypress="if(event.which==13) document.forms[1].submit();" class="password_input form-control" id="f_password_xs" name="f_password" placeholder="password">
                    </div>
                    <div class="form-group">
                        <a href="javascript:void(0)" class="forgotPassword" id="xslostPasswdLink">Forgot password?</a>
                    </div>
                    <div class="form-group">
                        <button class="login_btn btn btn-primary form-control">Login</button>
                        <%--<a href="javascript:void(0)" class="forgotPassword">Forgot password?</a>--%>
                    </div>
                </form>
                <form method="post" action="support/sendResetRequest" style="display:none" id="xsRecoverForm" role="form" class="form-inline">
                    <div class="form-group">
                        <label class="recover_email_txt sr-only" for="recover_email_xs">Your email address:</label>
                        <input class="recover_email_input form-control" title="Enter your email" type="email" name="recover[email]" id="Recover_email_xs" placeholder="Enter your email">
                    </div>
                    <div class="form-group">
                        <a href="javascript:void(0)" id="xsCancelRecoverPassword">cancel</a>
                    </div>
                    <div class="form-group">
                        <button class="login_btn btn btn-primary form-control">Send</button>
                    </div>
                </form>
            </div>
            <div class="modal-footer" style="height:65px">
                <a href="javascript:void(0)" class="login_btn btn btn-primary" data-dismiss="modal">Dismiss</a>
            </div>
        </div>
    </div>
</div>


<div class="modal fade" id="loginFailedModal">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal">&times;</button>
                <h3>Login Failed</h3>
            </div>
            <div class="modal-body">
                <div class="alert alert-error">
                    <strong>Bad login credentials!</strong> We could not authenticate you properly. Either your username or password were incorrect.
                </div>
            </div>
            <div class="modal-footer" style="height:65px">
                <a href="javascript:void(0)" onclick="$('#f_username').select();$('#f_username').focus();" class="btn login_btn btn-primary" data-dismiss="modal">Ok</a>
            </div>
        </div>
    </div>
</div>

<div class="modal fade" id="incompatibleBrowser">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal">&times;</button>
                <h3>Incompatibility Warning</h3>
            </div>
            <div class="modal-body">
                <div class="alert alert-error">
                    <strong>Your browser isn't fully compatible!</strong> We've determined that your web browser doesn't support all of the features
                    required for the best experience. You can still try to use the website but some features may be broken. We recommend updating to
                    a newer version of your web browser.
                </div>
            </div>
            <div class="modal-footer" style="height:65px">
                <a href="javascript:void(0)" class="btn login_btn btn-primary" data-dismiss="modal">Ok</a>
            </div>
        </div>
    </div>
</div>


<div id="registerModalWrapper">
</div>

<% if (tracker) { %>
<jsp:include page="tracker.jsp"/>
<% } %>


</body>
</html>
