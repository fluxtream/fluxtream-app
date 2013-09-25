<%@ page pageEncoding="utf-8" contentType="text/html; charset=UTF-8"
        %><%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %><%@ page import="java.util.List"
        %><%@ page import="com.fluxtream.domain.ConnectorInfo"
        %><%
    final List<ConnectorInfo> connectors = (List<ConnectorInfo>) request.getAttribute("connectors");%>

<h3>Connector Health Dashboard</h3>

<table>
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
            <td>
                <c:forEach var="apiKey" items="${connectorKeys}">
                    ${apiKey.status} &nbsp;
                </c:forEach>
            </td>
            </c:forEach>
        </tr>
        </c:forEach>
    </tbody>
</table>