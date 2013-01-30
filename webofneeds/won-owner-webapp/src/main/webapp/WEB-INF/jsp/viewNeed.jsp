<%--
  Created by IntelliJ IDEA.
  User: Gabriel
  Date: 19.12.12
  Time: 11:57
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ include file="/WEB-INF/jsp/include.jsp" %>
<html>
<head>
    <title>View Need</title>
</head>
<body>
<h1>Need: <c:out value="${needId}" />, <c:out value="${needURI}" />,
    <a href=<c:out value="/page/need/\" +${needId} + \"/toggle\"" /> ><c:out value="${active}"/></a>
</h1>

<h2>Connect to Need:</h2>
<form:form method="POST" action="/need/\" +needId + \"/connect">
    <table>
        <tr>
            <td><form:label path="needURI">Need URI:</form:label></td>
            <td><form:input path="needURI" /></td>
        </tr>
        <tr>
            <td colspan="2">
                <input type="submit" value="Submit"/>
            </td>
        </tr>
    </table>
</form:form>

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
            <td><a href=${"/match/" + match.id + "/connect"} >connect</a></td>
        </tr>
    </c:forEach>
</table>

<h2>Connections: </h2>
<table border="1">
    <tr>
        <th>Connection ID</th>
        <th>Connection URI</th>
        <th>State</th>
        <th>View Connection</th>
    </tr>
    <c:forEach var="conection" items="${connections}">
        <tr>
            <td>${conection.id}</td>
            <td>${conection.connectionURI}</td>
            <td>${conection.state}</td>
            <td><a href=${"/connection/" + conection.id} >connection</a></td>
        </tr>
    </c:forEach>
</table>

<script type="text/javascript" src="scripts/jquery-1.8.3.min.js" ></script>
<script type="text/javascript">
    setTimeout(function(){
        window.location.reload();
    }, 2000);
</script>
</body>
</html>