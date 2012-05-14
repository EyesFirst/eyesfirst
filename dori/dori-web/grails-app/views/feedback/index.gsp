<!DOCTYPE html>

<html>
 <head>
  <title>Feedback for Image Classifier</title>
  <link type="text/css" rel="stylesheet" href="${resource(dir:'css',file:'scanViewer.css')}">
  <dori:favicon/>
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