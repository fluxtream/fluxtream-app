<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"
%><%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"
%><%@ page isELIgnored="false"%>

<h4>Please choose the user you wish to link to this account</h4><br>

<c:forEach items="${scaleUsers}" var="scaleUser">
<label class="radio inline">
    <input autocorrect="off" autocapitalize="off"
           id="withings-chosenUser" name="withings-chosenUser"
           type="radio" value="${scaleUser.publickey}">
    ${scaleUser.firstname} ${scaleUser.lastname}
</label>
</c:forEach>
<br><br>
<button class="btn" onclick="Connectors.chooseWithingsUser()">Continue</button>
