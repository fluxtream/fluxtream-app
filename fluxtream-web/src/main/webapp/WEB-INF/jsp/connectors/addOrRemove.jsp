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

<div id="modal" class="modal hide fade" style="width: 650px">

	<div class="modal-header">
		<a class="close" data-dismiss="modal">&times;</a>
		<h3>Add Connectors</h3>
	</div>

	<div class="modal-body">
		<div class="addConnectorsMain">

			<div id="availableConnectors">
				<% if (availableConnectorRows.size() > 0) { %>
				<%@ include file="availableConnectors.jsp"%>
				<% } %>
			</div>

		</div>
	</div>
	<div class="modal-footer">
		<a class="btn" href="javascript:App.closeModal();">Close</a>
	</div>
</div>

