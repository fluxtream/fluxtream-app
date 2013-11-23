<%@ page pageEncoding="utf-8" contentType="text/html; charset=UTF-8"%><%@ page import="java.util.List"
%><%@ page import="java.util.Map"
%><%@ page import="java.util.Set"
%>
<% String username = (String) request.getAttribute("username");
    Map<Long, Map<String,Object>> connectorInstanceModels = (Map<Long, Map<String,Object>>) request.getAttribute("connectorInstanceModels");
    Set<Map.Entry<Long,Map<String,Object>>> connectorInstanceModelEntries = connectorInstanceModels.entrySet();
%>

<h3><%=username%></h3>


<% if (connectorInstanceModels!=null) { for ( Map.Entry<Long,Map<String,Object>> entry : connectorInstanceModelEntries) {
    Map<String,Object> connectorInstanceModel = entry.getValue();
%>

<script>

    function editApiKeyAttributeValue(apiKeyId, attributeKey, attributeValue){
        var newValue = prompt("Edit Attribute Value", attributeValue);
        if (newValue!=null) {
            $.ajax({
                url: "/api/admin/apiKeys/" + apiKeyId + "/attribute",
                data: {attributeKey: attributeKey, attributeValue : newValue},
                type: "POST",
                success: function(){
                    location.reload();
                }
            });
        }
    }

    function deleteApiKeyAttributeValue(apiKeyId, attributeKey){
        var confirmed = confirm("Are you sure?");
        if (confirmed) {
            $.ajax({
                url: "/api/admin/apiKeys/" + apiKeyId + "/" + attributeKey,
                type: "DELETE",
                success: function(){
                    location.reload();
                }
            });
        }
    }

    function addApiKeyAttribute(apiKeyId){
        var newKey = prompt("Please, enter Attribute Key (/Name)");
        if (newKey==null) return;
        var newValue = prompt("Please, enter Attribute Value");
        if (newValue!=null) {
            $.ajax({
                url: "/api/admin/apiKeys/" + apiKeyId + "/attribute/add",
                data: {attributeKey: newKey, attributeValue : newValue},
                type: "POST",
                success: function(status){
                    if (status.result==="OK")
                        location.reload();
                    else
                        alert(status.message);
                }
            });
        }
    }

</script>
<h3 id="apiKey-<%=entry.getKey()%>" style="margin-bottom:10px">
    <%=connectorInstanceModel.get("connectorName")%>
    <% if (connectorInstanceModel.get("status") == "STATUS_PERMANENT_FAILURE") { %>
    <span class="label label-important" style="vertical-align:middle">down</span>
    <% } else if (connectorInstanceModel.get("status") == "STATUS_TRANSIENT_FAILURE") { %>
    <span class="label label-warning" style="vertical-align:middle">transient</span>
    <% } else if (connectorInstanceModel.get("status") == "STATUS_OVER_RATE_LIMIT") { %>
    <span class="label label-info" style="vertical-align:middle">over limit</span>
    <% } else { %>
    <span class="label label-success" style="vertical-align:middle">up</span>
    <% } %>
    <a class="btn btn-link" href="/admin/${guestId}/<%=entry.getKey()%>">more...</a>
</h3>
<div class="well" id="apiKeyAttributes-<%=entry.getKey()%>">
    <% Map<String,String> attributes = (Map<String,String>) connectorInstanceModel.get("attributes");
        Set<Map.Entry<String,String>> attributeEntries = attributes.entrySet();
        for (Map.Entry<String,String> attributeEntry : attributeEntries) {%>
    <%=attributeEntry.getKey()%> : <%=attributeEntry.getValue()%>
        <a onclick="editApiKeyAttributeValue(<%=entry.getKey()%>, '<%=attributeEntry.getKey()%>', '<%=attributeEntry.getValue()%>')"><i class="icon icon-pencil"></i></a>
        <a onclick="deleteApiKeyAttributeValue(<%=entry.getKey()%>, '<%=attributeEntry.getKey()%>')"><i class="icon icon-trash"></i></a>
        <br>
    <% } %>
    <a onclick="addApiKeyAttribute(<%=entry.getKey()%>)"><i class="icon icon-plus"></i>&nbsp;add attribute</a>
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
