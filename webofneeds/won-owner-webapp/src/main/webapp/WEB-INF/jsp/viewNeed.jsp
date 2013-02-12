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
<h1>Need: <c:out value="${needId}" />, <c:out value="${needURI}" /> </h1>

<button onclick="jQuery.post('/owner/need/<c:out value="${needId}" />/toggle', function() { document.location.reload(true); });" ><c:out value="${active}"/></button>


<h2>Connect to Need:</h2>
<form:form method="POST" action="/owner/need/${needId}/connect">
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

<div class="matches">
    <iframe id="iMatches" class="mframe"  width="100%"  height="300px" src="/owner/need/<c:out value="${needId}" />/listMatches"></iframe>
</div>

<div class="connections">
    <iframe id="iConnections" class="cframe"  width="100%"  height="300px" src="/owner/need/<c:out value="${needId}" />/listConnections"></iframe>
</div>

<script type="text/javascript" src="/owner/scripts/jquery-1.8.3.min.js" ></script>
<script type="text/javascript">
    var refresh = function(){
        document.getElementById("iMatches").contentDocument.location.reload(true);
        document.getElementById("iConnections").contentDocument.location.reload(true);
        setTimeout(refresh, 2000);
    };
    setTimeout(refresh, 2000);
</script>
</body>
</html>