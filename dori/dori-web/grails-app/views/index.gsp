<!DOCTYPE html>
<%--
Copyright 2012, 2013 The MITRE Corporation

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
  <title>DORI</title>
  <dori:favicon/>
  <style type="text/css"><!--
body {
	font-family: "Lucida Grande", Verdana, Tahoma, sans-serif;
	font-size: 16pt;
}
h1 {
	border-bottom: solid 1px #C99;
}
  --></style>
 </head>
 <body>
  <h1>DORI</h1>
  <ul>
   <sec:ifNotLoggedIn><li><g:link controller="login">Log in / Register</g:link></li></sec:ifNotLoggedIn>
   <sec:ifLoggedIn><li><g:link controller="logout">Log out</g:link></li>
   <li><g:link controller="register" action="changePassword">Update Password</g:link></li></sec:ifLoggedIn>
   <li><g:link controller="efid">EFID Issue and Verification</g:link></li>
   <li><g:link controller="upload">Upload Scans</g:link></li>
   <li><g:link controller="importDori">Import Scans</g:link> (import an exported scan from a different instance of DoRI)</li>
   <sec:ifAllGranted roles="ROLE_ADMIN"><li><g:link controller="solrUpdate">Send SOLR Update Command</g:link></li></sec:ifAllGranted>
   <li><a href="browser/">Browser</a></li>
  </ul>
 </body>
</html>