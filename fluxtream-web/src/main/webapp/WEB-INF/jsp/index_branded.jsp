<!DOCTYPE html>
<%
    Boolean tracker = (Boolean)request.getAttribute("tracker");
    Boolean supportsFBLogin = (Boolean)request.getAttribute("supportsFBLogin");
%>
<html>
<head>
    <%--<link href="/static/css/bootstrap-2.3.2.min.css" rel="stylesheet">--%>
    <%--<link href="/static/css/bootstrap-responsive-2.3.2.min.css" rel="stylesheet">--%>
    <link href="${release}/css/index.css" rel="stylesheet" type="text/css"/>
    <link href='//fonts.googleapis.com/css?family=Open+Sans:400,700,600' rel='stylesheet' type='text/css'>
    <script src="//ajax.googleapis.com/ajax/libs/jquery/1.7.2/jquery.min.js"></script>
    <script type="text/javascript" src="/${release}/assets-home/js/welcome.js"></script>
    <!-- fav icons -->
    <link rel="shortcut icon" href="/favicon.ico">
</head>
<body>
<div id="header">
    <a href="#"><div class="logo"></div></a>
    <div class="login_container">
        <form method="post" action="signIn" id="loginForm">
        <table>
            <tr>
                <td><label class="username_txt" for="f_username">Username:</label></td>
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
        <p id="recoverPasswordFeedback" style="display:none;" class="sysInfo">A confirmation link has been emailed to you_XXX</p>
        </form>
    </div>
</div>
<div id="introBar">
    <div id="intro">
        <table>
            <tr>
                <td rowspan="2">
                    <video width="432" height="324" class="intro_video" controls>
                        <source src="${release}/videos/Fluxtream.mp4" type="video/mp4">
                        Your browser does not support the video tag.
                    </video>
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
                    </div><button class="register_btn">Register</button>
                </td>
            </tr>
        </table>
    </div>
</div>
<div id="content">
    <table>
        <tr class="sc_highlight">
            <td><a href="images/s1.png"><img src="images/s1.png" class="screenshot_img_1"/></a></td>
            <td><h1 class="screenshot_txt_1">Take control of your data</h1>
                <div class="screenshot_txt_2">
                    Choose from our growing list of supported devices and APIs. Importing your data is easy.
                </div></td>
            <td rowspan="4"><div class="connector_img"></div></td>
        </tr>
        <tr class="sc_highlight">
            <td><a href="images/s2.png"><img src="images/s2.png" class="screenshot_img_2"/></a></td>
            <td><h1 class="screenshot_txt_1">The whole is more than the sum of its parts</h1>
                <div class="screenshot_txt_2">
                    As you keep adding more different datatypes, you will picture your life unfold.
                </div></td>
        </tr>
        <tr class="sc_highlight">
            <td><a href="images/s3.png"><img src="images/s3.png" class="screenshot_img_3"/></a></td>
            <td><h1 class="screenshot_txt_1">Identify different patterns at different scales</h1>
                <div class="screenshot_txt_2">
                    Long term and short term trends sometimes tell different stories.
                </div></td>
        </tr>
        <tr class="sc_highlight">
            <td><a href="images/s4.png"><img src="images/s4.png" class="screenshot_img_4"/></a></td>
            <td><h1 class="screenshot_txt_1">Explore your daily life, be curious</h1>
                <div class="screenshot_txt_2">
                    Awareness is a muscle. Fluxtream gives you all the tools you need to exercise it.
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
                <a href="#" class="guideLink">Quick start guide</a>
            </li>
            <li>
                <a href="#" class="policyLink">Privacy policy and terms of use</a>
            </li>
            <li>
                <a href="#" class="developerLink">Developers: join our open-source team!</a>
            </li>
            <li>
                <a href="#" class="learnMoreLink">Learn more</a>
            </li>
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

<div id="registerModalWrapper">
</div>

<% if (tracker) { %>
<jsp:include page="tracker.jsp"/>
<% } %>

</body>
</html>