<%@page import="grails.util.GrailsConfig"
%><!DOCTYPE html>

<html>
 <head>
  <title>Upload DICOM Scans</title>
  <link type="text/css" rel="stylesheet" href="${resource(dir:'css',file:'upload.css')}">
  <jq:resources/>
  <jqui:resources/>
  <dori:favicon/>
  <g:javascript src="errorBox.js"/>
  <g:javascript src="progressFavIcon.js"/>
  <g:javascript src="upload/jquery.textchange.js"/>
  <g:javascript src="upload/upload-wizard.js"/>
  <g:javascript src="upload/upload-start.js"/>
  <g:javascript src="upload/upload-deid.js"/>
  <g:javascript src="upload/upload-send.js"/>
  <g:javascript>Uploader.setEFIDIssuerURL("${createLink(controller:'efid', action:'issue') }");
Uploader.setEFIDVerifyierURL("${createLink(controller:'efid', action:'verify')}");
Uploader.setSessionId("<%=session.getId()%>");
jQuery(function() { new Uploader('#uploader'); });</g:javascript>
 </head>
 <body>
  <noscript>
   <div class="ui-widget">
    <div class="ui-state-error ui-corner-all" style="padding: 0 .7em;"> 
     <p><span class="ui-icon ui-icon-alert" style="float: left; margin-right: .3em;"></span> 
      <strong>JavaScript is required.</strong></p>
     <p>Please enable JavaScript in your browser and reload this page.</p>
    </div>
   </div>
  </noscript>
  <div id="uploader"></div>
  <div id="hipaa" style="display:none;">
   <!-- <img style="float:right;" src="${resource(dir:'images', file:'ef_logo_small.png') }" alt="EyesFirst"> -->

   <h1>EyesFirst Image Analysis Tool</h1>

   <p>The EyesFirst Image Analysis Tool is a research prototype that performs
   automated image analysis of Optical Coherence Tomography (OCT) retinal scans
   to identify signs of Diabetic Retinopathy, specifically abnormal retinal
   thickness and presence of hard exudates. The purpose of this prototype is to
   evaluate the performance of the automated image analysis algorithms. As a
   research prototype, it is not intended to make a diagnosis nor serve as a
   diagnostic aid.</p>

   <p>All Protected Health Information (PHI) must be removed from the data
   uploaded to the EyesFirst server to comply with the <a
   href="http://www.hhs.gov/ocr/privacy/hipaa/administrative/privacyrule/index.html">HIPAA
   Privacy Rule</a> and EyesFirst policy. While the data upload process attempts to verify that the data being uploaded is de-identified, you (as the owner of the data) must review the data and confirm that it does not contain PHI.

  </div>
 </body>
</html>