<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page pageEncoding="utf-8" contentType="text/html; charset=UTF-8"%><%@ page import="java.util.ArrayList"
%><%@ page import="java.util.List"
%><%@ page import="java.util.Map"
%>
<%@ page import="java.util.Set" %>
<%@ page import="org.fluxtream.core.connectors.Connector" %>
<%@ page import="org.fluxtream.core.connectors.ObjectType" %>
<%@ page import="org.fluxtream.core.domain.ApiKey" %>
<%@ page import="org.fluxtream.core.domain.ApiUpdate" %>
<%@ page import="org.fluxtream.core.domain.Guest" %>
<%@ page import="org.fluxtream.core.domain.UpdateWorkerTask" %>
<%@ page import="org.apache.commons.lang.StringUtils" %>
<%@ page import="org.joda.time.format.DateTimeFormat" %>
<%
    Guest guest = (Guest)request.getAttribute("guest");
    Map<String,Object> connectorInstanceModel = (Map<String,Object>) request.getAttribute("connectorInstanceModel");
    List<ApiUpdate> lastUpdates = (List<ApiUpdate>) request.getAttribute("lastUpdates");
    List<UpdateWorkerTask> scheduledTasks = (List<UpdateWorkerTask>)request.getAttribute("scheduledTasks");
    ApiKey apiKey = (ApiKey)request.getAttribute("apiKey");
    Map<String,String> attributes = (Map<String,String>)request.getAttribute("attributes");
    List<String> liveServerUUIDs = (List<String>) request.getAttribute("liveServerUUIDs");

    final int[] values = apiKey.getConnector().objectTypeValues();
    final List<Integer> connectorObjectTypes = new ArrayList<Integer>();
    for (int value : values)
        connectorObjectTypes.add(value);
    String errors = (String) connectorInstanceModel.get("auditTrail");
%>
<jsp:include page="editAttributesScript.jsp"></jsp:include>

<h3><%=guest.getGuestName()%>/<%=apiKey.getConnector().prettyName()%>
    <% if (connectorInstanceModel.get("status").equals("STATUS_PERMANENT_FAILURE")) { %>
    <span class="label label-important" style="vertical-align:middle">down</span>
    <% } else if (connectorInstanceModel.get("status").equals("STATUS_TRANSIENT_FAILURE")) { %>
    <span class="label label-warning" style="vertical-align:middle">transient</span>
    <% } else if (connectorInstanceModel.get("status").equals("STATUS_OVER_RATE_LIMIT")) { %>
    <span class="label label-info" style="vertical-align:middle">over limit</span>
    <% } else { %>
    <span class="label label-success" style="vertical-align:middle">up</span>
    <% } if (!connectorInstanceModel.get("status").equals("STAOtwTUS_PERMANENT_FAILURE")) {%>
    <a class="btn btn-link" style="vertical-align: bottom" href="/admin/<%=apiKey.getGuestId()%>/<%=apiKey.getId()%>/setToPermanentFail">Set to permanent fail</a>
    <% } %>
</h3>


<div class="well">
    <% Set<Map.Entry<String,String>> attributeEntries = attributes.entrySet();
        for (Map.Entry<String,String> attributeEntry : attributeEntries) {%>
    <%=attributeEntry.getKey()%> : <%=attributeEntry.getValue()%>
    <a onclick="editApiKeyAttributeValue(<%=apiKey.getId()%>, '<%=attributeEntry.getKey()%>', '<%=attributeEntry.getValue()%>')"><i class="icon icon-pencil"></i></a>
    <a onclick="deleteApiKeyAttributeValue(<%=apiKey.getId()%>, '<%=attributeEntry.getKey()%>')"><i class="icon icon-trash"></i></a>
    <br>
    <% } %>
    <a onclick="addApiKeyAttribute(<%=apiKey.getId()%>)"><i class="icon icon-plus"></i>&nbsp;add attribute</a>
