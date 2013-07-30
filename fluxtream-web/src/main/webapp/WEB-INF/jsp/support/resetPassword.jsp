<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"
%><%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"
%><%@ page isELIgnored="false"
%><%@ page import="java.util.*"%><%
	List<String> errors = new ArrayList<String>();
	if (request.getAttribute("errors") != null)
		errors = (ArrayList<String>) request.getAttribute("errors");

	List<String> required = new ArrayList<String>();
	if (request.getAttribute("required") != null)
		required = (ArrayList<String>) request.getAttribute("required");

	String token = (String) request.getAttribute("token");
%><html>
<body>
<style>
<!--
body {
	background-color: #e8e8e8;
	margin: 0;
	padding: 0;
}

p {
	padding: 5px 0;
}

a img {
	border: none;
}

a {
	border: none;
}

.body {
	background-color: #ffffff;
}

td.content {
	padding: 14px 0 0 14px;
	text-align: left;
}

.header {
	
}

.header h1 {
	font-family: Georgia;
	font-size: 32px;
	font-weight: normal;
	color: #bfbfbf;
	display: inline;
	text-align: left;
	padding-left: 22px;
}

.content p {
	font-family: 'Lucida Grande';
	font-size: 12px;
	color: #333333;
	margin: 0 0 10px 0;
	text-align: left;
}

.content a {
	font-family: Georgia;
	font-size: 13px;
	color: #53A3DB;
	font-style: italic;
}

.content img.inline {
	border: 1px solid #dedede;
	padding: 4px;
}

.footer p {
	font-family: 'Lucida Grande';
	font-size: 10px;
	color: #8d8d8d;
	margin: 5px 0 0 25px;
	text-align: left;
}
-->
</style>
<p>&nbsp;</p>
<table cellspacing="0" cellpadding="0" width="100%" bgcolor="#e8e8e8">
	<tbody>
		<tr>
			<td align="center">
				<table width="590" border="0" cellspacing="0" cellpadding="0">
					<tbody>
						<tr>
							<td class="header" height="47" align="left">
								<table border="0" cellspacing="0" cellpadding="0" width="500">
									<tbody>
										<tr>
											<td style="padding-left: 42px;"><img width="94" height="20" alt="Fluxtream" src="/${release}/images/header-logo-v4.png">
											</td>
										</tr>
									</tbody>
								</table>
							</td>
						</tr>
					</tbody>
				</table>
			</td>
		</tr>
		<tr>
			<td align="center">
				<form action="doResetPassword">
				<table class="body" cellspacing="10" cellpadding="0" width="500"
					style="border: solid 1px #DDD; -moz-border-radius: =10px; -webkit-border-radius: 10px; border-radius: 10px;">
					<tbody>
					    <% if (errors.contains("invalidToken")) { %>
						<tr>
							<td colspan="3">Your token is invalid. This is strange. Please request a new password again.</td>
						</tr>
						<tr>
							<td colspan="3"><a href="/">Go Back</a></td>
						</tr>
					    <% } else if (errors.contains("tokenExpired")) {%>
						<tr>
							<td colspan="3">Your token has expired. Please request a new password again.</td>
						</tr>
						<tr>
							<td colspan="3"><a href="/">Go Back</a></td>
						</tr>
					    <% } else { %>
							<tr>
								<td colspan="3">Hi <%=request.getAttribute("username") %>,</td>
							</tr>
							<tr>
								<td colspan="3">Please enter your new password below:</td>
							</tr>
							<tr>
								<td style="width:140px">Password:</td>
								<td style="width:250px"><input style="width:220px" class="focushere" type="password" name="password" id="password"></input></td>
								<td>
									<% if (required.contains("password")) { %>
									<span class="formError">* Password is required</span>
									<% } else if (errors.contains("passwordTooShort")) { %>
									<span class="formError">* Your password must be at least 8 characters long</span>
									<% } else if (errors.contains("invalidToken")) { %>
									<span class="formError">* Your token is not valid. This is strange. Please try again.</span>
									<% } %>
								</td>
							</tr>
							<tr>
								<td>Password (repeat):</td>
								<td><input style="width:220px" type="password" name="password2" id="password2"></input></td>
								<td>
									<% if (required.contains("password2")) { %>
									<span class="formError">* Please enter your new password again</span>
									<% } else if (errors.contains("passwordsDontMatch")) { %>
									<span class="formError">* You entered two different passwords </span>
									<% } %>
								</td>
							</tr>
							<tr><td colspan="3"><input type="submit" value="Reset my password"></input></td></tr>
						<% } %>
					</tbody>
				</table>
				<input type="hidden" name="token" value="<%=token%>"/>
				</form>
			</td>
		</tr>
		<tr>
			<td align="center">
				<table class="footer" cellspacing="0" cellpadding="0" width="600">
					<tbody>
						<tr>
							<td valign="top">
                                <% int currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR); String until = ""; if (currentYear>2011) until = " - " + currentYear; %>
								<p style="padding-left: 35px;">Fluxtream - All right
									reserved &copy; 2010<%=until %></p>
							</td>
						</tr>
					</tbody>
				</table>
			</td>
		</tr>
	</tbody>
</table>

<script src="/js/jquery1.6.1.min.js"></script>
<script>
	$(".focushere").focus();
</script>
</body>
</html>