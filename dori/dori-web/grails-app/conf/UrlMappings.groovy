/*
 * Copyright 2012 The MITRE Corporation
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
class UrlMappings {

	static mappings = {
		"/$controller/$action?/$id?" {
			constraints {
				// apply constraints here
			}
		}

		"/login/$action?"(controller: "login")
		"/logout/$action?"(controller: "logout")

		"/browser/globals"(controller:"info", action:"globals")

		name root: "/"(view: "/index")

		// http://alwaysthecritic.typepad.com/atc/2011/03/grails-custom-exception-mapping.html
		// Handling specific exceptions requires a 500 code on the left for the
		// mapping to pick them up, but we can send back another code in the
		// controller that sends back the response.
		"500"(controller:"error", action:"error403", exception:org.springframework.security.access.AccessDeniedException)

		"500"(view: '/error')

		"/oct-scan-viewer/" (controller:"scanViewer")
		"/oct-scan-viewer/debug" (controller:"scanViewer", action:"debug")
		"/oct-scan-viewer/slices/$studyUID/$seriesUID/$objectUID/slices/$sliceType/$sliceNumber"(controller: "scanViewer", action:"slices")
		"/oct-scan-viewer/slices/$studyUID/$seriesUID/$objectUID/info"(controller: "scanViewer", action:"info")
		// FIXME: For the most part, these actions should all be done by methods
		// on a URL for a single annotation. Namely, POST to annotations should
		// create a new annotation, and there should be a URL for each
		// annotation used to edit and delete them.
		"/oct-scan-viewer/slices/$studyUID/$seriesUID/$objectUID/annotations"(controller: "imageAnnotation", action:"index")
		"/oct-scan-viewer/slices/$studyUID/$seriesUID/$objectUID/annotations/add"(controller: "imageAnnotation", action:"create")
		"/oct-scan-viewer/slices/$studyUID/$seriesUID/$objectUID/annotations/edit/$annotationID"(controller: "imageAnnotation", action:"update")
		"/oct-scan-viewer/slices/$studyUID/$seriesUID/$objectUID/annotations/remove/$annotationID"(controller: "imageAnnotation", action:"remove")

		"/browser/export/$efid/$imageID"(controller:"exportDori", action:"export")
		"/browser/import"(controller:"exportDori", action:"import")
	}
}
