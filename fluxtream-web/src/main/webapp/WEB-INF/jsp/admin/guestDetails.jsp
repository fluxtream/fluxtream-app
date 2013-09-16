<%@ page pageEncoding="utf-8" contentType="text/html; charset=UTF-8"%><%@ taglib
        uri="http://granule.com/tags" prefix="g"
%><%@ page import="java.util.List"
%><%@ page import="java.util.Map"
%><%@ page import="java.util.Set"
%><%@ page import="com.fluxtream.domain.ApiKey"
%>
<% String username = (String) request.getAttribute("username");
   Map<Long, Map<String,Object>> connectorInstanceModels = (Map<Long, Map<String,Object>>) request.getAttribute("connectorInstanceModels");
   Set<Map.Entry<Long,Map<String,Object>>> connectorInstanceModelEntries = connectorInstanceModels.entrySet();
%>

<% if (connectorInstanceModels!=null) { for ( Map.Entry<Long,Map<String,Object>> entry : connectorInstanceModelEntries) {
    Map<String,Object> connectorInstanceModel = entry.getValue();
%>

    <h3 id="apiKey-<%=entry.getKey()%>" style="margin-bottom:10px"><%=connectorInstanceModel.get("connectorName")%></h3>
    <div class="well">
        <% Map<String,String> attributes = (Map<String,String>) connectorInstanceModel.get("attributes");
           Set<Map.Entry<String,String>> attributeEntries = attributes.entrySet();
           for (Map.Entry<String,String> attributeEntry : attributeEntries) {%>
            <%=attributeEntry.getKey()%> : <%=attributeEntry.getValue()%>
        <% } %>
    </div>
    <div>Rate Limit Specifications: <%=connectorInstanceModel.get("rateLimitSpecs")%></div>
    <div <%if ((Boolean)connectorInstanceModel.get("isOverQuota")) {out.print("style=\"color:red\"");} %>>Over Quota: <%=connectorInstanceModel.get("isOverQuota")%></div>
    <% if (connectorInstanceModel.containsKey("numberOfUserCalls")) { %>
        <div>Number of User calls per specified time unit: <%=connectorInstanceModel.get("numberOfUserCalls")%></div>
    <% } %>
    <% if (connectorInstanceModel.containsKey("numberOfCalls")) { %>
        <div>Total number of calls per specified time unit: <%=connectorInstanceModel.get("numberOfCalls")%></div>
    <% } %>

<% }} else { %>
    <div>No api keys.</div>
<% } %>

