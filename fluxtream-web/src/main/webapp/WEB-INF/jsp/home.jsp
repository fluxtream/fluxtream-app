
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="utf-8">
    <title>Fluxtream - Personal Analytics</title>
    <meta name="description" content="">
    <meta name="author" content="">

    <!-- Le HTML5 shim, for IE6-8 support of HTML elements -->
    <!--[if lt IE 9]>
      <script src="http://html5shim.googlecode.com/svn/trunk/html5.js"></script>
    <![endif]-->

    <!-- Le styles -->
	<link rel="stylesheet/less" href="/${release}/css/flx.less">
	<link rel="stylesheet/less" href="/${release}/css/qtip/jquery.qtip.css">
	<link rel="stylesheet/less" href="/static/less/bootstrap.less">
	<link rel="stylesheet/less" href="/static/less/responsive.less">
	<script src="/static/js/less-1.2.1.min.js"></script>

    <!-- Le fav and touch icons -->
    <link rel="shortcut icon" href="/favicon.ico">
    
  </head>

  <body>

    <div class="navbar navbar-fixed-top">
      <div class="navbar-inner">
        <div class="container-fluid">
          <a class="btn btn-navbar" data-toggle="collapse" data-target=".nav-collapse">
            <span class="icon-bar"></span>
            <span class="icon-bar"></span>
            <span class="icon-bar"></span>
          </a>
          <a class="brand" href="#">Fluxtream</a>
          <div class="nav-collapse">
            <ul class="nav pull-right">
              <li><a href="/logout">Logout</a></li>
            </ul>
          </div><!--/.nav-collapse -->
        </div>
      </div>
    </div>

    <div class="container-fluid">

	<div class="row-fluid">
		<div class="span12">
			<a class="btn menuTodayButton">Today</a>&nbsp;
			<a class="btn menuPrevButton">&lt;</a>
			<span id="currentTimespanLabel"></span>
			<a class="btn menuNextButton">&gt;</a>
		</div>
	</div>
	<br/>
	
	<div class="row-fluid">
		<div class="span12 editor">
			<input class="diaryTitle" type="text" placeholder="This day..."></input>
		</div>
	</div>

	<div id="widgets" class="row-fluid">
	</div>

    </div> <!-- /container -->

    <!-- Le javascript
    ================================================== -->
    <!-- Placed at the end of the document so the pages load faster -->
	<script src="//ajax.googleapis.com/ajax/libs/jquery/1.7.1/jquery.min.js"></script>
	<script>window.jQuery || document.write('<script src="/static/js/jquery-1.7.1.min.js"><\/script>')</script>
    <script src="/static/js/bootstrap-2.0.min.js"></script>
	<script src="/${release}/js/libs/underscore-1.2.4.js"></script>
	<script src="/${release}/js/libs/backbone-0.5.3.js"></script>
	<script data-main="/${release}/js/main.js" src="/static/js/require.js"></script>

  </body>
</html>
