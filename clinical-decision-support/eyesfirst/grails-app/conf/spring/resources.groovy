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
import grails.util.Environment

beans = {
	switch(Environment.current) {
		case Environment.DEVELOPMENT:
			actualDicomAccessService(org.eyesfirst.trial.util.MockDicomAccessService)
			break
		default:
			actualDicomAccessService(org.mitre.eyesfirst.dicom.image.WADOService, "${application.config.eyesfirst.wadoURL}")
			break
	}
	dicomAccessService(org.mitre.eyesfirst.dicom.image.CachingDicomAccessService, actualDicomAccessService) { bean ->
		bean.destroyMethod = 'destroy'
	}
	dicomUploadService(org.mitre.eyesfirst.dicom.DcmSendDicomUploadService, "DCM4CHEE@${grailsApplication.config.eyesfirst.dcm4cheeHost}:${grailsApplication.config.eyesfirst.dcm4cheePort}")
}
