<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ include file="/WEB-INF/jsp/include.jsp" %>

<script type="text/javascript" src="/owner/scripts/jquery-1.8.3.min.js" ></script>
<h2>Matches: </h2>
<table border="1">
    <tr>
        <th>Remote Need</th>
        <th>Score</th>
        <th>Connect</th>
        <th>Feedback</th>
    </tr>
    <c:forEach var="match" items="${matches}" varStatus="i">
        <tr>
            <td>
                <DL>
                    <c:set var="remoteNeed" value="${remoteNeeds[i.index]}" />
                    <c:if test="${remoteNeed.basicNeedType != null}">
                        <DT>Type:</DT>
                        <DD>${remoteNeed.basicNeedType}</DD>
                    </c:if>
                    <c:if test="${remoteNeed.title != null}">
                        <DT>Title:</DT>
                        <DD>${remoteNeed.title}</DD>
                    </c:if>
                    <c:if test="${remoteNeed.contentDescription != null}">
                        <DT>Description:</DT>
                        <DD>${remoteNeed.contentDescription}</DD>
                    </c:if>
                    <c:if test="${remoteNeed.tags != null}">
                        <DT>Tags:</DT>
                        <DD>${remoteNeed.tags}</DD>
                    </c:if>
                </DL>
                <a href="${match.toNeed}" target="_blank"> visit </a>
            </td>
            <td>${match.score}</td>
            <td>
                <button onclick="jQuery.post('/owner/need/match/${match.id}/connect');" >connect</button>
            </td>
        </tr>
    </c:forEach>
</table>