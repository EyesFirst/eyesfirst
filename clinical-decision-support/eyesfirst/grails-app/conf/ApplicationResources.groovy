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
		resource url:'js/application.js'
	}
	eyesfirst {
		dependsOn 'jquery'
		resource url:'css/eyesfirst.less',attrs:[rel: "stylesheet/less", type:'css']
		resource url:'js/jquery.mousedrag.js'
		resource url:'js/dates.js'
		resource url:'js/animation.js'
		resource url:'js/eyesfirst.js'
		resource url:'js/patientList.js'
		resource url:'js/artifactViewer.js'
		resource url:'js/timeline.js'
		resource url:'js/oct.js'
		resource url:'js/popup.js'
	}
	common {
		dependsOn "jquery"
		resource url:"js/util.js"
		resource url:"js/errorBox.js"
		resource url:"js/jquery.favicon.js"
	}
	uploader {
		dependsOn 'jquery, jquery-ui, common'
		resource url:"css/upload.less",attrs:[rel: "stylesheet/less", type:'css']
		resource url:"js/upload/jquery.textchange.js"
		resource url:"js/upload/upload-wizard.js"
		resource url:"js/upload/upload-start.js"
		resource url:"js/upload/upload-deid.js"
		resource url:"js/upload/upload-send.js"
	}
}