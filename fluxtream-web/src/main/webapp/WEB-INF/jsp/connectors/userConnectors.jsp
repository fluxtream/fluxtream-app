<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%><%@ taglib
	prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%><%@ page
	isELIgnored="false"%><%@ page import="com.fluxtream.*"%><%@ page
	import="com.fluxtream.domain.*"%><%@ page import="java.util.List"%><%@ page
	import="java.util.ArrayList"%>

<div class="container-fluid" style="width:530px">
	<c:forEach items="${userConnectorRows}" var="row">
		<div class="row-fluid" style="margin: 5px 0">
			<c:forEach items="${row}" var="api">
				<div class="span4">
					<span class="btn" style="float:left; text-align: left; margin: 0 5px; width: 150px"><c:out
							value="${api.prettyName}"></c:out> <span class="iconRemove"
						onclick='javascript:App.removeConnector("<c:out value="${api.connectorName}"/>")'><i
							class="icon-trash icon-large" style="float:right; padding-top: 2px"></i></span> </span>
				</div>
			</c:forEach>
		</div>
	</c:forEach>
</div>