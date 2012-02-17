<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"
%><%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"
%><%@ page isELIgnored="false"
%><%@ page import="com.fluxtream.*" 
%><%@ page import="com.fluxtream.domain.*" 
%><%@ page import="java.util.List"
%><%@ page import="java.util.ArrayList"
%><%@ page import="java.lang.Object" %>
<jsp:useBean id="userApis" type="java.util.List" scope="request"/>
<jsp:useBean id="availableConnectorRows" type="java.util.List" scope="request"/>
<jsp:useBean id="userConnectorRows" type="java.util.List" scope="request"/>

<div id="fb-Settings-add-remove"> 
	
	
	<div class="addConnectorsMain">
	<h2>Add and remove Connectors</h2> 
		<% if (userApis.size()>0) { %><h3>Installed Connectors</h3>
		
		<div id="userConnectors">
			<%@ include file="userConnectors.jsp" %>
		</div>
		<hr class="space"> 
		<% } if (availableConnectorRows.size()>0) { %><h3>Add other Connectors</h3> 
		
		<div id="availableConnectors">
			<%@ include file="availableConnectors.jsp" %>
		</div><% } %>
		 
	</div> 
</div> 
 
