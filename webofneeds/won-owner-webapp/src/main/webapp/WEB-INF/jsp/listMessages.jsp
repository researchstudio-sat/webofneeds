<%--
  Created by IntelliJ IDEA.
  User: gabriel
  Date: 11.02.13
  Time: 10:25
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ include file="/WEB-INF/jsp/include.jsp" %>

<h2>Messages: </h2>
<table border="1">
    <tr>
        <th>Message ID</th>
        <th>Creation Date</th>
        <th>Originator URI</th>
        <th>Message</th>
    </tr>
    <c:forEach var="mes" items="${messages}">
        <tr>
            <td>${mes.id}</td>
            <td>${mes.creationDate}</td>
            <td>${mes.originatorURI}</td>
            <td>${mes.message}</td>
        </tr>
    </c:forEach>
</table>