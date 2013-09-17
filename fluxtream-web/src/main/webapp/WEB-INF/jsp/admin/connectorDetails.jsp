<%@ page pageEncoding="utf-8" contentType="text/html; charset=UTF-8"%><%@ page import="java.util.List"
%><%@ page import="java.util.Map"
%><%@ page import="com.fluxtream.domain.ApiUpdate"
%>
<%@ page import="org.joda.time.format.ISODateTimeFormat" %>
<%
    String username = (String) request.getAttribute("username");
    Map<String,Object> connectorInstanceModel = (Map<String,Object>) request.getAttribute("connectorInstanceModel");
    List<ApiUpdate> lastUpdates = (List<ApiUpdate>) request.getAttribute("lastUpdates");
%>

<% if ((Boolean)connectorInstanceModel.get("errors")) {
    String errors = (String) connectorInstanceModel.get("auditTrail");
%>
    <div class="alert alert-error"><%=errors%></div>
<% } %>

<table class="table">
    <thead>
    <tr>
        <th>Time</th>
        <th>Query</th>
        <th>Http Response Code</th>
        <th>Reason</th>
    </tr>
    </thead>
    <tbody>
    <% for (ApiUpdate call : lastUpdates) {
        String successOrError = call.success?"success":"error";
        final String time = ISODateTimeFormat.basicDateTime().print(call.ts);
    %>
    <tr class="<%=successOrError%>">
        <td><%=time%></td>
        <td><%=call.query%></td>
        <td><%=call.httpResponseCode%></td>
        <td><%=call.reason%></td>
    </tr>
    <% } %>
    </tbody>
</table>
