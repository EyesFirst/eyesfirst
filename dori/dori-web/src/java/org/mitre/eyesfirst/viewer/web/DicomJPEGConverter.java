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
package org.mitre.eyesfirst.viewer.web;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.dcm4che2.data.DicomElement;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;

/**
 * Utility class for converting slices out of a DICOM object into JPEGs.
 * 
 * @author dpotter
 */
public class DicomJPEGConverter {
	/**
	 * Convert the given slice from the given object into a JPEG image.
	 * 
	 * @param object
	 *            the object to pull a slice from
	 * @param slice
	 *            the slice to pull
	 * @return the generated JPEG image
	 * @throws IndexOutOfBoundsException
	 *             if the 0-based slice index is outside the number of slices in
	 *             the image
	 * @throws IOException
	 *             if the image cannot be generated either because it could not
	 *             be loaded or could not be written
	 */
	public static byte[] convertSliceToJPEG(DicomObject object, int slice) throws IOException {
		if (slice < 0)
			throw new IndexOutOfBoundsException("Bad slice " + slice);
		// Grab the pixel data
		DicomElement pixelData = object.get(Tag.PixelData);
		if (pixelData == null)
			return null;
		if (slice >= pixelData.countItems())
			throw new IndexOutOfBoundsException("Bad slice " + slice + ", max is " + pixelData.countItems());
		byte[] data = pixelData.getFragment(slice);
		BufferedImage image = ImageIO.read(new ByteArrayInputStream(data));
		if (image == null)
			return null;
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ImageIO.write(image, "jpeg", out);
		image.flush();
		return out.toByteArray();
	}

}
