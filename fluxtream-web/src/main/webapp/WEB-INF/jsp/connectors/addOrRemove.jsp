<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%><%@ taglib
	prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%><%@ page
	isELIgnored="false"%><%@ page import="com.fluxtream.*"%><%@ page
	import="com.fluxtream.domain.*"%><%@ page import="java.util.List"%><%@ page
	import="java.util.ArrayList"%><%@ page import="java.lang.Object"%>
<jsp:useBean id="userApis" type="java.util.List" scope="request" />
<jsp:useBean id="availableConnectorRows" type="java.util.List"
	scope="request" />
<jsp:useBean id="userConnectorRows" type="java.util.List"
	scope="request" />

<div class="modal hide fade" style="width: 650px">

	<div class="modal-header">
		<a class="close" data-dismiss="modal">×</a>
		<h3>Add and remove Connectors</h3>
	</div>

	<div class="modal-body">
		<div class="addConnectorsMain">

			<%
				if (userApis.size() > 0) {
			%><h3>Installed Connectors</h3>

			<div id="userConnectors">
				<%@ include file="userConnectors.jsp"%>
			</div>
			<hr class="space">
			<% }
			   if (availableConnectorRows.size() > 0) {
			%><h3>Add other Connectors</h3>
			<% } %>

			<div id="availableConnectors">
				<% if (availableConnectorRows.size() > 0) { %>
				<%@ include file="availableConnectors.jsp"%>
				<% } %>
			</div>

		</div>
	</div>
	<div class="modal-footer">
		<a class="btn" href="javascript:App.dialog.modal('hide');">Close</a>
	</div>
</div>

