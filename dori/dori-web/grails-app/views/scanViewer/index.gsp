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
  <title>OCT Scan Viewer</title>
  <r:require modules="scanViewer"/>
  <dori:favicon/>
  <r:layoutResources/>
 </head>
 <body>
  <div id="dicom"></div>
  <noscript><div class="ui-widget" style="padding:1em;">
   <div class="ui-state-error ui-corner-all" style="padding: 0 .7em;"> 
    <p><span class="ui-icon ui-icon-alert" style="float: left; margin-right: .3em;"></span> 
     <strong>JavaScript is required for the viewer.</strong> Please enable JavaScript, and then reload this page.</p>
   </div>
  </div></noscript>
  <script type="text/javascript"><!--
$('#dicom').text("Loading OCT scan viewer...");
$(function() {
	DICOMViewer.setAppRoot('${g.resource().encodeAsJavaScript()}');
	new DICOMViewer('#dicom').loadFromLocation();
});
//--></script>
  <r:layoutResources/>
</body>
</html>