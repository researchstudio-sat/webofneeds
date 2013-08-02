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
    <h1>New need</h1>

    <form:form method="POST" action="create">
        <input type="hidden" name="_anonymize" value="on" />
        <input type="hidden" name="deliveryMethod" value="DELIVERY_MODE_DIRECT_DOWNLOAD" />
        <table>
            <tr>
                <td><form:label path="title">Need title:</form:label></td>
                <td><form:input path="title" /></td>
            </tr>
            <tr>
                <td><form:label path="basicNeedType">Basic type:</form:label></td>
                <td><form:select path="basicNeedType">
                    <form:options items="${enumValues}" />
                </form:select></td>
            </tr>
            <tr>
                <td><form:label path="state">Need state:</form:label></td>
                <td><form:select path="state">
                    <form:options items="${enumValues}" />
                </form:select></td>
            </tr>
            <!--tr>
                <td><form:label path="anonymize">Anonymize owner:</form:label></td>
                <td><form:checkbox path="anonymize" /></td>
            </tr-->
            <tr>
                <td><form:label path="wonNode">WoN Node:</form:label></td>
                <td><form:input path="wonNode" /></td>
            </tr>
            <tr>
                <td><form:label path="textDescription">Text description:</form:label></td>
                <td><form:textarea path="textDescription" rows="4" cols="30" /></td>
            </tr>
            <tr>
                <td><form:label path="tags">Tags:</form:label></td>
                <td><form:textarea path="tags" rows="2" cols="30" /></td>
            </tr>
            <tr>
                <td><form:label path="upperPriceLimit">Upper price limit:</form:label> </td>
                <td><form:input path="upperPriceLimit" /></td>
            </tr>
            <tr>
                <td><form:label path="lowerPriceLimit">Lower price limit:</form:label> </td>
                <td><form:input path="lowerPriceLimit" /></td>
            </tr>
            <tr>
                <td><form:label path="currency">Currency</form:label> </td>
                <td><form:input path="currency" /></td>
            </tr>
            <tr>
                <td><form:label path="latitude">Latitude:</form:label></td>
                <td><form:input path="latitude" /></td>
            </tr>
            <tr>
                <td><form:label path="longitude">Longitude:</form:label></td>
                <td><form:input path="longitude" /></td>
            </tr>
            <tr>
                <td><form:label path="startTime">Start time:</form:label></td>
                <td><form:input path="startTime" /></td>
            </tr>
            <tr>
                <td><form:label path="endTime">End time:</form:label></td>
                <td><form:input path="endTime" /></td>
            </tr>
            <tr>
                <td><form:label path="recurIn">Recur in:</form:label></td>
                <td><form:input path="recurIn" /></td>
            </tr>
            <tr>
                <td><form:label path="recurTimes">Recur times:</form:label></td>
                <td><form:input path="recurTimes" /></td>
            </tr>
            <tr>
                <td><form:label path="recurInfiniteTimes">Recur inifinite times:</form:label></td>
                <td><form:checkbox path="recurInfiniteTimes" /></td>
            </tr>
            <tr>
                <td><form:label path="matchingConstraint">Matching constraint:</form:label></td>
                <td><form:textarea path="matchingConstraint" rows="10" cols="80" />
                    <br >
                    NOTES:
                    <UL>
                        <LI>TURTLE format expected</LI>
                        <LI>Only ONE constraint can be specified here</LI>
                        <LI>use the null relative URI for referring to the constraint node. It will be attached to the rest of the need graph. This looks like this: <pre>&lt;&gt; a &lt;http://purl.org/webofneeds/model#MatchingConstraint&gt; </pre></LI>
                    </UL>

                </td>
            </tr>
            <tr>
                <td><form:label path="contentDescription">Content description:</form:label></td>
                <td><form:textarea path="contentDescription" rows="10" cols="80" />
                    <br >
                    NOTES:
                    <UL>
                        <LI>TURTLE format expected</LI>
                        <LI>use the null relative URI for referring to the content node. It will be attached to the rest of the need graph. This looks like this: <pre>&lt;&gt; a &lt;http://dbpedia.org/resource/Couch&gt; </pre></LI>
                    </UL>
                </td>
            </tr>
            <tr>
                <td colspan="2"><input type="submit" value="Submit"/></td>
            </tr>
        </table>
    </form:form>
</body>
</html>