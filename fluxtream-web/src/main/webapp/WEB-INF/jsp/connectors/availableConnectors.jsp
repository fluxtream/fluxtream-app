<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"
%><%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"
%><%@ page isELIgnored="false"
%><%@ page import="org.fluxtream.core.*"
%><%@ page import="org.fluxtream.core.domain.*"
%><%@ page import="java.util.List"
%><%@ page import="java.util.ArrayList" %>
<jsp:useBean id="showing" type="java.lang.String" scope="request"/>
<jsp:useBean id="pages" type="java.lang.Integer" scope="request"/>
<jsp:useBean id="currentPage" type="java.lang.Integer" scope="request"/>
<jsp:useBean id="env" type="org.fluxtream.core.Configuration" scope="request"/>
<div class="container-fluid" style="width:530px">
<% 
	List<List<ConnectorInfo>> rows = (List<List<ConnectorInfo>>) request.getAttribute("availableConnectorRows");
	for(List<ConnectorInfo> row: rows) {
%><div class="row-fluid">
		<% for (ConnectorInfo connector : row) {
			String connectorImage = connector.image;
			String connectorName = connector.name;
			String connectUrl = connector.connectUrl;
			String connectorText = connector.text;
		%><div class="span4"><%  if (connectUrl.startsWith("ajax:")) {
						connectUrl = connectUrl.substring(6);
						connectUrl = "ajax:" + env.get("homeBaseUrl") + connectUrl;
					}
				%><!-- <h5><%= connectorName %></h5>  -->
				<a  href="javascript:App.addConnector('<%=connectUrl%>')">
					<img src="<%=connectorImage%>" alt="" width=178 Height=120>
				</a>
				<p><%=connectorText%></p>
<%-- 				<div onclick="javascript:App.addConnector('<%=connectUrl%>')" role="button" class="v-button v-button- addConnectorButton" tabindex="0">  --%>
<!-- 					<span class="btn">Add</span>  -->
<!-- 				</div>  -->
		</div>
			
		<% } %>
	</div>
<% } %><% if (pages>1) { %>
	<div class="pagination">
		<ul>
	<% for (int i=0; i<pages; i++) { %>
		<li><a onclick="javascript:App.showConnectorsPage(<%=i %>)" <%if (i==currentPage) out.print("class=\"active\" style=\"color:gray\""); else out.print(" style=\"cursor:pointer\""); %>><%=(i+1) %></a>
		</li>
	<% } %>
	</ul>
	</div>
	<span class="paginaText">${showing}</span>
<% } %>
</div>
