<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"
%><%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"
%><%@ page isELIgnored="false"
%><%@ page import="com.fluxtream.*"
%><%@ page import="com.fluxtream.domain.*"
%><%@ page import="java.util.List"
%><%@ page import="java.util.ArrayList" %>
<jsp:useBean id="showing" type="java.lang.String" scope="request"/>
<jsp:useBean id="pages" type="java.lang.Integer" scope="request"/>
<jsp:useBean id="currentPage" type="java.lang.Integer" scope="request"/>
<jsp:useBean id="env" type="com.fluxtream.Configuration" scope="request"/>
<% 
	List<List<ConnectorInfo>> rows = (List<List<ConnectorInfo>>) request.getAttribute("availableConnectorRows");
	for(List<ConnectorInfo> row: rows) {
%><div class="thirdwidth">
		<% for (ConnectorInfo connector : row) {
			String connectorImage = connector.image;
			String connectorName = connector.name;
			String connectUrl = connector.connectUrl;
			String connectorText = connector.text;
		%><article><%  if (connectUrl.startsWith("ajax:")) {
						connectUrl = connectUrl.substring(6);
						connectUrl = "ajax:" + env.get("homeBaseUrl") + connectUrl;
					}
				%><h5><%= connectorName %></h5> 
				<a  href="javascript:App.addConnector('<%=connectUrl%>')">
					<img src="<%=connectorImage%>" alt="" width=178 Height=120>
				</a>
				<p><%=connectorText%></p> 
				<div onclick="javascript:App.addConnector('<%=connectUrl%>')" role="button" class="v-button v-button- addConnectorButton" tabindex="0"> 
					<span class="btn">Add</span> 
				</div> 
			</article> 
			
		<% } %>
	</div>
<% } %><% if (pages>1) { %>
<div class="clear floatR pagination">
	<span class="paginaText">${showing}</span>
	<% for (int i=0; i<pages; i++) { %>
		<div onclick="javascript:App.showConnectorsPage(<%=i %>)" class="v-button v-button- textBut">
			<span class="v-button-wrap ">
				<%if (i!=currentPage) out.print("<u>"); %>
					<span class="v-button-caption"><%=(i+1) %></span>
				<%if (i!=currentPage) out.print("</u>"); %>
			</span>
		</div>
	<% } %>
</div>
<% } %>
