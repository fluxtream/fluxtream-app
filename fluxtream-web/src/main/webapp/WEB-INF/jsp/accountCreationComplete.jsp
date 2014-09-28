<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"
%><%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"
%><%@ page isELIgnored="false"
%><div class="modal hide">
    <script>
        if (typeof(ga)!='undefined') {ga('send', 'event', 'button', 'click', 'accountCreated', 1);}
    </script>
	<div class="modal-header">
		<a class="close" data-dismiss="modal">�</a>
		<h3>Welcome</h3>
	</div>
	<div class="modal-body">
	<p>Hold on, you are being logged on...</p>
	<form action="signIn" method="post" id="hiddenlogin" style="display:none">
		<input name="autoLoginToken"
			value="<%=request.getAttribute("autoLoginToken")%>"
		type="text"><br>
	</form>
	<script>setTimeout("document.getElementById('hiddenlogin').submit()", 100)</script>
	<a href="javascript:document.getElementById('hiddenlogin').submit()" >continue</a>
	
	</div>
	<div class="modal-footer">
	</div>
</div>