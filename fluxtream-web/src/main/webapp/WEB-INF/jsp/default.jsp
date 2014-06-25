<!DOCTYPE html>
<%
    Boolean tracker = (Boolean)request.getAttribute("tracker");
%>
<html lang="en">
<head>
    <meta charset="utf-8">
    <title>Fluxtream</title>
    <meta name="viewport" content="width=1000, initial-scale=1.0">
    <meta name="description" content="Landing page,Subscribe">
    <meta name="author" content="">
    <!-- Styles -->
    <link href="/static/css/bootstrap-2.0.3.min.css" rel="stylesheet">
    <link href="/static/css/bootstrap-responsive-2.0.3.min.css" rel="stylesheet">
    <link rel="stylesheet" href="/static/css/font-awesome-3.2.1.css">
    <link href="/${release}/assets-home/css/style-responsive-default.css" rel="stylesheet">
    <!-- Le HTML5 shim, for IE6-8 support of HTML5 elements -->
    <!--[if lt IE 9]>
    <!--<script src="//html5shim.googlecode.com/svn/trunk/html5.js"></script>-->
    <![endif]-->

    <script src="/static/js/modernizr-2.6.2-custom1.js"></script>

    <!-- For IE 7 and 8 Media Query Support -->
    <script type="text/javascript" src="/${release}/assets-home/js/respond.js"></script>
    <script src="//ajax.googleapis.com/ajax/libs/jquery/1.7.2/jquery.min.js"></script>
    <script src="/static/js/bootstrap-2.0.2.min.js"></script>
    <script type="text/javascript" src="/${release}/assets-home/js/welcome.js"></script>

    <!-- fav icons -->
    <link rel="shortcut icon" href="/favicon.ico">
</head>
<body>

<div id="login" style="display:none;">
    <div class="cont cf">

        <form method="post" action="signIn" id="loginForm">
            <label for="f_username">Username</label>
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
        <p><a href="javascript:void(0)" id="lostPasswdLink">Lost password?</a></p>
    </div>
</div>




<!-- <a href="https://github.com/fluxtream/fluxtream-app"><img style="position: absolute; top: 0; right: 0; border: 0; z-index:105;" src="https://s3.amazonaws.com/github/ribbons/forkme_right_darkblue_121621.png" alt="Fork me on GitHub"></a> -->




<div id="intro">

    <div class="wrapper">
        <div id="login-collapse">
            <div class="cont" style="margin-left:10px;margin-top:3px;">
                <a href="javascript:void(0)" style="cursor:pointer;font-size:16px" id="toggleLoginPanel"><span>Login</span></a>
                <a role="button" style="cursor:pointer;font-size:16px" id="registerShow" class="regLink">Register</a>
            </div>
        </div>

        <!-- register modal -->




    </div><!-- end .wrapper -->
</div><!-- end #intro -->


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
        <a href="javascript:void(0)" class="btn" data-dismiss="modal">Ok</a>
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
        <a href="javascript:void(0)" class="btn" data-dismiss="modal">Ok</a>
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
        <a href="javascript:void(0)" class="btn" data-dismiss="modal">Ok</a>
    </div>
</div>


<div id="registerModalWrapper">
</div>

<% if (tracker) { %>
<jsp:include page="tracker.jsp"/>
<% } %>


</body>
</html>