</div>


<h4>Force Update <small> - the type of update (history vs incremental) will depend on the type and
    status of the last update of that facet type</small>
</h4>
<div>
<% if (!apiKey.getConnector().isAutonomous()) {
     for (Integer objectTypes : connectorObjectTypes) { %>
         <a class="btn btn-primary" href="/admin/<%=guest.getId()%>/<%=apiKey.getId()%>/<%=objectTypes%>/refresh">Update <%=ObjectType.getObjectTypes(apiKey.getConnector(), objectTypes)%> facets Now!</a>
<%   }
   } else { %>
<a class="btn btn-primary" href="/admin/<%=guest.getId()%>/<%=apiKey.getId()%>/0/refresh">Update Now!</a>
<% } %>
</div>

<h4>Force History Update</h4>
<div>
    <% if (!apiKey.getConnector().isAutonomous()) {
        for (Integer objectTypes : connectorObjectTypes) { %>
    <a class="btn btn-primary" href="/admin/<%=guest.getId()%>/<%=apiKey.getId()%>/<%=objectTypes%>/historyUpdate">Redo History Update of <%=ObjectType.getObjectTypes(apiKey.getConnector(), objectTypes)%> facets Now!</a>
    <%   }
    } else { %>
    <a class="btn btn-primary" href="/admin/<%=guest.getId()%>/<%=apiKey.getId()%>/0/historyUpdate">Redo History Update Now!</a>
    <% } %>
</div>


<% if ((Boolean)connectorInstanceModel.get("errors")&&StringUtils.isNotEmpty(errors)) {
%>
    <h4>Stack trace</h4>
    <div class="alert alert-error"><%=errors%></div>
<% } %>

<% if (scheduledTasks.size()>0) {%>
<h4>Worker Tasks</h4>

<table class="table">
    <thead>
    <tr>
        <th>ID</th>
        <th>Time</th>
        <th>Object Types</th>
        <th>Status</th>
        <th>Server UUID</th>
    </tr>
    </thead>
    <tbody>
    <% for (UpdateWorkerTask task : scheduledTasks) {
        System.out.println(task);
        System.out.println(task.timeScheduled);
        final String time = DateTimeFormat.mediumDateTime().print(task.timeScheduled);
        final List<ObjectType> objectTypes = ObjectType.getObjectTypes(Connector.getConnector(task.connectorName), task.objectTypes);
        String color, status;
        switch (task.status) {
            case FAILED:
                status = "thumbs-down";
                color = "red";
                break;
            case DONE:
                status = "thumbs-up";
                color = "green";
                break;
            case IN_PROGRESS:
                status = "refresh";
                color = "black";
                break;
            case SCHEDULED:
                status = "inbox";
                color = "green";
                break;
            default:
                status = "thumbs-down";
                color = "black";
        };
        // set the color of the serverUUID to red if it's null or not a live server, green if it's a live
        // server and black if it's unassigned
        String serverUUIDColor;
        if (task.serverUUID==null)
            serverUUIDColor = "red";
        else if (task.serverUUID.equals("unassigned"))
            serverUUIDColor = "black";
        else if (liveServerUUIDs.contains(task.serverUUID))
            serverUUIDColor = "green";
        else
            serverUUIDColor = "red";
    %>
    <tr>
        <td style="width:30px;vertical-align: middle"><%=task.getId()%></td>
        <td style="width:100px;vertical-align: middle"><%=time%></td>
        <td style="width:200px;vertical-align: middle"><%=objectTypes!=null&&objectTypes.size()>0?objectTypes:"N/A"%></td>
        <td style="width:30px;vertical-align: middle; color:<%=color%>" title="<%=task.auditTrail%>"><i class="icon-<%=status%>"></i></td>
        <td style="width:30px;vertical-align: middle; color:<%=serverUUIDColor%>"><%=task.serverUUID%></td>
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
