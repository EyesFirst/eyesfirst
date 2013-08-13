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
  <title>Feedback for Image Classifier</title>
  <r:require modules="scanViewerCSS"/>
  <dori:favicon/>
  <r:layoutResources/>
 </head>
 <body>
  <header><h1>Human Clinical Reviewer Feedback</h1></header>
  <div style="margin: 1em">
  <table class="results-table">
   <tr>
    <th>Reviewer</th>
    <th>Agree with Retinal Thickness Classifier</th>
    <th>Agree with Hard Exudate Classifier</th>
    <th>Notes</th>
   </tr>
   <g:each in="${feedback}" var="fb">
    <tr>
     <td>${fb.reviewer?.username?.encodeAsHTML()}</td>
     <td><dori:feedbackIcon value="${fb.affirmAbnormalRetinalThickness}"/></td>
     <td><dori:feedbackIcon value="${fb.affirmHardExudates}"/></td>
     <td>${fb.processedNotes?.encodeAsHTML()}</td>
    </tr>
   </g:each>
  </table>
  </div>
 </body>
</html>