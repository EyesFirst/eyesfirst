<!DOCTYPE html>
<%--
Copyright 2013 The MITRE Corporation

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
--%>
<html>
<head>
    <title>Grails Runtime Exception</title>
    <style type="text/css">
    .message {
        border: 1px solid black;
        padding: 5px;
        background-color: #E9E9E9;
    }

    .stack {
        border: 1px solid black;
        padding: 5px;
        overflow: auto;
        height: 300px;
    }

    .snippet {
        padding: 5px;
        background-color: white;
        border: 1px solid black;
        margin: 3px;
        font-family: courier;
    }
    </style>
</head>

<body>
<h1>Grails Runtime Exception</h1>
<h2>Error Details</h2>
<!--
<div class="message">
    <strong>Error ${request.'javax.servlet.error.status_code'}:</strong> ${request.'javax.servlet.error.message'.encodeAsHTML()}<br/>
    <strong>Servlet:</strong> ${request.'javax.servlet.error.servlet_name'}<br/>
    <strong>URI:</strong> ${request.'javax.servlet.error.request_uri'}<br/>
    <g:if test="${exception}">
        <strong>Exception Message:</strong> ${exception.message?.encodeAsHTML()} <br/>
        <strong>Caused by:</strong> ${exception.cause?.message?.encodeAsHTML()} <br/>
        <strong>Class:</strong> ${exception.className} <br/>
        <strong>At Line:</strong> [${exception.lineNumber}] <br/>
        <strong>Code Snippet:</strong><br/>
        <div class="snippet">
            <g:each var="cs" in="${exception.codeSnippet}">
                ${cs?.encodeAsHTML()}<br/>
            </g:each>
        </div>
    </g:if>
</div>
<g:if test="${exception}">
    <h2>Stack Trace</h2>
    <div class="stack">
        <pre><g:each in="${exception.stackTraceLines}">${it.encodeAsHTML()}<br/></g:each></pre>
    </div>
</g:if>
-->
</body>
</html>