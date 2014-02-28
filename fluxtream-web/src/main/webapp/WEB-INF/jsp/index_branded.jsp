<!DOCTYPE html>
<%
    Boolean tracker = (Boolean)request.getAttribute("tracker");
    Boolean supportsFBLogin = (Boolean)request.getAttribute("supportsFBLogin");
%>
<html>
<head>
    <link href="/static/css/bootstrap-2.3.2.min.css" rel="stylesheet">
    <link href="/static/css/bootstrap-responsive-2.3.2.min.css" rel="stylesheet">
    <link href="${release}/css/index.css" rel="stylesheet" type="text/css"/>
    <link href='//fonts.googleapis.com/css?family=Open+Sans:400,700,600' rel='stylesheet' type='text/css'>
    <script src="//ajax.googleapis.com/ajax/libs/jquery/1.7.2/jquery.min.js"></script>
    <script src="/static/js/bootstrap-2.3.2.min.js"></script>
    <script type="text/javascript" src="/${release}/assets-home/js/welcome.js"></script>
    <!-- fav icons -->
    <link rel="shortcut icon" href="/favicon.ico">
</head>
<body>
<div id="wrapper">
    <div id="header">
        <a href="#"><div class="logo"></div></a>
        <div class="login_container">
            <form method="post" action="signIn" id="loginForm">
                <table>
                    <tr>
                        <td><label class="username_txt" for="f_username">Username/email:</label></td>
                        <td><label class="password_txt" for="f_password">Password:</label></td>
                        <td rowspan="2"><button class="login_btn">Login</button></td>
                    </tr>
                    <tr>
                        <td><input type="text" onkeypress="if(event.which==13) document.forms[0].submit();" class="username_input" id="f_username" name="f_username"></td>
                        <td><input type="password" onkeypress="if(event.which==13) document.forms[0].submit();" class="password_input" id="f_password" name="f_password"></td>
                    </tr>
                    <tr>
                        <td></td>
                        <td><a href="#" class="forgotPassword" id="lostPasswdLink">Forgot password?</a></td>
                    </tr>
                </table>
            </form>
            <form method="post" action="support/sendResetRequest" style="display:none" id="recoverForm">
                <table>
                    <tr>
                        <td><label class="recover_email_txt" for="recover_email">Your email address:</label></td>
                        <td rowspan="2"><button class="login_btn">Send</button></td>
                    </tr>
                    <tr>
                        <td><input class="recover_email_input" title="Enter your email" type="text" name="recover[email]" id="recover_email"></td>
                    </tr>
                    <tr>
                        <td><a href="#" class="forgotPassword" id="cancelRecoverPassword">cancel</a></td>
                        <td></td>
                    </tr>
                </table>
                <%--<p id="recoverPasswordFeedback" style="display:none;" class="sysInfo">A confirmation link has been emailed to you_XXX</p>--%>
            </form>
        </div>
    </div>
    <div id="introBar">
        <div id="intro">
            <table>
                <tr>
                    <td rowspan="2">
                        <br><br>
                        <iframe width="432" height="324" src="//www.youtube.com/embed/D85ICrW52e0?hd=1" frameborder="0" allowfullscreen></iframe>
                    </td>
                    <td>
                        <div class="intro_txt">
                            An open-source non-profit personal data visualization framework to help you make sense of your life and compare hypotheses about what affects your well-being
                        </div>
                    </td>
                </tr>
                <tr>
                    <td>
                        <div class="register_txt">
                            It's free!
                        </div><button style="cursor:pointer" id="registerShow" class="register_btn">Register</button>
                    </td>
                </tr>
            </table>
        </div>
    </div>
    <div id="content">
        <table>
            <tr class="sc_highlight">
                <td><a href="javascript:showLightbox(1)"><img src="images/s1.png" class="screenshot_img_general screenshot_img_1"/></a></td>
                <td><h1 class="screenshot_txt_1">Take control of your data</h1>
                    <div class="screenshot_txt_2">
                        Choose from our growing list of supported devices and APIs. Importing your data is easy.
                    </div></td>
                <td rowspan="5"><div class="connector_img"></div></td>
            </tr>
            <tr class="sc_highlight">
                <td><a href="javascript:showLightbox(2);"><img src="images/s2.png" class="screenshot_img_general screenshot_img_2"/></a></td>
                <td><h1 class="screenshot_txt_1">The whole is more than the sum of its parts</h1>
                    <div class="screenshot_txt_2">
                        As you keep adding more different data types, the picture of your life will unfold.
                    </div></td>
            </tr>
            <tr class="sc_highlight">
                <td><a href="javascript:showLightbox(3)"><img src="images/s3.png" class="screenshot_img_general screenshot_img_3"/></a></td>
                <td><h1 class="screenshot_txt_1">Identify different patterns at different scales</h1>
                    <div class="screenshot_txt_2">
                        Long term and short term trends sometimes tell different stories.
                    </div></td>
            </tr>
            <tr class="sc_highlight">
                <td><a href="javascript:showLightbox(4)"><img src="images/s4.png" class="screenshot_img_general screenshot_img_4"/></a></td>
                <td><h1 class="screenshot_txt_1">Explore your daily life, be curious</h1>
                    <div class="screenshot_txt_2">
                        Awareness is a muscle. Fluxtream gives you the tools you need to exercise it.
                    </div></td>
            </tr>
        </table>

        <hr class="line1">
        <div class="about_txt">
            <p>
                Embarking upon the practice of self-tracking and self-reflection is like learning to cook or play a sport.  Leveraging the right tools is important, but they are only part of the process.  It is richer and more enjoyable when sharing the journey with others.
            </p>
        </div>
        <div class="nav">
            <ul>
                <li>
                    <a href="${release}/html/QuickStartGuide.pdf" class="guideLink">Quick start guide [PDF]</a>
                </li>
                <li>
                    <a href="${release}/html/privacyPolicy.html" class="policyLink">Privacy policy and terms of use</a>
                </li>
                <li>
                    <a href="https://github.com/fluxtream/fluxtream-app/wiki" class="developerLink">Developers: join our open-source team!</a>
                </li>

