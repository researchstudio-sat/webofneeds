<%@ page import="won.owner.pojo.NeedPojo" %>
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
    <title>Need: <c:out value="${pojo.title}" /></title>
    <style type="text/css">
        #wholepanel {
            position: relative;
        }

        #rightpanel {
            position: absolute;
            top: 0;
            right: 0;
            width: 600px;
        }
        #leftpanel {
            position: absolute;
            top: 0;
            left: 0;
            width: 600px;
        }
        #needdata {
            position: relative;
            top: 0;
            left: 0;
            width: 500px;
        }
        #connectpanel {
            position: relative;
        }
        #connections {
            position: relative;
            height:300px;
        }
        #matches {
            position: relative;
            height:300px;
        }
    </style>
</head>
<body>
<div id="wholepanel">
    <div id="leftpanel">
    <div id="connectpanel">
        <h2>Connect to Need:</h2>
        <form:form method="POST" action="/owner/need/${needId}/connect">
            <table>
                <tr>
                    <td><form:label path="needURI">Need URI:</form:label></td>
                    <td><form:input path="needURI" size="40"/></td>        needFacetURIs
                </tr>
                <tr>
                    <td>My Facet type:</td>
                    <td><form:select path="ownFacetURI">
                        <form:options items="${command.needFacetURIs}"/>
                    </form:select>
                    </td>
                </tr>
                <tr>
                    <td>Remote Facet type:</td>
                    <td>
                        <form:select path="remoteFacetURI">
                            <form:options items="${command.facetURIs}"/>
                        </form:select>
                    </td>
                </tr>
                <tr>
                    <td colspan="2">
                        <input type="submit" value="Submit"/>
                    </td>
                </tr>
            </table>
        </form:form>
    </div> <!-- connectpanel -->
<div id="needdata">

<h1>Need: <c:out value="${pojo.title}" /></h1>
<p>[<c:out value="${needId}" />] <a href="<c:out value="${needURI}" />"><c:out value="${needURI}" /></a></p>

<dl>
    <dt>Created on</dt>
        <dd><c:out value="${pojo.creationDate}" /></dd>
    <dt>Facets:</dt>
        <c:forEach var="facet" items="${pojo.needFacetURIs}">
            <dd><c:out value="${facet}" /> </dd>
        </c:forEach>
    <dt>Need type</dt>
        <dd><c:out value="${pojo.basicNeedType}" /></dd>
    <c:if test="${ not empty pojo.textDescription}">
        <dt>Text description</dt>
        <dd><c:out value="${pojo.textDescription}" /></dd>
    </c:if>
    <c:if test="${ not empty pojo.tags}">
        <dt>Tags</dt>
            <dd><c:out value="${pojo.tags}" /></dd>
    </c:if>
    <dt>Active</dt>
        <dd><c:out value="${pojo.state}" /><br />
            <button onclick="jQuery.post('/owner/need/<c:out value="${needId}" />/toggle', function() { document.location.reload(true); });" ><c:out value="${active}"/></button>
        </dd>
    <c:if test="${not empty pojo.upperPriceLimit || not empty pojo.lowerPriceLimit}">
    <dt>Price specification</dt>
        <dd>Upper limit: <c:out value="${pojo.upperPriceLimit}" /><br />
        Lower limit: <c:out value="${pojo.lowerPriceLimit}" /><br />
        Currency <c:out value="${pojo.currency}" /></dd>
    </c:if>
    <c:if test="${not empty pojo.latitude|| not empty pojo.longitude}">
        <dt>Location</dt>
            <dd>Latitude: <c:out value="${pojo.latitude}" /><br />
            Longitude: <c:out value="${pojo.longitude}" /></dd>
    </c:if>
    <c:if test="${not empty pojo.startTime|| not empty pojo.endTime}">
        <dt>Time constraints</dt>
            <dd>Start time:<c:out value="${pojo.startTime}" /><br />
                End time:<c:out value="${pojo.endTime}" /><br />
                Recur in:<c:out value="${pojo.recurIn}" /><br />
                Recur times:<c:out value="${pojo.recurTimes}" /><br />
                Recur infinite times:<c:out value="${pojo.recurInfiniteTimes}" /><br />
            </dd>
    </c:if>
    <c:if test="${not empty pojo.contentDescription}">
        <dt>Content description:</dt>
            <dd>
                <c:out value="${pojo.contentDescription}" />
            </dd>
    </c:if>

</dl>
</div> <!-- needdata -->
</div> <!-- leftpanel -->
<div id="rightpanel">

<div id="connections">
    <iframe id="iConnections" class="cframe"  width="100%"  height="100%"
            src="/owner/need/<c:out value="${needId}" />/listConnections"></iframe>
</div>
<div id="matches">
    <iframe id="iMatches" class="mframe"  width="100%"  height="100%" src=
            "/owner/need/<c:out value="${needId}" />/listMatches"></iframe>
</div>
</div> <!-- actionpanel -->
</div> <!-- wholepanel-->
<script type="text/javascript" src="/owner/scripts/jquery-1.8.3.min.js" ></script>
<script type="text/javascript">
    var refresh = function(){
        document.getElementById("iMatches").contentDocument.location.reload(true);
        document.getElementById("iConnections").contentDocument.location.reload(true);
        setTimeout(refresh, 15000);
    };
    setTimeout(refresh, 15000);
</script>
</body>
</html>