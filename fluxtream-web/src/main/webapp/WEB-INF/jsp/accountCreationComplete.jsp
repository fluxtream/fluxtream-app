<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"
%><%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"
%><%@ page isELIgnored="false"
%><%@ page import="com.fluxtream.*"%><div class="modal hide">
	<div class="modal-header">
		<a class="close" data-dismiss="modal">×</a>
		<h3>Welcome</h3>
	</div>
	<div class="modal-body">
	<p>Hold on, you are being logged on...</p>
	<form action="signIn" method="post" id="hiddenlogin" style="display:none">
		<input name="f_username"
			value="<%=request.getAttribute("username")%>"
		type="text"><br>
		<input value="<%=request.getAttribute("password")%>" name="f_password">
	</form>
	<script>setTimeout("document.getElementById('hiddenlogin').submit()", 2000)</script>
	<a href="javascript:document.getElementById('hiddenlogin').submit()" >continue</a>
	
	</div>
	<div class="modal-footer">
	</div>
</div>