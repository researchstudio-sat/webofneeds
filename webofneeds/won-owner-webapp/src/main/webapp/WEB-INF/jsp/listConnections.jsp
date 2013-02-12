<%--
  Created by IntelliJ IDEA.
  User: gabriel
  Date: 08.02.13
  Time: 15:08
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ include file="/WEB-INF/jsp/include.jsp" %>

<h2>Connections: </h2>
<table border="1">
    <tr>
        <th>Connection ID</th>
        <th>Connection URI</th>
        <th>State</th>
        <th>View Connection</th>
    </tr>
    <c:forEach var="connection" items="${connections}">
        <tr>
            <td>${connection.id}</td>
            <td>${connection.connectionURI}</td>
            <td>${connection.state}</td>
            <td><a href="/owner/connection/${connection.id}" target="_parent" >connection</a></td>
        </tr>
    </c:forEach>
</table>
