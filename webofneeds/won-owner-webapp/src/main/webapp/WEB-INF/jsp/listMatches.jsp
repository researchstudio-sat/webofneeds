<%--
  Created by IntelliJ IDEA.
  User: gabriel
  Date: 08.02.13
  Time: 15:08
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ include file="/WEB-INF/jsp/include.jsp" %>

<script type="text/javascript" src="/owner/scripts/jquery-1.8.3.min.js" ></script>
<h2>Matches: </h2>
<table border="1">
    <tr>
        <th>Match Id</th>
        <th>other Need URI</th>
        <th>Score</th>
        <th>Connect</th>
    </tr>
    <c:forEach var="match" items="${matches}">
        <tr>
            <td>${match.id}</td>
            <td>${match.toNeed}</td>
            <td>${match.score}</td>
            <td>
                <button onclick="jQuery.post('/owner/need/match/${match.id}/connect');" >connect</button>
            </td>
        </tr>
    </c:forEach>
</table>