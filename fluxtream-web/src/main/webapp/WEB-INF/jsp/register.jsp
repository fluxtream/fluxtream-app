<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%><%@ taglib
	prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ page
	isELIgnored="false"%><%@ page import="com.fluxtream.*"%><%@ page
	import="com.fluxtream.domain.*"%><%@ page import="java.util.*"%>
<%
	List<String> errors = new ArrayList<String>();
	if (request.getAttribute("errors") != null)
		errors = (ArrayList<String>) request.getAttribute("errors");

	List<String> required = new ArrayList<String>();
	if (request.getAttribute("required") != null)
		required = (ArrayList<String>) request.getAttribute("required");

	String invitationCode = "";
	if (request.getAttribute("invitationCode") != null)
		invitationCode = (String) request
				.getAttribute("invitationCode");

	String email = "";
	if (request.getAttribute("email") != null)
		email = (String) request.getAttribute("email");

	String username = "";
	if (request.getAttribute("username") != null)
		username = (String) request.getAttribute("username");

	String firstname = "";
	if (request.getAttribute("firstname") != null)
		firstname = (String) request.getAttribute("firstname");

	String lastname = "";
	if (request.getAttribute("lastname") != null)
		lastname = (String) request.getAttribute("lastname");
%>
<div class="modal hide">
    <form action="createAccount" method="POST">


	<div class="modal-body">
		<table class="modalFormNewAccount">
            <tr>
				<td valign="top"><label>E-mail</label></td>
				<td><input autocorrect="off" autocapitalize="off" id="email" name="email" value="<%=email%>"
					type="text"></input> <%
 	if (required.contains("email")) {
 %>
					<div class="formErrorCont">
						<div class="icon"></div>
						<span class="formError">Email is required</span>
					</div> <%
 	} else if (errors.contains("userExists")) {
 %>
					<div class="formErrorCont">
						<div class="icon"></div>
						<span class="formError">This email address is already used</span>
					</div> <%
 	}
 %></td>
			</tr>
			<tr>
				<td valign="top"><label>Username</label></td>
				<td><input autocorrect="off" autocapitalize="off" id="username" name="username" value="<%=username%>"></input>
					<br> <%
 	if (required.contains("username")) {
 %>
					<div class="formErrorCont">
						<div class="icon"></div>
						<span class="formError">Username is required</span>
					</div> <%
 	} else if (errors.contains("usernameTaken")) {
 %>
					<div class="formErrorCont">
						<div class="icon"></div>
						<span class="formError username">This username is already
							taken</span>
					</div> <%
 	}
 %></td>
			</tr>
			<tr>
				<td valign="top"><label>First name</label></td>
				<td><input id="firstname" name="firstname"
					value="<%=firstname%>"></input></td>
			</tr>

			<tr>
				<td valign="top"><label>Last name</label></td>
				<td><input id="lastname" name="lastname" value="<%=lastname%>"></input></td>
			</tr>

			<tr>
				<td valign="top"><label>Password</label></td>
				<td><input type="password" name="password1" id="password1"></input><br>
					<%
						if (required.contains("password")) {
					%>
					<div class="formErrorCont">
						<div class="icon"></div>
						<span class="formError">Password is required</span>
					</div> <%
 	} else if (errors.contains("passwordTooShort")) {
 %>
					<div class="formErrorCont">
						<div class="icon"></div>
						<span class="formError">Password must be at least 8
							characters long</span>
					</div> <%
 	} else if (errors.contains("passwordsDontMatch")) {
 %>
					<div class="formErrorCont">
						<div class="icon"></div>
						<span class="formError">Passwords don't match</span>
					</div> <%
 	}
 %></td>
			</tr>
			<tr>
				<td valign="top"><label>Re-type password</label></td>
				<td valign="top"><input type="password" name="password2"
					id="password2"></input><br> <%
 	if (required.contains("password2")) {
 %>
					<div class="formErrorCont">
						<div class="icon"></div>
						<span class="formError">Password verification is required</span>
					</div> <%
 	}
 %></td>
			</tr>
			<tr>
				<td colspan="3" id="recaptcha"></td>
			</tr>
			<%
				if (errors.contains("wrongCaptcha")) {
			%>
			<tr>
				<td colspan="3">Wrong Captcha answer. Are you human?</td>
			</tr>
			<%
				}
			%>
		</table>
	</div>
	<div class="modal-footer">
		<input type="submit" class="btn btn-primary" value="Create Account">
	</div>
    </form>
</div>