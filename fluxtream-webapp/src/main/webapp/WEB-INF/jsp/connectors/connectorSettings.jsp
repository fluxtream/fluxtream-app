<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"
%><%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"
%><%@ page isELIgnored="false"%>
<%@ include file='<%="connectors/" + request.getAttribute("api") + ".jsp"%>'%>

<a class="settingsBackButton" href="javascript:settings()">Go Back</a>
