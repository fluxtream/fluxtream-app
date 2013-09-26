<%@ page pageEncoding="utf-8" contentType="text/html; charset=UTF-8"
        %><%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %><%@ page import="java.util.List"
        %><%@ page import="com.fluxtream.domain.ConnectorInfo"
        %><%
    final List<ConnectorInfo> connectors = (List<ConnectorInfo>) request.getAttribute("connectors");%>

<h3>Connector Health Dashboard</h3>

<table class="table table-bordered">
    <thead>
        <th>Guest Name</th>
        <c:forEach var="connectorInfo" items="${connectors}">
        <th>${connectorInfo.name}</th>
        </c:forEach>
    </thead>
    <tbody>
        <c:forEach var="row" items="${rows}">
        <tr>
            <td>${row.key.guestName}</td>
            <c:forEach var="connectorKeys" items="${row.value}">
            <td class="apiKeyStatus">
                <c:if test="${empty connectorKeys}">
                    &empty;
                </c:if>
                <c:forEach var="apiKey" items="${connectorKeys}">
                    <c:choose>
                        <c:when test="${empty apiKey.status}">
                            <div title="${apiKey.status}" class="syncStatus-NA">&nbsp;</div>
                        </c:when>
                        <c:otherwise>
                            <div title="${apiKey.status}" class="syncStatus-${apiKey.status}">&nbsp;</div>
                        </c:otherwise>
                    </c:choose>
                </c:forEach>
            </td>
            </c:forEach>
        </tr>
        </c:forEach>
    </tbody>
</table>