<%@ page import="won.owner.pojo.NeedPojo" %>
<%--
  Created by IntelliJ IDEA.
  User: Gabriel
  Date: 19.12.12
  Time: 11:57
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ include file="/WEB-INF/jsp/include.jsp" %>
<% //NeedPojo need = (NeedPojo) request.getAttribute("pojo"); %>

<html>
<head>
    <title>Need: <c:out value="${pojo.title}" /></title>
</head>
<body>
<h1>Need: <c:out value="${pojo.title}" /></h1>
<p>[<c:out value="${needId}" />] <a href="<c:out value="${needURI}" />"><c:out value="${needURI}" /></a></p>

<dl>
    <dt>Created on</dt>
        <dd><c:out value="${pojo.creationDate}" /></dd>
    <dt>Text description</dt>
        <dd><c:out value="${pojo.textDescription}" /></dd>
    <dt>Need type</dt>
        <dd><c:out value="${pojo.basicNeedType}" /></dd>
    <dt>Active</dt>
        <dd><c:out value="${active}" /><br />
            <button onclick="jQuery.post('/owner/need/<c:out value="${needId}" />/toggle', function() { document.location.reload(true); });" ><c:out value="${active}"/></button>
        </dd>
    <dt>Price specification</dt>
        <dd>Upper limit: <c:out value="${pojo.upperPriceLimit}" /><br />
        Lower limit: <c:out value="${pojo.lowerPriceLimit}" /><br />
        Currency <c:out value="${pojo.currency}" /></dd>
    <dt>Delivery method</dt>
        <dd><c:out value="${pojo.deliveryMethod}" /></dd>
    <dt>Location</dt>
        <dd>Latitude: <c:out value="${pojo.latitude}" /><br />
        Longitude: <c:out value="${pojo.longitude}" /></dd>
    <dt>Time constraints</dt>
        <dd>Start time:<c:out value="${pojo.startTime}" /><br />
            End time:<c:out value="${pojo.endTime}" /><br />
            Recur in:<c:out value="${pojo.recurIn}" /><br />
            Recur times:<c:out value="${pojo.recurTimes}" /><br />
            Recur infinite times:<c:out value="${pojo.recurInfiniteTimes}" /><br />
        </dd>

</dl>

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