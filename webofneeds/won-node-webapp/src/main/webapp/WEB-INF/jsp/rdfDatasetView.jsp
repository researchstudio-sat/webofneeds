<%@ page import="com.hp.hpl.jena.query.Dataset" %>
<%@ page import="org.apache.jena.riot.RDFDataMgr" %>
<%@ page import="org.apache.jena.riot.RDFFormat" %>
<%@ page import="java.io.StringWriter" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.regex.Pattern" %>
<%--
~ Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
~
~ Licensed under the Apache License, Version 2.0 (the "License");
~ you may not use this file except in compliance with the License.
~ You may obtain a copy of the License at
~
~    http://www.apache.org/licenses/LICENSE-2.0
~
~ Unless required by applicable law or agreed to in writing, software
~ distributed under the License is distributed on an "AS IS" BASIS,
~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
~ See the License for the specific language governing permissions and
~ limitations under the License.
--%>

<%--
  Created by IntelliJ IDEA.
  User: fkleedorfer
  Date: 27.11.12
  Time: 12:48
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c"%>
<html>
<head>
    <title>Linked Data Page View</title>
</head>
<body>
    <h1>This is linked data. Don't Panic!</h1>
    <h2>This page describes the resource with this URI: ${resourceURI}</h2>

    Description of the Resource (human readable):
    <div style="background-color:#C9C9E0">
    <pre>
<%
        Dataset dataset = (Dataset) request.getAttribute("rdfDataset");
        StringWriter stringWriter = new StringWriter();
        RDFDataMgr.write(stringWriter, dataset, RDFFormat.TRIG.getLang());
        //String escapedTurtle = HtmlUtils.htmlEscape(stringWriter.toString());
        String htmlTurtle = stringWriter.toString().replaceAll("<([^>]+)>","<a href=\"$1\">&lt;$1&gt;</a>");
        //convert all prefixed URIs to links
        Map<String, String> prefixes = dataset.getDefaultModel().getNsPrefixMap();
        for(String key: prefixes.keySet()) {
            String value = prefixes.get(key);
            htmlTurtle = htmlTurtle.replaceAll("("+Pattern.quote(key)+":(\\w+))", "<a href=\"" + value + "$2\">$1</a>");
        }
        out.print(htmlTurtle);
%>
    </pre>
    </div>
    <hr>
If you can handle it, you may want to see the <a href="${dataURI}">machine-readable form</a>
</body>
</html>