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
package org.mitre.eyesfirst.dicom.image;

import org.mitre.eyesfirst.dicom.DicomImage;

/**
 * Interface for accessing DICOM objects.
 * 
 * @author dpotter
 */
public interface DicomAccessService {
	/**
	 * Retrieve the DICOM data under the given name. All DICOM objects can be
	 * identified by the use of three separate UIDs: the study UID, the series
	 * UID, and the object UID. The exact meanings of these UIDs are defined by
	 * the DICOM spec. Essentially the first two help to correlate related
	 * scans, while the object UID uniquely identifies the actual DICOM object.
	 * Despite the fact that these are all "UIDs," the DICOM spec does in fact
	 * require all three and not just the object UID.
	 * 
	 * @param studyUID
	 *            the DICOM study UID
	 * @param seriesUID
	 *            the DICOM series UID
	 * @param objectUID
	 *            the DICOM object UID
	 * @return a DicomImage with the data, or {@code null} if no image could be
	 *         created
	 * @throws DicomAccessException
	 *             if an error occurs while retrieving the DICOM object
	 */
	public DicomImage retrieveDicomObject(String studyUID, String seriesUID,
			String objectUID) throws DicomAccessException;

	/**
	 * Called to destroy the service, freeing any resources.
	 */
	public void destroy();
}
