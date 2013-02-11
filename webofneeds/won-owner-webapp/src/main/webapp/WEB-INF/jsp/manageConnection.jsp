<%--
  Created by IntelliJ IDEA.
  User: gabriel
  Date: 11.02.13
  Time: 11:04
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ include file="/WEB-INF/jsp/include.jsp" %>

<script type="text/javascript" src="/owner/scripts/jquery-1.8.3.min.js" ></script>
<button onclick="jQuery.post('/owner/connection/${connection.id}/accept', function() { document.location.reload(true); });" >accept</button>
<button onclick="jQuery.post('/owner/connection/${connection.id}/deny', function() { document.location.reload(true); });" >deny</button>