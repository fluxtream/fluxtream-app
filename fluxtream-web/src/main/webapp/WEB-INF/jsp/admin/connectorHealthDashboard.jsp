<%@ page pageEncoding="utf-8" contentType="text/html; charset=UTF-8"
        %><%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %><%@ page import="java.util.List"
        %><%@ page import="org.fluxtream.core.domain.ConnectorInfo"
        %><%
    final List<ConnectorInfo> connectors = (List<ConnectorInfo>) request.getAttribute("connectors");
    final int fromGuest = (Integer)request.getAttribute("fromGuest");
    final int toGuest = (Integer)request.getAttribute("toGuest");
    final long totalGuests = (Long)request.getAttribute("totalGuests");
    final int pageSize = (Integer)request.getAttribute("pageSize");
    final int currentPage = (Integer)request.getAttribute("page");
    final long lastPage = totalGuests%pageSize==0 ? totalGuests/pageSize : totalGuests/pageSize+1;
%>

<h3>Connector Health Dashboard</h3>

<style>
    #header-fixed {
        position: fixed;
        top: 0px; display:none;
        background-color:white;
        padding-right: 40px;
    }
    .guestName-column {
        width: 120px;
    }
    .guestId-column {
        width: 25px;
    }
    .apiKeyStatus {
        width: 25px;
    }
    #dashboardTable, #header-fixed {
        table-layout: fixed;
    }

    th, td {
        overflow: hidden;
    }

</style>


<div style="float:left">
    Showing guests ${fromGuest} to ${toGuest} from a total of ${totalGuests}
    <%if(fromGuest-pageSize>=0) { %> &nbsp;<a href="/admin?page=<%=currentPage-1%>&pageSize=<%=pageSize%>">Previous</a><% } %>
    <%if(toGuest<totalGuests) { %>&nbsp;<a href="/admin?page=<%=currentPage+1%>&pageSize=<%=pageSize%>">Next</a><% } %>
    &nbsp;<a href="/admin?page=1&pageSize=<%=pageSize%>">First</a>
    &nbsp;<a href="/admin?page=<%=lastPage%>&pageSize=<%=pageSize%>">Last</a>
</div>
<div style="float:right">
<label style="display:inline;vertical-align:middle; margin-right:5px" for="pageSizeSelect">Page Size:</label><select id="pageSizeSelect" style="width: 70px" onchange="window.location='/admin?page=1&pageSize='+this.value">
    <option<%if(pageSize==20){%> selected<%}%>>20</option>
    <option<%if(pageSize==50){%> selected<%}%>>50</option>
    <option<%if(pageSize==100){%> selected<%}%>>100</option>
    <option<%if(pageSize==200){%> selected<%}%>>200</option>
    <option<%if(pageSize==400){%> selected<%}%>>400</option>
    <option<%if(pageSize==1000){%> selected<%}%>>1000</option>
    <option<%if(pageSize==2000){%> selected<%}%>>2000</option>
</select></div>

<table class="table table-bordered" id="header-fixed"></table>

<table class="table table-bordered" id="dashboardTable">
    <thead>
        <th class="guestName-column">Guest name</th>
        <th class="guestId-column">Guest ID</th>
        <c:forEach var="connectorInfo" items="${connectors}">
        <th class="apiKeyStatus">${connectorInfo.name}</th>
        </c:forEach>
    </thead>
    <tbody>
        <c:forEach var="row" items="${rows}">
        <tr>
            <td class="guestName-column">
                <a class="btn btn-link" href="/admin/${row.key.id}">${row.key.guestName}</a>
            </td>
            <td class="guestId-column">
                <a class="btn btn-link" href="/admin/${row.key.id}">${row.key.id}</a>
            </td>
            <c:forEach var="connectorKeys" items="${row.value}">
            <td class="apiKeyStatus">
                <c:if test="${empty connectorKeys}">
                    &empty;
                </c:if>
                <c:forEach var="apiKey" items="${connectorKeys}">
                    <c:set var="currentlySynching" value="${synching.get(apiKey.id)}"/>
                    <c:set var="due" value="${tasksDue.get(apiKey.id)}"/>
                    <c:set var="overdue" value="${tasksOverdue.get(apiKey.id)}"/>
                    <c:choose>
                        <c:when test="${empty apiKey.status}">
                            <a class="btn btn-link" href="/admin/${apiKey.getGuestId()}/${apiKey.id}">
                               <div title="${apiKey.status}" class="syncStatus-NA">&nbsp;</div>
                            </a>
                        </c:when>
                        <c:otherwise>
                            <a class="btn btn-link" href="/admin/${apiKey.getGuestId()}/${apiKey.id}">
                               <div title="${apiKey.status}" class="syncStatus-${apiKey.status}">&nbsp;</div>
                            </a>
                        </c:otherwise>
                    </c:choose>
                    <c:if test="${currentlySynching>0}">
                        <c:choose>
                            <c:when test="${currentlySynching==1}">
                                <i class="icon-refresh"></i>
                            </c:when>
                            <c:otherwise>
                                <span style="white-space:nowrap">${currentlySynching}&nbsp;<i class="icon-refresh"></i></span>
                            </c:otherwise>
                        </c:choose>
                    </c:if>
                    <c:if test="${due>0}">
                        <c:choose>
                            <c:when test="${due==1}">
                                <i class="icon-inbox" style="color:forestgreen"></i>
                            </c:when>
                            <c:otherwise>
                                <span style="white-space:nowrap">${due}&nbsp;<i class="icon-inbox" style="color:forestgreen"></i></span>
                            </c:otherwise>
                        </c:choose>
                    </c:if>
                    <c:if test="${overdue>0}">
                        <c:choose>
                            <c:when test="${overdue==1}">
                                <i class="icon-inbox" style="color:#ff0000"></i>
                            </c:when>
                            <c:otherwise>
                                <span style="white-space:nowrap">${overdue}&nbsp;<i class="icon-inbox" style="color:#ff0000"></i></span>
                            </c:otherwise>
                        </c:choose>
                    </c:if><br>
                </c:forEach>
            </td>
            </c:forEach>
        </tr>
        </c:forEach>
    </tbody>
</table>

<script>
    var tableOffset = $("#dashboardTable").offset().top;
    var $header = $("#dashboardTable > thead").clone();
    var $fixedHeader = $("#header-fixed").append($header);

    var $body = $("#dashboardTable > tbody").clone();
    var $fixedBody = $("#header-fixed").append($body);
    $body.hide();

    $(window).bind("scroll", function() {
        var offset = $(this).scrollTop();

        if (offset >= tableOffset && $fixedHeader.is(":hidden")) {
            $fixedHeader.show();
        }
        else if (offset < tableOffset) {
            $fixedHeader.hide();
        }
    });
</script>