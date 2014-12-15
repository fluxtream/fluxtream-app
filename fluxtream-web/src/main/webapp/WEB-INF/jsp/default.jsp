<!DOCTYPE html>
<html>
<head>
    <title>Fluxtream - exercise your awareness muscle</title>
    <script type="text/javascript">var NREUMQ=NREUMQ||[];NREUMQ.push(["mark","firstbyte",new Date().getTime()]);</script>
<!--     <link href="static/css/bootstrap-2.3.2.min.css" rel="stylesheet"> -->
<!--     <link href="static/css/bootstrap-responsive-2.3.2.min.css" rel="stylesheet"> -->
    <link href="static/css/bootstrap-3.2.0.min.css" rel="stylesheet">
    <link href="css/index-2014.css" rel="stylesheet" type="text/css"/>
    <link href='//fonts.googleapis.com/css?family=Open+Sans:400,700,600' rel='stylesheet' type='text/css'>
    <script src="//ajax.googleapis.com/ajax/libs/jquery/1.7.2/jquery.min.js"></script>
<!--     <script src="static/js/bootstrap-2.3.2.min.js"></script> -->
    <script src="static/js/bootstrap-3.2.0.min.js"></script>
    <script type="text/javascript" src="${release}/js/welcome.js"></script>
    <!-- fav icons -->
    <link rel="shortcut icon" href="favicon.ico">
</head>
<body>

<div id="wrapper">
    <div class="startup-page">
        <div class="startup-verticalizer">
            <div class="startup-centerizer">
               <div class="logo_default text-hide" >
                    Fluxtream
                </div>
                <h1 class="congrats-title">Congrats!</h1>
                <p class="congrats-txt">You have made it!</p>
                <div class="buttons">
                <button style="cursor:pointer" onclick="enterCredentials()" class="register_btn btn-primary btn btn-lg">Login</button>
                <button style="cursor:pointer" id="registerShow" class="register_btn btn-primary btn btn-lg">Register</button>
                </div>
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
        <a href="javascript:void(0)" class="btn" data-dismiss="modal">Ok</a>
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
        <a href="javascript:void(0)" class="btn" data-dismiss="modal">Dismiss</a>
    </div>
</div> <!-- /.wrapper -->

<div id="registerModalWrapper">
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


<script type="text/javascript">
  $(".please-expand" ).click(function() {
      $( ".connector-img-cloud" ).toggleClass( "connector-img-cloud-open" );
  });
</script>

<script type="text/javascript">if (!NREUMQ.f) { NREUMQ.f=function() {NREUMQ.push(["load",new Date().getTime()]);var e=document.createElement("script");e.type="text/javascript";e.src=(("http:"===document.location.protocol)?"http:":"https:") + "//" + "js-agent.newrelic.com/nr-100.js";document.body.appendChild(e);if(NREUMQ.a)NREUMQ.a();};NREUMQ.a=window.onload;window.onload=NREUMQ.f;};NREUMQ.push(["nrfj","beacon-1.newrelic.com","320efd248b","2133971","YVxbZUcHCEAHUkdcW1gWakFHDwhUJV5dQUZZVVVUR0knQxZyXFtARFZVXVAUSVoIVVZN",0,2,new Date().getTime(),"","","","",""]);</script></body>
</html>
