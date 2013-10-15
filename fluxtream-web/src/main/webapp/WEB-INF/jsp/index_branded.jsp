<!DOCTYPE html>
<%
    Boolean tracker = (Boolean)request.getAttribute("tracker");
    Boolean supportsFBLogin = (Boolean)request.getAttribute("supportsFBLogin");
%>
<html lang="en">
<head>
    <meta charset="utf-8">
    <title>Fluxtream</title>
    <meta name="viewport" content="width=1000, initial-scale=1.0">
    <meta name="description" content="Landing page,Subscribe">
    <meta name="author" content="">
    <!-- Styles -->
    <link href="/static/css/bootstrap-2.3.2.min.css" rel="stylesheet">
    <link href="/static/css/bootstrap-responsive-2.3.2.min.css" rel="stylesheet">
    <link rel="stylesheet" href="/static/css/font-awesome-3.2.1.css">
    <link href="/${release}/assets-home/css/style-responsive-red.css" rel="stylesheet">
    <link href="/${release}/assets-home/css/style.css" rel="stylesheet">
    <!-- Google Web Font-->
    <link href='//fonts.googleapis.com/css?family=Droid+Serif:400,700,400italic,700italic' rel='stylesheet' type='text/css'>
    <link href='//fonts.googleapis.com/css?family=Droid+Sans:400,700' rel='stylesheet' type='text/css'>
    <!--[if IE 7]><link rel="stylesheet" href="/${release}/assets-home/css/font-awesome-ie7.css"><![endif]-->
    <!-- Le HTML5 shim, for IE6-8 support of HTML5 elements -->
    <!--[if lt IE 9]>
    <!--<script src="//html5shim.googlecode.com/svn/trunk/html5.js"></script>-->
    <![endif]-->
    <script src="/static/js/modernizr-2.6.2-custom1.js"></script>

    <!-- For IE 7 and 8 Media Query Support -->
    <script type="text/javascript" src="/${release}/assets-home/js/respond.js"></script>
    <script src="//ajax.googleapis.com/ajax/libs/jquery/1.7.2/jquery.min.js"></script>
    <script src="/static/js/bootstrap-2.3.2.min.js"></script>
    <script type="text/javascript" src="/${release}/assets-home/js/welcome.js"></script>

    <!-- fav icons -->
    <link rel="shortcut icon" href="/favicon.ico">
