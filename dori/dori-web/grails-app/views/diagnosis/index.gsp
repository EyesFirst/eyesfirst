<!DOCTYPE html>
<%--
Copyright 2012 The MITRE Corporation

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
  <title>Human Clinical Reviewer Diagnoses</title>
  <r:require modules="scanViewerCSS"/>
  <dori:favicon/>
  <r:layoutResources/>
 </head>
 <body>
  <header><h1>Human Clinical Reviewer Diagnoses</h1></header>
  <div style="margin: 1em">
  <table class="results-table">
   <tr>
    <th>Reviewer</th>
    <th>Abnormal Thickness</th>
    <th>Hard Exudates</th>
    <th>Microaneurysms</th>
    <th>Neovascularization</th>
    <th>Notes</th>
   </tr>
   <g:each in="${diagnoses}" var="diagnosis">
    <tr>
     <td>${diagnosis.reviewer?.username?.encodeAsHTML()}</td>
     <td><dori:feedbackIcon type="diagnosis" value="${diagnosis.abnormalRetinalThickness}"/></td>
     <td><dori:feedbackIcon type="diagnosis" value="${diagnosis.hardExudates}"/></td>
     <td><dori:feedbackIcon type="diagnosis" value="${diagnosis.microaneurysms}"/></td>
     <td><dori:feedbackIcon type="diagnosis" value="${diagnosis.neovascularization}"/></td>
     <td>${diagnosis.notes?.encodeAsHTML()}</td>
    </tr>
   </g:each>
  </table>
  </div>
 </body>
</html>