<%@ page pageEncoding="utf-8" contentType="text/html; charset=UTF-8"
        %><%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %><%@ page import="java.util.List"
        %><%@ page import="com.fluxtream.domain.ConnectorInfo"
        %><%
    final List<ConnectorInfo> connectors = (List<ConnectorInfo>) request.getAttribute("connectors");%>

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
<table class="table table-bordered" id="header-fixed"></table>

<table class="table table-bordered" id="dashboardTable">
    <thead>
        <th class="guestName-column">Guest Name</th>
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