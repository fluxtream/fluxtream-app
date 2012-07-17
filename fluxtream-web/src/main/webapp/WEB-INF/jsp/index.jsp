<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <title>Fluxtream</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="description" content="Landing page,Subscribe">
    <meta name="author" content="">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <!-- Styles -->
    <link href="/static/css/bootstrap-2.0.3.min.css" rel="stylesheet">
    <link href="/static/css/bootstrap-responsive-2.0.3.min.css" rel="stylesheet">
    <link rel="stylesheet" href="/static/css/font-awesome.css">
    <link href="/${release}/assets-home/css/style-responsive-red.css" rel="stylesheet">
    <link href="/${release}/assets-home/css/style.css" rel="stylesheet">
    <!-- Google Web Font-->
    <link href='http://fonts.googleapis.com/css?family=Droid+Serif:400,700,400italic,700italic' rel='stylesheet' type='text/css'>
    <link href='http://fonts.googleapis.com/css?family=Droid+Sans:400,700' rel='stylesheet' type='text/css'>
    <!--[if IE 7]><link rel="stylesheet" href="/${release}/assets-home/css/font-awesome-ie7.css"><![endif]-->
    <!-- Le HTML5 shim, for IE6-8 support of HTML5 elements -->
    <!--[if lt IE 9]>
    <script src="http://html5shim.googlecode.com/svn/trunk/html5.js"></script>
    <![endif]-->
    <!-- fav icons -->
    <link rel="shortcut icon" href="/favicon.ico">
</head>
<body>

<div id="login" style="display:none;">
    <div class="cont cf">

        <form method="post" action="signIn" id="loginForm">
            <label for="f_username">Email</label>
                <input title="Email" autocorrect="off" autocapitalize="off" autocomplete="on" onkeypress="if(event.which==13) document.forms[0].submit();" id="f_username" name="f_username" placeholder="Username" value="<%=request.getParameter("username") != null ? request.getParameter("username") : ""%>" type="text">
            <label for="f_password">Password</label>
                <input title="Password" value="" onkeypress="if(event.which==13) document.forms[0].submit();" id="f_password" name="f_password" placeholder="Password" type="password"></input>
            <input type="submit" value="Submit" class="btn " />
            <input type="hidden" name="login[_csrf_token]" value="965e5cf220b402ba98a8344266bb804d" id="login__csrf_token" />      </form>

        <form method="post" action="/index.php/en/register" style="display:none" id="recoverForm">

            <label for="recover_email">Enter your email</label>      	<input title="Enter your email" type="text" name="recover[email]" id="recover_email" />
            <input type="submit" id="mc-embedded-subscribe" class="btn" value="Submit">
            <input type="hidden" name="recover[_csrf_token]" value="ebb981ef2ee1ad730d0e676d2af2336c" id="recover__csrf_token" />
        </form>
        <p id="recoverPasswordFeedback" style="display:none;" class="sysInfo">A confirmation link has been emailed to you_XXX</p>
        <p><a href="/index.php/en/recover-password" id="lostPasswdLink">Lost password?</a></p>
    </div>
</div>




<a href="https://github.com/fluxtream/fluxtream-app"><img style="position: absolute; top: 0; right: 0; border: 0; z-index:105;" src="https://s3.amazonaws.com/github/ribbons/forkme_right_darkblue_121621.png" alt="Fork me on GitHub"></a>




