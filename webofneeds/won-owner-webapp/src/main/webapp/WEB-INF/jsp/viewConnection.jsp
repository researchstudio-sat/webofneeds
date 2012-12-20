<%--
  Created by IntelliJ IDEA.
  User: Gabriel
  Date: 19.12.12
  Time: 15:07
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ include file="/WEB-INF/jsp/include.jsp" %>
<html>
<head>
    <title>Connection</title>
</head>
<body>
<h1>Connection: <c:out value="${connection.id}" /></h1>

<form:form method="POST" action="/connection/${connection.id}/send">
    <table>
        <tr>
            <td><form:label path="text">Text: </form:label></td>
            <td><form:input path="text" /></td>
        </tr>
        <tr>
            <td colspan="2">
                <input type="submit" value="Submit"/>
            </td>
        </tr>
    </table>
</form:form>

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

<script type="text/javascript" src="scripts/jquery-1.8.3.min.js" ></script>
<script type="text/javascript">
    setTimeout(function(){
        window.location.reload();
    }, 2000);
</script>
</body>
</html>