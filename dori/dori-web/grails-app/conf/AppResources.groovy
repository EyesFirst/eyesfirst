/*
 * Copyright 2013 The MITRE Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
modules = {
	application {
	}
	common {
		dependsOn "jquery"
		resource url:"js/util.js"
		resource url:"js/errorBox.js"
		resource url:"js/jquery.favicon.js"
	}
	flot {
		// Flot doesn't define its own resources, so do it for it
		dependsOn 'jquery'
		resource url:[plugin: 'flot', dir:'js/flot', file:"jquery.flot.min.js"], disposition:'head', nominify: true
	}
	"flot-symbol" {
		dependsOn 'flot'
		resource url:[plugin: 'flot', dir:'js/flot', file:"jquery.flot.symbol.min.js"], disposition:'head', nominify: true
	}
	uploader {
		dependsOn 'jquery, jquery-ui, common'
		resource url:"css/upload.css"
		resource url:"js/upload/jquery.textchange.js"
		resource url:"js/upload/upload-wizard.js"
		resource url:"js/upload/upload-start.js"
		resource url:"js/upload/upload-deid.js"
		resource url:"js/upload/upload-send.js"
	}
	scanViewerCSS {
		resource url:'css/scanViewer.less',attrs:[rel: "stylesheet/less", type:'css']
	}
	scanViewer {
		dependsOn 'jquery, jquery-ui, flot, flot-symbol, common, scanViewerCSS'
		resource url:"js/flot.label.js"
		resource url:"js/jquery.xyslider.js"
		resource url:"js/scanViewer/scanViewer.js"
		resource url:"js/scanViewer/unprocessedScan.js"
		resource url:"js/scanViewer/processedScan.js"
		resource url:"js/scanViewer/hardExudates.js"
		resource url:"js/scanViewer/sliceManager.js"
		resource url:"js/scanViewer/sliceManagerCanvas.js"
		// Experimental, not used:
		//resource url:"scanViewer/sliceManagerWebGL.js"
		resource url:"js/scanViewer/annotations.js"
	}
}