<div id="intro">
    <div class="background"></div>

    <div class="wrapper">
        <div id="login-collapse">
            <div class="cont">
                <a href="#" id="toggleLoginPanel"><span>Login</span></a>
            </div>
        </div>

        <div class="logo">
            <a href="#"><img src="/${release}/assets-home/img/logo-fulltext2.png"></a>
        </div>

        <div class="page-title-wrapper">
            <div class="page-title">
                <h1>
                    <span class="title-wrap">An open-source<br>personal data<br>visualization framework<br>to help you<br>make sense of your life</span>
                </h1>
            </div><!-- end .page-title -->

        </div><!-- end .page-title-wrapper -->

    </div><!-- end .wrapper -->
    <div class="customer-card">
        <p><a href="http://www.flickr.com/people/25506891@N06/" target="_blank">Photo credit</a></p>
        <h2><a href="http://creativecommons.org/licenses/by-nc-sa/2.0/deed.en" target="_blank">Nasos Zovo</h2></a>


    </div>
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
                <h2>Documentation</h2>
                <div class="dividerHoriBot">
                    <ul>
                        <li><a href="#">Build instructions</a></li>
                        <li><a href="#">API documentation</a></li>
                    </ul>
                </div>
                <h2>Stay in the loop</h2>
                <div class="">


                    <div id="mc_embed_signup">
                        <p>Leave us your email below, and we'll notify you the minute we (re-)open the doors.</p>
                        <form action="http://fluxtream.createsend.com/t/j/s/ydtkyk/"  method="post" id="mc-embedded-subscribe-form" name="mc-embedded-subscribe-form" class="validate form-inline">
                            <input type="email" name="cm-ydtkyk-ydtkyk" id="ydtkyk-ydtkyk" name="EMAIL" class="span3 input-large email" id="mce-EMAIL" placeholder="email address"/>
                            <input type="submit" id="mc-embedded-subscribe" class="btn btn-success btn-large" value="Subscribe" />
                        </form>
                    </div>

                    <div class="media dividerHoriTop">
                        <div class="img">
                            <div class="footericon img"><a href="https://twitter.com/_Fluxtream"><i class="icon-twitter-sign"></i></a></div>
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
                <h2>Upcoming events</h2>

                <div class="media ">
                    <div class="img">
                        <a href="http://www.oscon.com/" target="_blank"><img class="img" src="/${release}/assets-home/img/oscon2012.png"></a>
                    </div>
                    <div class="bd">
                        <h3>OScon 2012</h3>
                        <p>We will be presenting the new combined Fluxtream/BodyTrack tool set at OSCon in Portland, Oregon on July 18, 2012. Please join us there!</p>
                    </div>
                </div>

                <div class="media dividerHoriTop">
                    <div class="img">
                        <a href="http://quantifiedself.com/" target="_blank"><img class="img" src="/${release}/assets-home/img/qs.png"></a>
                    </div>
                    <div class="bd">
                        <h3>Quantfied Self 2012</h3>
                        <p>We will be presenting an Ignite Plus talk at the Quantfied Self Conference in Stanford : "Data aggregation and exploration with Fluxtream/BodyTrack"</p>
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
              <div class="footericon"><a href="https://twitter.com/_Fluxtream"><i class="icon-twitter-sign"></i></a></div>
        -->
        <div class="span6" >
            <p style="text-align:right;"> The BodyTrack Team @ CMU CREATE Lab &amp; Candide Kemmler.<br>&copy; 2012 - All rights reserved.</p>
        </div>
    </div><!--end row-->
