<%@ page import="won.protocol.model.ConnectionState" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ include file="/WEB-INF/jsp/include.jsp" %>
<script type="text/javascript" src="/owner/scripts/jquery-1.8.3.min.js" ></script>

<h2>Connections: </h2>
<table border="1">

    <tr>
        <th>Remote Need</th>
        <th>Actions</th>
        <th>Feedback</th>
    </tr>
    <c:forEach var="connection" items="${connections}" varStatus="i">
        <c:set var="remoteNeed" value="${remoteNeeds.get(i.index)}" />
        <tr>
            <td>
                <DL>

                        <c:if test="${not empty remoteNeed.basicNeedType }">
                            <DT>Type:</DT>
                            <DD>${remoteNeed.basicNeedType}</DD>
                        </c:if>
                        <c:if test="${not empty remoteNeed.title}">
                            <DT>Title:</DT>
                            <DD>${remoteNeed.title}</DD>
                        </c:if>
                        <c:if test="${not empty remoteNeed.textDescription}">
                            <DT>Description:</DT>
                            <DD>${remoteNeed.textDescription}</DD>
                        </c:if>
                        <c:if test="${not empty remoteNeed.tags}">
                            <DT>Tags:</DT>
                            <DD>${remoteNeed.tags}</DD>
                        </c:if>

                    <DT>State:</DT><DD>${connection.state}</DD>
                </DL>
                <a href="/owner/connection/${connection.id}" target="_parent" > connection (in owner app)</a> <br>
                <a href="${remoteNeed.needURI}" target="_blank"> remote need data</a> <br>
                <a href="${connection.connectionURI}" target="_blank"> connection data</a> <br>
            </td>
            <td>
                <c:set var="SUGGESTED" value="<%=ConnectionState.SUGGESTED%>"/>
                <c:set var="CLOSED" value="<%=ConnectionState.CLOSED%>"/>
                <c:set var="REQUEST_RECEIVED" value="<%=ConnectionState.REQUEST_RECEIVED%>"/>
                <c:set var="REQUEST_SENT" value="<%=ConnectionState.REQUEST_SENT%>"/>
                <c:set var="CONNECTED" value="<%=ConnectionState.CONNECTED%>"/>
                <c:choose>
                    <c:when test="${connection.state eq REQUEST_RECEIVED}">
                        <button onclick="jQuery.post('/owner/connection/${connection.id}/accept', function() { document.location.reload(true); });" >accept</button>
                        <button onclick="jQuery.post('/owner/connection/${connection.id}/deny', function() { document.location.reload(true); });" >deny</button>
                    </c:when>
                    <c:when test="${connection.state eq REQUEST_SENT}">
                        <button onclick="jQuery.post('/owner/connection/${connection.id}/close', function() { document.location.reload(true); });" >close</button>
                    </c:when>
                    <c:when test="${connection.state eq CONNECTED}">
                        <button onclick="jQuery.post('/owner/connection/${connection.id}/close', function() { document.location.reload(true); });" >close</button>
                    </c:when>
                    <c:when test="${connection.state eq CLOSED}">
                        <button onclick="jQuery.post('/owner/connection/${connection.id}/open', function() { document.location.reload(true); });" >open</button>
                    </c:when>
                    <c:when test="${connection.state eq SUGGESTED}">
                        <button onclick="jQuery.post('/owner/connection/${connection.id}/open', function() { document.location.reload(true); });" >open</button>
                    </c:when>
                    <c:otherwise>
                        no actions available in this connection state (${connection.state}).
                    </c:otherwise>
                </c:choose>
            </td>
            <td>
                <form method="POST" action="/owner/connection/${connection.id}/feedback">
                    <input type="hidden" name="feedback" value="true" />
                    <input type="submit" value="Good Match!"/>
                </form>
                <form method="POST" action="/owner/connection/${connection.id}/feedback">
                    <input type="hidden" name="feedback" value="true" />
                    <input type="submit" value="Bad Match!"/>
                </form>
            </td>
        </tr>
    </c:forEach>
</table>

