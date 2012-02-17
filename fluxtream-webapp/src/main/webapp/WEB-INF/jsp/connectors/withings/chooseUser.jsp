<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"
%><%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"
%><%@ page isELIgnored="false"%><p>Please choose the user you wish to link to this account</p>

<c:forEach items="${scaleUsers}" var="scaleUser">
	<input id="withings-chosenUser" name="withings-chosenUser" type="radio"
		value="${scaleUser.publickey}">${scaleUser.firstname} ${scaleUser.lastname}
</c:forEach>

<br/><button onclick="chooseWithingsUser()">Send</button>
