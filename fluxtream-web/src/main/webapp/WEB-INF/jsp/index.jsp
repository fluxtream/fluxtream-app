
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<title>Fluxtream - Personal Analytics</title>
<meta name="viewport" content="width=device-width, initial-scale=1" />
<meta name="description" content="">
<meta name="author" content="">

<!-- Le HTML5 shim, for IE6-8 support of HTML elements -->
<!--[if lt IE 9]>
      <script src="http://html5shim.googlecode.com/svn/trunk/html5.js"></script>
    <![endif]-->

<!-- Le styles -->
<link rel="stylesheet" href="/static/css/bootstrap-2.0.2.min.css">
<link rel="stylesheet"
	href="/static/css/bootstrap-responsive-2.0.2.min.css">
<link rel="stylesheet" href="/${release}/css/flx.css">
<link rel="stylesheet" href="/${release}/css/font-awesome.css">

<!-- Le fav and touch icons -->
<link rel="shortcut icon" href="/favicon.ico">

</head>

<body>
	<a href="https://github.com/fluxtream/fluxtream-app"><img
		style="position: absolute; top: 0; right: 0; border: 0;"
		src="http://s3.amazonaws.com/github/ribbons/forkme_right_darkblue_121621.png"
		alt="Fork me on GitHub"></a>
		
	<div class="container-fluid">


		<div class="row-fluid">
			<div class="span4 offset8">
				<form action="signIn" class="form-inline well" method="POST">
					<fieldset>
						<div class="control-group">
							<label class="control-label" for="f_username">Username:</label>
							<div class="controls">
								<div class="input">
								<!-- <span class="add-on"><i class="icon-envelope"></i></span> --><input autocomplete="on" class="span3" onkeypress="if(event.which==13) document.forms[0].submit();" id="f_username" name="f_username" placeholder="Username" value="<%=request.getParameter("username") != null ? request.getParameter("username") : ""%>" type="text"></input>
								</div>
							</div>
						</div>
	
						<div class="control-group">
							<label class="control-label" for="f_username">Password:</label>
							<div class="controls">
								<div class="input">
								<!-- <span class="add-on"><i class="icon-key"></i></span> --><input value="" class="span3" onkeypress="if(event.which==13) document.forms[0].submit();" id="f_password" name="f_password" placeholder="Password" type="password"></input>
								</div>
							</div>
						</div>
						
						<div class="form-actions">
							<button type="submit" style="width:100px" id="signinButton" class="btn btn-primary">&raquo; Sign In</button>
							<br/><br/><a href="support/lostPassword">&raquo; Password forgotten?</a>
						</div>
					</fieldset>
				</form>

			</div>
			<div class="span1">&nbsp;</div>
		</div>
		
		<div class="row-fluid">
			<div class="span4 offset8 well">
				<h3>Sign Up!</h3>
				<a href="javascript:createAccount()" class="btn">Register</a>
			</div>
		</div>

	</div>
	<!-- /container -->
	
	<div id="modal"></div>

	<!-- Le javascript
    ================================================== -->
	<!-- Placed at the end of the document so the pages load faster -->
	<script
		src="//ajax.googleapis.com/ajax/libs/jquery/1.7.1/jquery.min.js"></script>
	<script>
		window.jQuery
				|| document
						.write('<script src="/static/js/jquery-1.7.1.min.js"><\/script>')
	</script>
	<script src="/static/js/bootstrap-2.0.2.min.js"></script>
	<script src="/${release}/js/welcome.js"></script>

</body>
</html>