</div><!--end container-->
<!-- For IE 7 and 8 Media Query Support -->
<script type="text/javascript" src="/${release}/assets-home/js/respond.js"></script>
<script src="https://ajax.googleapis.com/ajax/libs/jquery/1.7.2/jquery.min.js"></script>
<script type="text/javascript">
    var jstz=function(){var b=function(a){a=-a.getTimezoneOffset();return a!==null?a:0},d=function(){return b(new Date(2010,0,1,0,0,0,0))},e=function(){return b(new Date(2010,5,1,0,0,0,0))},c=function(){var a=d(),b=e(),f=d()-e();if(f<0)return a+",1";else if(f>0)return b+",1,s";return a+",0"};return{determine_timezone:function(){var a=c();return new jstz.TimeZone(jstz.olson.timezones[a])},date_is_dst:function(a){var c=a.getMonth()>5?e():d(),a=b(a);return c-a!==0}}}();
    jstz.TimeZone=function(){var b=null,d=null,e=null,c=function(a){e=a[0];b=a[1];d=a[2];if(typeof jstz.olson.ambiguity_list[b]!=="undefined")for(var a=jstz.olson.ambiguity_list[b],c=a.length,f=0,g=a[0];f<c;f+=1)if(g=a[f],jstz.date_is_dst(jstz.olson.dst_start_dates[g])){b=g;break}};c.prototype={constructor:jstz.TimeZone,name:function(){return b},dst:function(){return d},offset:function(){return e}};return c}();jstz.olson={};
    jstz.olson.timezones=function(){return{"-720,0":["-12:00","Etc/GMT+12",!1],"-660,0":["-11:00","Pacific/Pago_Pago",!1],"-600,1":["-11:00","America/Adak",!0],"-660,1,s":["-11:00","Pacific/Apia",!0],"-600,0":["-10:00","Pacific/Honolulu",!1],"-570,0":["-09:30","Pacific/Marquesas",!1],"-540,0":["-09:00","Pacific/Gambier",!1],"-540,1":["-09:00","America/Anchorage",!0],"-480,1":["-08:00","America/Los_Angeles",!0],"-480,0":["-08:00","Pacific/Pitcairn",!1],"-420,0":["-07:00","America/Phoenix",!1],"-420,1":["-07:00",
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           "America/Denver",!0],"-360,0":["-06:00","America/Guatemala",!1],"-360,1":["-06:00","America/Chicago",!0],"-360,1,s":["-06:00","Pacific/Easter",!0],"-300,0":["-05:00","America/Bogota",!1],"-300,1":["-05:00","America/New_York",!0],"-270,0":["-04:30","America/Caracas",!1],"-240,1":["-04:00","America/Halifax",!0],"-240,0":["-04:00","America/Santo_Domingo",!1],"-240,1,s":["-04:00","America/Asuncion",!0],"-210,1":["-03:30","America/St_Johns",!0],"-180,1":["-03:00","America/Godthab",!0],"-180,0":["-03:00",
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          "America/Argentina/Buenos_Aires",!1],"-180,1,s":["-03:00","America/Montevideo",!0],"-120,0":["-02:00","America/Noronha",!1],"-120,1":["-02:00","Etc/GMT+2",!0],"-60,1":["-01:00","Atlantic/Azores",!0],"-60,0":["-01:00","Atlantic/Cape_Verde",!1],"0,0":["00:00","Etc/UTC",!1],"0,1":["00:00","Europe/London",!0],"60,1":["+01:00","Europe/Berlin",!0],"60,0":["+01:00","Africa/Lagos",!1],"60,1,s":["+01:00","Africa/Windhoek",!0],"120,1":["+02:00","Asia/Beirut",!0],"120,0":["+02:00","Africa/Johannesburg",!1],
        "180,1":["+03:00","Europe/Moscow",!0],"180,0":["+03:00","Asia/Baghdad",!1],"210,1":["+03:30","Asia/Tehran",!0],"240,0":["+04:00","Asia/Dubai",!1],"240,1":["+04:00","Asia/Yerevan",!0],"270,0":["+04:30","Asia/Kabul",!1],"300,1":["+05:00","Asia/Yekaterinburg",!0],"300,0":["+05:00","Asia/Karachi",!1],"330,0":["+05:30","Asia/Kolkata",!1],"345,0":["+05:45","Asia/Kathmandu",!1],"360,0":["+06:00","Asia/Dhaka",!1],"360,1":["+06:00","Asia/Omsk",!0],"390,0":["+06:30","Asia/Rangoon",!1],"420,1":["+07:00","Asia/Krasnoyarsk",
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 !0],"420,0":["+07:00","Asia/Jakarta",!1],"480,0":["+08:00","Asia/Shanghai",!1],"480,1":["+08:00","Asia/Irkutsk",!0],"525,0":["+08:45","Australia/Eucla",!0],"525,1,s":["+08:45","Australia/Eucla",!0],"540,1":["+09:00","Asia/Yakutsk",!0],"540,0":["+09:00","Asia/Tokyo",!1],"570,0":["+09:30","Australia/Darwin",!1],"570,1,s":["+09:30","Australia/Adelaide",!0],"600,0":["+10:00","Australia/Brisbane",!1],"600,1":["+10:00","Asia/Vladivostok",!0],"600,1,s":["+10:00","Australia/Sydney",!0],"630,1,s":["+10:30",
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               "Australia/Lord_Howe",!0],"660,1":["+11:00","Asia/Kamchatka",!0],"660,0":["+11:00","Pacific/Noumea",!1],"690,0":["+11:30","Pacific/Norfolk",!1],"720,1,s":["+12:00","Pacific/Auckland",!0],"720,0":["+12:00","Pacific/Tarawa",!1],"765,1,s":["+12:45","Pacific/Chatham",!0],"780,0":["+13:00","Pacific/Tongatapu",!1],"840,0":["+14:00","Pacific/Kiritimati",!1]}}();
    jstz.olson.dst_start_dates=function(){return{"America/Denver":new Date(2011,2,13,3,0,0,0),"America/Mazatlan":new Date(2011,3,3,3,0,0,0),"America/Chicago":new Date(2011,2,13,3,0,0,0),"America/Mexico_City":new Date(2011,3,3,3,0,0,0),"Atlantic/Stanley":new Date(2011,8,4,7,0,0,0),"America/Asuncion":new Date(2011,9,2,3,0,0,0),"America/Santiago":new Date(2011,9,9,3,0,0,0),"America/Campo_Grande":new Date(2011,9,16,5,0,0,0),"America/Montevideo":new Date(2011,9,2,3,0,0,0),"America/Sao_Paulo":new Date(2011,
            9,16,5,0,0,0),"America/Los_Angeles":new Date(2011,2,13,8,0,0,0),"America/Santa_Isabel":new Date(2011,3,5,8,0,0,0),"America/Havana":new Date(2011,2,13,2,0,0,0),"America/New_York":new Date(2011,2,13,7,0,0,0),"Asia/Gaza":new Date(2011,2,26,23,0,0,0),"Asia/Beirut":new Date(2011,2,27,1,0,0,0),"Europe/Minsk":new Date(2011,2,27,2,0,0,0),"Europe/Helsinki":new Date(2011,2,27,4,0,0,0),"Europe/Istanbul":new Date(2011,2,28,5,0,0,0),"Asia/Damascus":new Date(2011,3,1,2,0,0,0),"Asia/Jerusalem":new Date(2011,3,1,
            6,0,0,0),"Africa/Cairo":new Date(2010,3,30,4,0,0,0),"Asia/Yerevan":new Date(2011,2,27,4,0,0,0),"Asia/Baku":new Date(2011,2,27,8,0,0,0),"Pacific/Auckland":new Date(2011,8,26,7,0,0,0),"Pacific/Fiji":new Date(2010,11,29,23,0,0,0),"America/Halifax":new Date(2011,2,13,6,0,0,0),"America/Goose_Bay":new Date(2011,2,13,2,1,0,0),"America/Miquelon":new Date(2011,2,13,5,0,0,0),"America/Godthab":new Date(2011,2,27,1,0,0,0)}}();
    jstz.olson.ambiguity_list={"America/Denver":["America/Denver","America/Mazatlan"],"America/Chicago":["America/Chicago","America/Mexico_City"],"America/Asuncion":["Atlantic/Stanley","America/Asuncion","America/Santiago","America/Campo_Grande"],"America/Montevideo":["America/Montevideo","America/Sao_Paulo"],"Asia/Beirut":"Asia/Gaza,Asia/Beirut,Europe/Minsk,Europe/Helsinki,Europe/Istanbul,Asia/Damascus,Asia/Jerusalem,Africa/Cairo".split(","),"Asia/Yerevan":["Asia/Yerevan","Asia/Baku"],"Pacific/Auckland":["Pacific/Auckland",
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               "Pacific/Fiji"],"America/Los_Angeles":["America/Los_Angeles","America/Santa_Isabel"],"America/New_York":["America/Havana","America/New_York"],"America/Halifax":["America/Goose_Bay","America/Halifax"],"America/Godthab":["America/Miquelon","America/Godthab"]};
    function setCookie(c_name,value,exdays) {
        var exdate=new Date();
        exdate.setDate(exdate.getDate() + exdays);
        var c_value=escape(value) + ((exdays==null) ? "" : "; expires="+exdate.toUTCString());
        document.cookie=c_name + "=" + c_value;
    }

    $(document).ready(function() {
        $('#toggleLoginPanel').click(function() {
            if ($('#login').is(':visible')) {
                $('#login').slideUp();
            } else {
                $('#login').slideDown();
                $('#login_email').focus();
            }
            return false;
        });

        $('#lostPasswdLink').click(function() {
            $('#loginForm').hide();
            $('#recoverForm').show();
            $('#recoverForm').submit(function() { $(this).ajaxSubmit({success: handleRecoverCallback }); return false; });
            return false;
        });

        var timezone = jstz.determine_timezone(),
                d = new Date(),
                currentDate = d.getDate(),
                currentMonth = d.getMonth() + 1,
                currentYear = d.getFullYear();
        setCookie("timeZone", timezone.name(), 1);
        setCookie("date", currentYear + "-" + currentMonth + "-" + currentDate, 1);
    });




</script>

<script type="text/javascript"> // Tracker Google Analytics

var _gaq = _gaq || [];
_gaq.push(['_setAccount', 'UA-2318371-3']);
_gaq.push(['_trackPageview']);

(function() {
    var ga = document.createElement('script'); ga.type = 'text/javascript'; ga.async = true;
    ga.src = ('https:' == document.location.protocol ? 'https://ssl' : 'http://www') + '.google-analytics.com/ga.js';
    var s = document.getElementsByTagName('script')[0]; s.parentNode.insertBefore(ga, s);
})();

</script>



</body>
</html>