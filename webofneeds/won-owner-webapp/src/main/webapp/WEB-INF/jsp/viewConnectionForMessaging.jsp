<%--
  ~ Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
  ~
  ~    Licensed under the Apache License, Version 2.0 (the "License");
  ~    you may not use this file except in compliance with the License.
  ~    You may obtain a copy of the License at
  ~
  ~        http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~    Unless required by applicable law or agreed to in writing, software
  ~    distributed under the License is distributed on an "AS IS" BASIS,
  ~    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~    See the License for the specific language governing permissions and
  ~    limitations under the License.
  --%>

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

<form:form method="POST" action="/owner/connection/${connection.id}/msg/send">
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

<button onclick="jQuery.post('/owner/connection/${connection.id}/msg/close', function() { document.location.reload(true); });" >close</button>
<div class="messages">
    <iframe id="iMessages" class="cframe"  width="100%"  height="300px" src="/owner/connection/<c:out value="${connection.id}" />/body"></iframe>
</div>


<script type="text/javascript" src="/owner/scripts/jquery-1.8.3.min.js" ></script>
<script type="text/javascript">
    var refresh = function(){
        document.getElementById("iMessages").contentDocument.location.reload(true);
        setTimeout(refresh, 2000);
    }
    setTimeout(refresh, 2000);
</script>
</body>
</html>