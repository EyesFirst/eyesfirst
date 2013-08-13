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

 // Place your Spring DSL code here
beans = {
	secureRandom(java.security.SecureRandom)
	actualDicomAccessService(org.mitre.eyesfirst.dicom.image.WADOService, "${application.config.eyesfirst.wadoURL}")
	dicomAccessService(org.mitre.eyesfirst.dicom.image.CachingDicomAccessService, actualDicomAccessService) { bean ->
		bean.destroyMethod = 'destroy'
	}
	dicomUploadService(org.mitre.eyesfirst.dicom.DcmSendDicomUploadService, "DCM4CHEE@${application.config.eyesfirst.dcm4cheeHost}:${application.config.eyesfirst.dcm4cheePort}")
	solrService(org.mitre.eyesfirst.solr.SimpleSolrService, "${grailsApplication.config.eyesfirst.solrUpdate}")
}
