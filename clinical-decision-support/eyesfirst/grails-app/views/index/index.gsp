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
--%><!DOCTYPE html>

<html>
	<head>
		<title>EyesFirst</title>
		<link rel="shortcut icon" href="${resource(dir: 'images', file: 'favicon.ico')}" type="image/x-icon">
		<link rel="apple-touch-icon" href="${resource(dir: 'images', file: 'apple-touch-icon.png')}">
		<link rel="apple-touch-icon" sizes="114x114" href="${resource(dir: 'images', file: 'apple-touch-icon-retina.png')}">
		<r:require modules="eyesfirst"/>
		<r:layoutResources/>
		<r:script>$(function() { new EyesFirst("body").start(); $("body>p.loading").remove() });</r:script>
	</head>
	<body>
		<noscript> 
			<strong>JavaScript is required for EyesFirst.</strong> Please enable JavaScript, and then reload this page.
		</noscript>
		<script type="text/javascript"><!--
		document.writeln('<p class="loading">Loading...</p>');
		//--></script>
		<r:layoutResources/>
	</body>
</html>