<!-- Comment this out until we have something good to point a learn more link at.  
     Modify guideLink and developerLink in index.css when this is added back in -->
<!--                <li> -->
<!--                    <a href="#" class="learnMoreLink">Learn more</a> -->
<!--                </li> -->

            </ul>
        </div>
    </div>
    <div id="footerBar">
        <div id="footer">
            <a href="#"><div class="createLab_logo"></div></a>
            <div class="creator">
                The BodyTrack Team @ CMU CREATE Lab & Candide Kemmler.
            </div>
            <div class="copyright">
                Copyright &copy 2012 - All rights reserved.
            </div>
        </div>
    </div>
</div>
<div class="modal fade hide" id="accessDeniedModal">
    <div class="modal-header">
        <button type="button" class="close" data-dismiss="modal">&times;</button>
        <h3>Login Failed</h3>
    </div>
    <div class="modal-body">
        <div class="alert alert-error">
            <strong>Not so fast, Hot Shot!</strong> Login is required before you access this resource.
        </div>
    </div>
    <div class="modal-footer">
        <a href="#" class="btn" data-dismiss="modal">Ok</a>
    </div>
</div>

<div class="modal fade hide" id="loginFailedModal">
    <div class="modal-header">
        <button type="button" class="close" data-dismiss="modal">&times;</button>
        <h3>Login Failed</h3>
    </div>
    <div class="modal-body">
        <div class="alert alert-error">
            <strong>Bad login credentials!</strong> We could not authenticate you properly. Either your username or password were incorrect.
        </div>
    </div>
    <div class="modal-footer">
        <a href="#" class="btn" data-dismiss="modal">Ok</a>
    </div>
</div>

<div class="modal fade hide" id="subscribedModal">
    <div class="modal-header">
        <button type="button" class="close" data-dismiss="modal">&times;</button>
        <h3>Thank You!</h3>
    </div>
    <div class="modal-body">
        <strong>Thanks for subscribing!</strong> We'll send you an email when we (re-)open the doors to the website and you'll be among the first to know.
    </div>
    <div class="modal-footer">
        <a href="#" class="btn" data-dismiss="modal">Ok</a>
    </div>
</div>

<div class="modal fade hide" id="incompatibleBrowser">
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
    <div class="modal-footer">
        <a href="#" class="btn" data-dismiss="modal">Ok</a>
    </div>
</div>

<div class="modal fade hide" id="screenshots-lightbox">
    <div class="modal-header">
        <button type="button" class="close" data-dismiss="modal">&times;</button>
        <h4>UX Examples</h4>
    </div>
    <div class="modal-body">
        <div id="myCarousel" class="carousel">
            <ol class="carousel-indicators">
                <li data-target="#myCarousel" data-slide-to="0" class="active"></li>
                <li data-target="#myCarousel" data-slide-to="1"></li>
                <li data-target="#myCarousel" data-slide-to="2"></li>
            </ol>
            <!-- Carousel items -->
            <div class="carousel-inner">
                <div class="active item">
                    <img src="images/s1.png"/>
                </div>
                <div class="item">
                    <img src="images/s2.png"/>
                </div>
                <div class="item">
                    <img src="images/s3.png"/>
                </div>
                <div class="item">
                    <img src="images/s4.png"/>
                </div>
            </div>
            <!-- Carousel nav -->
            <a class="carousel-control left" href="#myCarousel" data-slide="prev">&lsaquo;</a>
            <a class="carousel-control right" href="#myCarousel" data-slide="next">&rsaquo;</a>
        </div>
    </div>
    <div class="modal-footer">
        <a href="#" class="btn" data-dismiss="modal">Dismiss</a>
    </div>
</div>


<div id="registerModalWrapper">
</div>

<% if (tracker) { %>
<jsp:include page="tracker.jsp"/>
<% } %>

</body>
</html>
