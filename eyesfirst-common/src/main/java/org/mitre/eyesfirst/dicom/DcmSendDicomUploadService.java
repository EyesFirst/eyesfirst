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
package org.mitre.eyesfirst.dicom;

import java.io.File;

import org.mitre.eyesfirst.common.DicomSend;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DcmSendDicomUploadService implements DicomUploadService {
	private final String deviceName;
	private final String endpointURL;
	private final Logger log = LoggerFactory.getLogger(getClass());

	public DcmSendDicomUploadService(String endpointURL) {
		this(endpointURL, null);
	}
	public DcmSendDicomUploadService(String endpointURL, String deviceName) {
		if (endpointURL == null)
			throw new NullPointerException();
		this.deviceName = deviceName == null ? "EYESFIRST" : deviceName;
		this.endpointURL = endpointURL;
		log.info("DcmSend Upload Service: {} to {}", this.deviceName, this.endpointURL);
	}

	@Override
	public void uploadDicomFile(File file) throws Exception {
		if (log.isDebugEnabled()) {
			log.debug("DcmSend: Send " + file.getPath() + " from " + deviceName + " to " + endpointURL);
		}
		DicomSend send = new DicomSend(deviceName);
		send.setDicomURL(endpointURL);
		send.sendFile(file);
	}

}
