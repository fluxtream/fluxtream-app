<%@ page pageEncoding="utf-8" contentType="text/html; charset=UTF-8"%><%@ page import="java.util.List"
%><%@ page import="java.util.Map"
%><%@ page import="com.fluxtream.connectors.Connector"
%>
<%@ page import="com.fluxtream.connectors.ObjectType" %>
<%@ page import="com.fluxtream.domain.ApiKey" %>
<%@ page import="com.fluxtream.domain.ApiUpdate" %>
<%@ page import="com.fluxtream.domain.Guest" %>
<%@ page import="com.fluxtream.domain.UpdateWorkerTask" %>
<%@ page import="org.joda.time.format.DateTimeFormat" %>
<%
    Guest guest = (Guest)request.getAttribute("guest");
    Map<String,Object> connectorInstanceModel = (Map<String,Object>) request.getAttribute("connectorInstanceModel");
    List<ApiUpdate> lastUpdates = (List<ApiUpdate>) request.getAttribute("lastUpdates");
    List<UpdateWorkerTask> scheduledTasks = (List<UpdateWorkerTask>)request.getAttribute("scheduledTasks");
    ApiKey apiKey = (ApiKey)request.getAttribute("apiKey");
%>

<h3><%=guest.getGuestName()%>/<%=apiKey.getConnector().prettyName()%></h3>

<% if ((Boolean)connectorInstanceModel.get("errors")) {
    String errors = (String) connectorInstanceModel.get("auditTrail");
%>
    <h4>Stack trace</h4>
    <div class="alert alert-error"><%=errors%></div>
<% } %>

<% if (scheduledTasks.size()>0) {
    System.out.println(scheduledTasks);%>
<h4>Next Scheduled Tasks</h4>

<table class="table">
    <thead>
    <tr>
        <th></th>
        <th>Time</th>
        <th>Object Types</th>
    </tr>
    </thead>
    <tbody>
    <% for (UpdateWorkerTask task : scheduledTasks) {
        System.out.println(task);
        System.out.println(task.timeScheduled);
        final String time = DateTimeFormat.mediumDateTime().print(task.timeScheduled);
        final List<ObjectType> objectTypes = ObjectType.getObjectTypes(Connector.getConnector(task.connectorName), task.objectTypes);
    %>
    <tr>
        <td style="width:120px"><a class="btn btn-primary" href="/admin/<%=guest.getId()%>/<%=apiKey.getId()%>/<%=task.objectTypes%>/refresh">Update Now!</a></td>
        <td style="width:200px;vertical-align: middle"><%=time%></td>
        <td style="vertical-align: middle"><%=objectTypes%></td>
    </tr>
    <% } %>
    </tbody>
</table>

<% } %>

<h4>Recent API calls</h4>

<table class="table">
    <thead>
    <tr>
        <th style="min-width:200px">Time</th>
        <th>Query</th>
        <th>Http Response Code</th>
        <th>Reason</th>
    </tr>
    </thead>
    <tbody>
    <% for (ApiUpdate call : lastUpdates) {
        String successOrError = call.success?"success":"error";
        final String time = DateTimeFormat.mediumDateTime().print(call.ts);
    %>
    <tr class="<%=successOrError%>">
        <td><%=time%></td>
        <td><%=call.query%></td>
        <td><%=call.httpResponseCode==null?"N/A":call.httpResponseCode%></td>
        <td><%=call.reason==null?"N/A":call.reason%></td>
    </tr>
    <% } %>
    </tbody>
</table>
