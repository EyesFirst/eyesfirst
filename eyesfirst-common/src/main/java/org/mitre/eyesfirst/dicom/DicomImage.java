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

import java.awt.image.BufferedImage;

import org.dcm4che2.data.DicomObject;

/**
 * An interface for access imagery data from a DICOM image.
 * @author dpotter
 */
public interface DicomImage {
	/**
	 * Get the DicomObject from the image, if possible. If this
	 * {@code DicomImage} was generated using DCM4CHE2, this may return
	 * {@code null}.
	 * 
	 * @return
	 * @throws DicomImageException
	 */
	public DicomObject getDicomObject() throws DicomImageException;

	/**
	 * Get the DICOM ID stored in the image.
	 * 
	 * @return the DICOM ID
	 */
	public DicomID getDicomID();

	/**
	 * Gets the last modified time, if possible. If not possible, return
	 * {@link Long#MIN_VALUE}. (Since it's safe to say that, should a later
	 * version offer that data, it will certainly be after 292 million years
	 * ago. (Roughly.))
	 * 
	 * @return the last modified time, or {@link Long#MIN_VALUE} if it isn't
	 *         known
	 */
	public long getLastModifiedTime();

	/**
	 * Create a BufferedImage of the given slice, if possible. This method
	 * should never return {@code null} if a slice cannot be read, instead it
	 * should raise an appropriate exception to indicate the error that prevents
	 * the slice from being rendered to an image.
	 * 
	 * @param slice
	 *            the 0-based index of the slice to get
	 * @return the slice as an image
	 * @throws DicomImageException
	 *             if an error prevents the image from being read
	 */
	public BufferedImage getSlice(int slice) throws DicomImageException;

	/**
	 * Gets the number of slices in the image, if known. If the number of slices
	 * can't be determined, then it's likely that attempts to get the actual
	 * slices via {@link #getSlice(int)} will fail.
	 * 
	 * @return the number of slices contained in the image, or 0 if not known
	 */
	public int getSliceCount();

	/**
	 * Get the appropriate aspect ratio to use to display this image.
	 * @return the appropriate aspect ratio to use to display this image
	 */
	public double getAspectRatio();
}
