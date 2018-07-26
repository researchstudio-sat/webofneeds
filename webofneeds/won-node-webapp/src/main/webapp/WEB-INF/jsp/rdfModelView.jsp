<%@ page import="org.apache.jena.rdf.model.Model" %>
<%@ page import="java.io.StringWriter" %>
<%@ page import="org.springframework.web.util.HtmlUtils" %>
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
    <h2>This page describes the resource with this URI: ${resourceURI}</h2>
    <div style="background-color:#C9C9E0">
    <pre>
<%
        Model model = (Model) request.getAttribute("rdfModel");
        StringWriter stringWriter = new StringWriter();
        model.write(stringWriter,"TURTLE");
        //String escapedTurtle = HtmlUtils.htmlEscape(stringWriter.toString());
        String htmlTurtle = stringWriter.toString().replaceAll("<([^>\\s]+)>","<a href=\"$1\">&lt;$1&gt;</a>");
        out.print(htmlTurtle);
%>
    </pre>
    </div>
    <hr>
<a href="${dataURI}">Machine-readable form</a> (with content negotiation via Accept: header)
</body>
</html>