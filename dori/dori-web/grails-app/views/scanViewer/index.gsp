<!DOCTYPE html>

<html>
 <head>
  <title>OCT Scan Viewer</title>
  <jq:resources/>
  <jqui:resources/>
  <jq:plugin name="flot"></jq:plugin>
  <link type="text/css" rel="stylesheet" href="${resource(dir:'css',file:'scanViewer.css')}">
  <dori:favicon/>
  <flot:resources includeJQueryLib="false" includeExCanvasLib="false" plugins="['symbol']"/>
  <g:javascript src="flot.label.js"/>
  <g:javascript src="errorBox.js"/>
  <g:javascript src="progressFavIcon.js"/>
  <g:javascript src="scanViewer/scanViewer.js"/>
  <g:javascript src="scanViewer/unprocessedScan.js"/>
  <g:javascript src="scanViewer/processedScan.js"/>
  <g:javascript src="scanViewer/hardExudates.js"/>
  <g:javascript src="scanViewer/sliceManager.js"/>
 </head>
<body onload="new DICOMViewer('#dicom').loadFromLocation()"> 
<div id="dicom"></div>
<noscript><div class="ui-widget" style="padding:1em;">
	<div class="ui-state-error ui-corner-all" style="padding: 0 .7em;"> 
		<p><span class="ui-icon ui-icon-alert" style="float: left; margin-right: .3em;"></span> 
		<strong>JavaScript is required for the viewer.</strong> Please enable JavaScript, and then reload this page.</p>
	</div>
</div></noscript>
</body>
</html>