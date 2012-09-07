<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%><%@ taglib
	prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%><%@ page
	isELIgnored="false"%><%@ page import="com.fluxtream.*"%><%@ page
	import="com.fluxtream.domain.*"%><%@ page import="java.util.*"%><%@ page
	import="net.tanesha.recaptcha.*"%>
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
<div class="modal" id="registerModal">
    <div class="modal-header">
        <button type="button" class="close" data-dismiss="modal">&times;</button>
        <h3 id="myModalLabel">Register</h3>
    </div>
    <form class="form-horizontal" action="javascript:void(0)">
        <div class="modal-body">
            <div class="control-group">
                <%
                    if (required.contains("email")) {
                %>
                <span class="error">E-mail is required</span>
                <% } else if (errors.contains("userExists")) { %>
                <span class="error">This e-mail address is already used</span>
                <% } %>
                <label class="control-label" for="email">Email</label>
                <div class="controls">
                    <input type="email" value="<%=email%>" autocorrect="off" autocapitalize="off" id="email" name="email" placeholder="ex: example@example.com">
                </div>
            </div>
            <div class="control-group">
                <%
                    if (required.contains("username")) {
                %>
                <span class="error">Username is required</span>
                <% } else if (errors.contains("usernameTaken")) { %>
                <span class="error">This username is already taken</span>
                <% } %>
                <label class="control-label" for="username">User Name</label>
                <div class="controls">
                    <input type="text" value="<%=username%>" autocorrect="off" autocapitalize="off" id="username" name="username" placeholder="ex: Johndoe">
                </div>
            </div>
            <div class="control-group">
                <%
                    if (required.contains("firstname")) {
                %>
                <span class="error">First name is required</span>
                <% } %>
                <label class="control-label" for="firstname">First Name</label>
                <div class="controls">
                    <input type="text" value="<%=firstname%>" autocapitalize="off" id="firstname" name="firstname" placeholder="ex: John">
                </div>
            </div>
            <div class="control-group">
                <%
                    if (required.contains("lastname")) {
                %>
                <span class="error">Last name is required</span>
                <% } %>
                <label class="control-label" for="lastname">Last Name</label>
                <div class="controls">
                    <input type="text" value="<%=lastname%>" autocapitalize="off" id="lastname" name="lastname" placeholder="ex: Doe">
                </div>
            </div>
            <div class="control-group">
                <%
                    if (required.contains("password")) {
                %>
                <span class="error">Password is required</span>
                <% } else if (errors.contains("passwordsDontMatch")) { %>
                <span class="error">Passwords don't match</span>
                <% } else if (errors.contains("passwordTooShort")) { %>
                <span class="error">Password must be at least 8 characters long</span>
                <% } %>
                <label class="control-label" for="password">Password</label>
                <div class="controls">
                    <input type="password" name="password1" id="password" placeholder="Password">
                </div>
            </div>
            <div class="control-group">
                <%
                    if (required.contains("password2")) {
                %>
                <span class="error">Password verification is required</span>
                <% } %>
                <label class="control-label" for="password2">re-type Password</label>
                <div class="controls">
                    <input type="password" name="password2" id="password2" placeholder="Password">
                </div>
            </div>
            <div class="control-group">
                <div class="controls">
                    <button type="submit" id="submitCreateAccountForm" class="btn btn-primary">Register</button>
                </div>
            </div>
        </div>
    </form>
</div>