</head>
<body>
<div id="fb-root"></div>
<% if (supportsFBLogin) { %><script>
    window.fbAsyncInit = function() {
        FB.init({
            appId      : '${facebookAppId}', // App ID
            channelUrl : 'channel.html', // Channel File
            status     : true, // check login status
            cookie     : true, // enable cookies to allow the server to access the session
            xfbml      : true  // parse XFBML
        });

        $(document).ready(function(){
            checkLoginStatus();
            $("#fbLogin").click(checkLoginStatus);
        });
    };

    function checkLoginStatus() {
        $("#fbLogin").popover('destroy');
        FB.getLoginStatus(function(response) {
            console.log("status:" + response.status);
            if (response.status === 'not_authorized') {
                noPostingPopover();
            } else if (response.status=='unknown') {
                notLoggedInPopover();
            }
        });
    }

    // Load the SDK asynchronously
    (function(d){
        var js, id = 'facebook-jssdk', ref = d.getElementsByTagName('script')[0];
        if (d.getElementById(id)) {return;}
        js = d.createElement('script'); js.id = id; js.async = true;
        js.src = "//connect.facebook.net/en_US/all.js";
        ref.parentNode.insertBefore(js, ref);
    }(document));

    function noPostingPopover() {
        $("#fbLogin").popover({
            container: "body",
            placement: "bottom",
            trigger: "hover",
            html: true,
            title: "Don't worry:",
            content: "We will <strong>NEVER</strong> post on your behalf!!!",
            animation: true
        });
    }

    function notLoggedInPopover() {
        $("#fbLogin").popover({
            container: "body",
            placement: "bottom",
            html: true,
            title: "You are not logged in to Facebook!",
            content: "Please <a href='http://www.facebook.com' target='_blank'>log in to Facebook</a> before using this button",
            animation: true
        });
    }

    // Here we run a very simple test of the Graph API after login is successful.
    // This testAPI() function is only called in those cases.
    function getIn(loginResponse) {
        $.ajax({
            url: "/api/facebook/login?access_token=" +loginResponse.authResponse.accessToken,
            type: "POST",

            success: function(status) {
                if (status.result==="OK") {
                    location = "/signIn?autoLoginToken="+status.payload;
                } else {
                    alert(status.message);
                }
            },
            error: function($, textStatus) {
                alert("there was an error while logging you in with facebook: " + textStatus);
            }
        });
    }

    function fbLogin() {
        var timezone = jstz.determine_timezone(),
                d = new Date(),
                currentDate = d.getDate(),
                currentMonth = d.getMonth() + 1,
                currentYear = d.getFullYear();

        setCookie("timeZone", timezone.name(), 1);
        setCookie("date", currentYear + "-" + currentMonth + "-" + currentDate, 1);

        FB.getLoginStatus(function(response) {
            if (response.status === 'connected') {
                getIn(response);
            } else if (response.status === 'not_authorized') {
                FB.login(function(loginResponse){
                    if (loginResponse.authResponse) {
                        getIn(loginResponse);
                    }
                },{scope: 'email'});
            }
        });
    }
</script><% } %>
<div id="login" style="display:none;">
    <div class="cont cf">

        <form method="post" action="signIn" id="loginForm">
            <label for="f_username">Username/email</label>
            <input title="Username" autocorrect="off" autocapitalize="off" autocomplete="on" onkeypress="if(event.which==13) document.forms[0].submit();" id="f_username" name="f_username" placeholder="Username" value="<%=request.getParameter("username") != null ? request.getParameter("username") : ""%>" type="text">
            <label for="f_password">Password</label>
            <input title="Password" value="" onkeypress="if(event.which==13) document.forms[0].submit();" id="f_password" name="f_password" placeholder="Password" type="password"/>
            <input type="submit" value="Submit" class="btn " />
            <input type="hidden" name="login[_csrf_token]" value="965e5cf220b402ba98a8344266bb804d" id="login__csrf_token" />      </form>

        <form method="post" action="support/sendResetRequest" style="display:none" id="recoverForm">

            <label for="recover_email">Enter your email</label>      	<input title="Enter your email" type="text" name="recover[email]" id="recover_email" />
            <input type="submit" class="btn" value="Submit">
            <input type="hidden" name="recover[_csrf_token]" value="ebb981ef2ee1ad730d0e676d2af2336c" id="recover__csrf_token" />
        </form>
        <p id="recoverPasswordFeedback" style="display:none;" class="sysInfo">A confirmation link has been emailed to you_XXX</p>
        <p><a href="#" id="lostPasswdLink">Lost password?</a></p>
    </div>
</div>




<!-- <a href="https://github.com/fluxtream/fluxtream-app"><img style="position: absolute; top: 0; right: 0; border: 0; z-index:105;" src="https://s3.amazonaws.com/github/ribbons/forkme_right_darkblue_121621.png" alt="Fork me on GitHub"></a> -->




<div id="intro">
    <div class="background"></div>

    <div class="wrapper">
        <% if (supportsFBLogin) { %>
        <a role="button" style="cursor:pointer" id="fbLogin" onclick="fbLogin();"></a>
        <% } %>

        <div id="login-collapse">
            <div class="cont">
                <a href="#" id="toggleLoginPanel"><span>Login</span></a>
            </div>
        </div>

        <div class="logo">
            <a href="#"><img src="/${release}/assets-home/img/logo-fulltext2.png"></a>
        </div>

        <!-- register modal -->

        <a role="button" style="cursor:pointer" id="registerShow" class="regLink">Register</a>


        <!-- end register modal -->

        <div class="page-title-wrapper">
            <div class="page-title">
                <h1>
                    <span class="title-wrap">An open-source<br>personal data<br>visualization framework<br>to help you make<br>sense of your life</span>
                </h1>
            </div><!-- end .page-title -->

        </div><!-- end .page-title-wrapper -->

    </div><!-- end .wrapper -->
