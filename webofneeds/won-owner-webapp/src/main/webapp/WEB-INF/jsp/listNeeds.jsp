<%--
  Created by IntelliJ IDEA.
  User: gabriel
  Date: 11.02.13
  Time: 17:08
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ include file="/WEB-INF/jsp/include.jsp" %>
<html>
<head>
    <title></title>
</head>
<body>

<h2>Needs: </h2>
<a href="/owner/need/reload">reload</a>
<table border="1">
    <tr>
        <th>Need Id</th>
        <th>Need URI</th>
        <th>Status</th>
        <th>Detail</th>
    </tr>
    <c:forEach var="need" items="${needs}">
        <tr>
            <td>${need.id}</td>
            <td>${need.needURI}</td>
            <td>${need.state}</td>
            <td><a href="/owner/need/${need.id}">view</a></td>
        </tr>
    </c:forEach>
</table>
</body>
</html>