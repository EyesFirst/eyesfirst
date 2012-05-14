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
package org.eyesfirst.dori

import grails.plugins.springsecurity.Secured
import grails.util.Environment

@Secured ('IS_AUTHENTICATED_ANONYMOUSLY')
class InfoController {

	def springSecurityService

	static boolean iDORI
	static {
		boolean iDORI = System.getProperty('iDORI', 'true').toBoolean()
		System.out.println("Global iDORI is $iDORI (default=true; set a java system property to change)");
	}

	def globals = {
		def thisEnv = Environment.current.name

		render(contentType: "text/json") {
			internal = iDORI;
			solrCoreUrl = '';
			solrCoreRequestHandler = 'solr.groovy';
			octScanViewerUrl = grails.util.GrailsConfig['eyesfirst.octScanViewerURL'];
			diagnosisUrl = '/doriweb/diagnosis?rawQueryString=';
			feedbackUrl = '/doriweb/feedback?processedQueryString=';
		}
	}
}