</div><!-- end #intro -->

<div class="container">
    <!--<div class="row" id="catchycontent">
       <div class="span12">
         <h2>We're kicking the tires... </h2>
         <p>We've been quite busy lately: going open source, building a core team of developers, and completely revisiting the architecture.<br> Leave us your email below, and we'll notify you the minute we (re-)open the doors. </p>
     </div>
   </div><!--end row-->



    <div class="row" id="">
        <br><br>
        <div class="span6">
            <div style="padding:0 10px;">
                <h2>Documents</h2>
                <div class="dividerHoriBot">
                    <ul>
                        <li><a href="${release}/html/QuickStartGuide.pdf">Quick Start Guide [PDF]</a></li>
                        <li><a href="https://github.com/fluxtream/fluxtream-app/wiki" target="_blank">Build instructions</a></li>
                        <li><a href="${release}/html/privacyPolicy.html">Privacy Policy and Terms of Use</a></li>
                    </ul>
                </div>
                <div>
                    <div class="media">
                        <div class="img">
                            <div class="footericon img"><a href="https://twitter.com/fluxtream"><i class="icon-twitter-sign"></i></a></div>
                        </div>
                        <div class="bd">
                            <p>For more details leading up to our launch <br/>follow us on Twitter</p>
                        </div>
                    </div>

                </div>
            </div>
        </div>


        <div class="span6" >
            <div style="padding:0 10px;">
                <h2>Recent events</h2>

                <div class="media">
                    <div class="img">
                        <a href="http://quantifiedself.com/" target="_blank"><img class="img" src="/${release}/assets-home/img/qs.png"></a>
                    </div>
                    <div class="bd">
                        <h3>Quantified Self 2013</h3>
                        <p>We will be participating in the Quantified Self 2013 Conference in San Francisco on Friday, October 10, 2013.  Join us for the "Quantified Self APIs" QS Conversation at 10:30 am and Office Hours at 1:30 pm.</p>  
                    </div>
                </div>

                <div class="media dividerHoriTop">
                    <div class="img">
                        <a href="http://www.oscon.com/" target="_blank"><img class="img" src="/${release}/assets-home/img/oscon2012.png"></a>
                    </div>
                    <div class="bd">
                        <h3>OScon 2012</h3>
                        <p>We presented the new combined Fluxtream/BodyTrack tool set at OSCon in Portland, Oregon on July 18, 2012.</p>
                    </div>
                </div>
            </div>
        </div>
    </div><!--end row-->

    <div class="row" id="footer">
        <div class="span6" >
            <div class="media">
                <div class="img">
                    <div class="featureicon"><a href="http://www.cmucreatelab.org/"><img src="/${release}/assets-home/img/createlab.png"></a></div>
                </div>
            </div>
        </div>

        <!--
              <h4>For more details leading up to our launch <br/>follow us on twitter</h4>
              <div class="footericon"><a href="https://twitter.com/fluxtream"><i class="icon-twitter-sign"></i></a></div>
        -->
        <div class="span6" >
            <p style="text-align:right;"> The BodyTrack Team @ CMU CREATE Lab &amp; Candide Kemmler.<br>&copy; 2012 - All rights reserved.</p>
        </div>
    </div><!--end row-->
</div><!--end container-->

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


<div id="registerModalWrapper">
</div>

<% if (tracker) { %>
<jsp:include page="tracker.jsp"/>
<% } %>


</body>
</html>
