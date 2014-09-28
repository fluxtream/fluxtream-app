<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%><%@ taglib
        prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%><%@ page
        isELIgnored="false"%><%@ page import="org.fluxtream.*"%><%@ page
        import="org.fluxtream.core.domain.*"%><%@ page import="java.util.*"%><%@ page
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

    boolean isDeveloperAccount = (Boolean) request.getAttribute("developerAccount");
%>
<div class="modal" id="registerModal">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" aria-hidden="true" data-dismiss="modal">&times;</button>
                <h4 class="modal-title" id="myModalLabel">Register</h4>
            </div>
            <div class="modal-body">
                <%--<div class="row">--%>
                <%--<div class="col-md-offset-4 col-md-8" style="margin-bottom:1em;">--%>
                <%--<a role="button" style="display:inline-block;cursor:pointer" id="fbLogin" onclick="fbLogin();"></a>--%>
                <%--</div>--%>
                <%--</div>--%>

                <form role="form" class="form-horizontal" action="javascript:void(0)">
                    <div class="form-group">
                        <label for="email" class="col-sm-4 control-label">Email</label>
                        <div class="col-sm-8">
                            <input class="form-control" type="email" value="<%=email%>" autocorrect="off" autocapitalize="off" id="email" name="email" placeholder="ex: example@example.com">
                            <%
                                if (required.contains("email")) {
                            %>
                            <span class="help-block error">E-mail is required</span>
                            <% } else if (errors.contains("userExists")) { %>
                            <span class="help-block error">This e-mail address is already used</span>
                            <% } %>
                        </div>
                    </div>
                    <div class="form-group">
                        <label for="username" class="col-sm-4 control-label">User Name</label>
                        <div class="col-sm-8">
                            <input class="form-control" type="text" value="<%=username%>" autocorrect="off" autocapitalize="off" id="username" name="username" placeholder="ex: Johndoe">
                            <%
                                if (required.contains("username")) {
                            %>
                            <span class="help-block error">Username is required</span>
                            <% } else if (errors.contains("usernameTaken")) { %>
                            <span class="help-block error">This username is already taken</span>
                            <% } %>
                        </div>
                    </div>
                    <div class="form-group">
                        <label for="firstname" class="col-sm-4 control-label">First Name</label>
                        <div class="col-sm-8">
                            <input class="form-control" type="text" value="<%=firstname%>" autocapitalize="off" id="firstname" name="firstname" placeholder="ex: John">
                            <%
                                if (required.contains("firstname")) {
                            %>
                            <span class="help-block error">First name is required</span>
                            <% } %>
                        </div>
                    </div>
                    <div class="form-group">
                        <label for="lastname" class="col-sm-4 control-label">Last Name</label>
                        <div class="col-sm-8">
                            <input class="form-control" type="text" value="<%=lastname%>" autocapitalize="off" id="lastname" name="lastname" placeholder="ex: Doe">
                            <%
                                if (required.contains("lastname")) {
                            %>
                            <span class="help-block error">Last name is required</span>
                            <% } %>
                        </div>
                    </div>
                    <div class="form-group">
                        <label for="password" class="col-sm-4 control-label">Password</label>
                        <div class="col-sm-8">
                            <input class="form-control" type="password" name="password1" id="password" placeholder="Password">
                            <%
                                if (required.contains("password")) {
                            %>
                            <span class="help-block error">Password is required</span>
                            <% } else if (errors.contains("passwordsDontMatch")) { %>
                            <span class="help-block error">Passwords don't match</span>
                            <% } else if (errors.contains("passwordTooShort")) { %>
                            <span class="help-block error">Password must be at least 8 characters long</span>
                            <% } %>
                        </div>
                    </div>
                    <div class="form-group">
                        <label for="password2" class="col-sm-4 control-label">re-type Password</label>
                        <div class="col-sm-8">
                            <input class="form-control" type="password" name="password2" id="password2" placeholder="Password">
                            <%
                                if (required.contains("password2")) {
                            %>
                            <span class="help-block error">Verification is required</span>
                            <% } %>
                        </div>
                    </div>
                    <div class="form-group">
                        <div class="col-sm-offset-4 col-sm-2">
                            <button type="submit"
                                    onclick="if (typeof(ga)!='undefined') {ga('send', 'event', 'button', 'click', 'register', 1);}"
                                    id="submitCreateAccountForm" class="login_btn btn btn-primary">Register</button>
                        </div>
                        <div class="col-sm-6">
                            <div style="display:inline-block;height:37px;line-height:37px;vertical-align: top;margin-right: 15px;"></a>
                        </div>
                    </div>
                </form>
            </div>
        </div>
    </div>
</div>
