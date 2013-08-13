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

import java.util.Date;

import org.dcm4che2.data.DicomElement;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.VR;

public class DicomMetadataUtil {
	private DicomMetadataUtil() {
	}

	/**
	 * Attempts to find the aspect ratio for a DICOM file. Uses the PixelSpacing
	 * value in SharedFunctionalGroupsSequence if available, otherwise returns
	 * an aspect ratio of width/height, unless that's not available either, in
	 * which case it returns 1.0.
	 * 
	 * @param dicom
	 *            the dicom object to find the aspect ratio of
	 * @return the aspect ratio, a ratio between the width and height of the
	 *         final image
	 */
	public static double findAspectRatio(DicomObject dicom) {
		// First, grab the width and height
		DicomElement element = dicom.get(Tag.Columns);
		if (element == null) {
			return 1.0;
		}
		int width = element.getInt(true);
		element = dicom.get(Tag.Rows);
		if (element == null) {
			return 1.0;
		}
		int height = element.getInt(true);
		element = dicom.get(Tag.SharedFunctionalGroupsSequence);
		if (element != null) {
			// Element is a sequence
			DicomObject so = element.getDicomObject();
			DicomElement pixelMS = so.get(Tag.PixelMeasuresSequence);
			if (pixelMS != null) {
				so = pixelMS.getDicomObject();
				DicomElement pixelS = so.get(Tag.PixelSpacing);
				if (pixelS != null) {
					double[] spacing = pixelS.getDoubles(false);
					if (spacing.length >= 2)
						return (spacing[1] * width) / (spacing[0] * height);
				}
			}
		}
		return (double)width/(double)height;
	}

	/**
	 * Get the content date out of a DICOM object.
	 * @param object the object to get
	 * @return the date
	 */
	public static Date getContentDate(DicomObject object) {
		try {
			DicomElement contentDate = object.get(Tag.ContentDate);
			if (contentDate == null || !contentDate.vr().equals(VR.DA))
				return null;
			Date date = contentDate.getDate(true);
			DicomElement contentTime = object.get(Tag.ContentTime);
			if (contentTime != null && contentTime.vr().equals(VR.TM)) {
				// Grab the time, and increase the date using that.
				date = new Date(date.getTime() + contentTime.getDate(true).getTime());
			}
			return date;
		} catch (NumberFormatException e) {
			// This can happen with bad DICOM files that store something that isn't a date in the date fields
			return null;
		}
	}

	public static String getLaterality(DicomObject object) {
		DicomElement laterality = object.get(Tag.Laterality);
		if (laterality == null) {
			// OK, not that one, how about ImageLaterality?
			laterality = object.get(Tag.ImageLaterality);
			if (laterality == null) {
				// Frame laterality?
				laterality = object.get(Tag.FrameLaterality);
				if (laterality == null)
					return null;
			}
		}
		return laterality.getString(object.getSpecificCharacterSet(), false);
	}

	public static Date getBirthday(DicomObject object) {
		DicomElement birthday = object.get(Tag.PatientBirthDate);
		return birthday == null ? null : birthday.getDate(false);
	}

	public static String getGender(DicomObject object) {
		DicomElement gender = object.get(Tag.PatientSex);
		if (gender == null) {
			return null;
		}
		return gender.getString(object.getSpecificCharacterSet(), false);
	}
}
