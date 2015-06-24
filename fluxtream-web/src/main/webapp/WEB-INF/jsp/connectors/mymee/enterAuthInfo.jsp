<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"
%><%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"
%><%@ page isELIgnored="false"
%><%@ page import="java.util.*" %><%
	List<String> required = new ArrayList<String>();
	if (request.getAttribute("required")!=null) required = (ArrayList<String>) request.getAttribute("required");

	String username = "";
	if (request.getAttribute("username")!=null) username = (String) request.getAttribute("username");

    String activationCode = "";
    if (request.getAttribute("activationCode")!=null) activationCode = (String) request.getAttribute("activationCode");

	String errorMessage = null;
	if (request.getAttribute("errorMessage")!=null) errorMessage = (String) request.getAttribute("errorMessage");
%>


<form class="form-horizontal" action="javascript:void(0)">

    <% if (errorMessage!=null) { System.out.println(errorMessage); %>
    <div class="alert alert-error"><%=errorMessage%></div>
    <% } %>

    <div class="control-group">
        <label class="control-label" for="mymee-username">Username</label>
        <div class="controls">
            <input autocorrect="off" autocapitalize="off" type="text"
                   onkeypress="if(event.which==13) Connectors.submitMymeeAuthInfo()"
                   class="focushere" name="mymee-username" id="mymee-username" value="<%=username%>">
            <% if (required.contains("username")) { %>
            <span class="help-inline">username is required</span>
            <% } %>
        </div>
    </div>

    <div class="control-group">
        <label class="control-label" for="mymee-password">Password</label>
        <div class="controls">
            <input autocorrect="off" autocapitalize="off"
                   onkeypress="if(event.which==13) Connectors.submitMymeeAuthInfo()"
                   name="mymee-password" id="mymee-password" type="password" value="">
            <% if (required.contains("password")) { %>
            <span class="help-inline">password is required</span>
            <% } %>
        </div>
    </div>

    <div class="control-group">
        <label class="control-label" for="mymee-activationCode">Activation Code</label>
        <div class="controls">
            <input autocorrect="off"  type="text" autocapitalize="off"
                   onkeypress="if(event.which==13) Connectors.submitMymeeAuthInfo()"
                   name="mymee-activationCode" id="mymee-activationCode"
                   value="<%=activationCode%>">
            <% if (required.contains("activationCode")) { %>
            <span class="help-inline">Activation Code is required</span>
            <% } %>
        </div>
    </div>

    <div class="control-group">
        <div class="controls">
            <button  onclick="Connectors.submitMymeeAuthInfo()" type="submit" id="submitMymeeInfo" class="btn">Send</button>
        </div>
    </div>
</form>
<%--<div class="well">--%>
    <%--<form action="javascript:void(0)">--%>
        <%--&lt;%&ndash;<p>(legacy users only)</p>&ndash;%&gt;--%>
        <%--<p>Please enter your Mymee fetch URL (which you received when you created your Mymee account)</p>--%>

        <%--<fieldset>--%>
            <%--<label>Fetch URL</label>--%>
            <%--<input autocorrect="off" autocapitalize="off" type="text"--%>
                   <%--onkeypress="if(event.which==13) Connectors.submitMymeeFetchURL()"--%>
                   <%--name="mymee-fetchURL" id="mymee-fetchURL" style="width:450px"> <img style="display:none" class="mymee-loading loading-animation" src="/static/img/loading.gif"/>--%>
            <%--<span class="help-block"><% if (required.contains("fetchURL")) { %>Fetch URL is required<% } else if (errorMessage!=null) {%><%=errorMessage%><%}%></span>--%>
            <%--<button onclick="Connectors.submitMymeeFetchURL()" type="submit" class="mymee-send btn">Send</button>--%>
        <%--</fieldset>--%>
    <%--</form>--%>
<%--</div>--%>
