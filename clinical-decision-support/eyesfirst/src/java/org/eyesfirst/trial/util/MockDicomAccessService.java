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
package org.eyesfirst.trial.util;

import java.util.HashMap;
import java.util.Map;

import org.mitre.eyesfirst.dicom.DicomID;
import org.mitre.eyesfirst.dicom.DicomImage;
import org.mitre.eyesfirst.dicom.image.DicomAccessException;
import org.mitre.eyesfirst.dicom.image.DicomAccessService;

/**
 * Basic system that permanently caches DICOM images to allow testing.
 * @author dpotter
 */
public class MockDicomAccessService implements DicomAccessService {
	private final Map<DicomID, DicomImage> images = new HashMap<DicomID, DicomImage>();

	public MockDicomAccessService() {
	}

	public synchronized void addDicomImage(DicomImage image) {
		images.put(image.getDicomID(), image);
	}

	@Override
	public void destroy() {
		// Does nothing
	}

	@Override
	public synchronized DicomImage retrieveDicomObject(String studyUID, String seriesUID,
			String objectUID)
			throws DicomAccessException {
		return images.get(new DicomID(studyUID, seriesUID, objectUID));
	}

}
