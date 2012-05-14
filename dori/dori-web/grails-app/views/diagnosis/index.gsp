<!DOCTYPE html>

<html>
 <head>
  <title>Human Clinical Reviewer Diagnoses</title>
  <link type="text/css" rel="stylesheet" href="${resource(dir:'css',file:'scanViewer.css')}">
  <dori:favicon/>
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