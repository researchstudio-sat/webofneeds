<%--
  Created by IntelliJ IDEA.
  User: Gabriel
  Date: 17.12.12
  Time: 13:25
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ include file="/WEB-INF/jsp/include.jsp" %>

<html>
<head>
    <title>Create Need</title>
</head>
<body>
    <h1>Create a Need</h1>

    <form:form method="POST" action="create">
        <table>
            <tr>
                <td><form:label path="title">Need title:</form:label></td>
                <td><form:input path="title" /></td>
            </tr>
            <tr>
                <td><form:label path="basicNeedType">Basic type:</form:label></td>
                <td></td>
            </tr>
            <tr>
                <td><form:label path="active">Need state:</form:label></td>
                <td><form:checkbox path="active" /></td>
            </tr>
            <tr>
                <td><form:label path="anonymize">Anonymize owner:</form:label></td>
                <td><form:checkbox path="anonymize" /></td>
            </tr>
            <tr>
                <td><form:label path="wonNode">WoN Node:</form:label></td>
                <td><form:input path="wonNode" /></td>
            </tr>
            <tr>
                <td><form:label path="textDescription">Text description:</form:label></td>
                <td><form:textarea path="textDescription" rows="5" cols="30" /></td>
            </tr>
            <tr>
                <td colspan="2"><input type="submit" value="Submit"/></td>
            </tr>
        </table>
    </form:form>
</body>
</html>