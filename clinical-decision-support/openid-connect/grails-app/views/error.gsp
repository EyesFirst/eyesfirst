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
		<title><g:if env="development">Grails Runtime Exception</g:if><g:else>Error</g:else></title>
		<meta name="layout" content="main">
		<g:if env="development"><link rel="stylesheet" href="${resource(dir: 'css', file: 'errors.css')}" type="text/css"></g:if>
	</head>
	<body>
		<g:if env="development">
			<g:renderException exception="${exception}" />
			<h2>Filter chain</h2>
			<ul>
<%
// Render the entire exception
exception.stackTrace.each {
	if (it.methodName == "doFilter" && it.className != "org.springframework.security.web.FilterChainProxy\$VirtualFilterChain" && it.className != "org.apache.catalina.core.ApplicationFilterChain" && it.className != "org.springframework.web.filter.OncePerRequestFilter") {
		out << "<li>" << it.className << "</li>"
	}
}
%>
			</ul>
			<h2>Call Stack</h2>
			<ul>
<%
// This is the stupidest thing
try {
	throw new RuntimeException()
} catch (RuntimeException e) {
	e.stackTrace.each {
		out << "<li>" << it << "</li>";
	}
}
%>
			</ul>
		</g:if>
		<g:else>
			<ul class="errors">
				<li>An error has occurred</li>
			</ul>
		</g:else>
	</body>
</html>
