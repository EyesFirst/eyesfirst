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
--%><%@page import="grails.util.GrailsConfig"
%><!DOCTYPE html>

<html>
 <head>
  <title>Upload DICOM Scans</title>
  <%--<dori:favicon/>--%>
  <r:require module="uploader"/>
  <r:layoutResources/>
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
  <g:javascript>Uploader.setEFIDIssuerURL("${createLink(controller:'efid', action:'issue') }");
Uploader.setEFIDVerifyierURL("${createLink(controller:'efid', action:'verify')}");
Uploader.setSessionId("<%=session.getId()%>");
jQuery(function() { new Uploader('#uploader'); });</g:javascript>
  <r:layoutResources/>
 </body>
</html>