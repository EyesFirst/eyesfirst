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
  <title>Import Scans</title>
  <dori:favicon/>
 </head>
 <body>
 <g:if test="${importedEFID}"><p style="size:120%;font-weight:bold;">Successfully imported scan for EFID ${importedEFID}.</p></g:if>
 <g:if test="${solrError}"><p>${solrError}</p></g:if>
  <h1>Import Scans</h1>
  <p>This page allows you to import a scan that was exported from another
  instance of DoRI.</p>
  <g:uploadForm controller="importDori" action="upload" >
   <p>Import from: <input type="file" name="importedFile"></p>
   <p><input type="submit" value="Import">
  </g:uploadForm>
 </body>
</html>