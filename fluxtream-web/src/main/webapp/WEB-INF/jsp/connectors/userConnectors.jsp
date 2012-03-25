<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"
%><%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"
%><%@ page isELIgnored="false"
%><%@ page import="com.fluxtream.*"
%><%@ page import="com.fluxtream.domain.*"
%><%@ page import="java.util.List"
%><%@ page import="java.util.ArrayList" %>

<c:forEach items="${userConnectorRows}" var="row">
	<div class="thirdwidth installedConnector">
		<c:forEach items="${row}" var="api">
		<article>
			<div role="button" class="v-button v-button- removeConnectorButton" tabindex="0"> 
				<span class="v-button-wrap "> 
					<span class="btn"><c:out value="${api.prettyName}"></c:out>
						<span class="iconRemove" onclick='javascript:App.removeConnector("<c:out value="${api.connectorName}"/>")'><i class="icon-trash"></i></span>
<%-- 						<div class="iconSettings" onclick='javascript:connectorSettings("<c:out value="${api.connectorName}"/>")'></div> --%>
					</span> 
				</span>
			</div> 
		</article>
		</c:forEach>
	</div>
</c:forEach>
