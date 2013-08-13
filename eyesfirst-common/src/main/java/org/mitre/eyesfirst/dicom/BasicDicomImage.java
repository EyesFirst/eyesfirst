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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;

import org.dcm4che2.data.ConfigurationError;
import org.dcm4che2.data.DicomElement;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.imageio.ImageReaderFactory;
import org.dcm4che2.imageio.plugins.dcm.DicomStreamMetaData;
import org.dcm4che2.io.DicomInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BasicDicomImage implements DicomImage {
	private static final Logger log = LoggerFactory.getLogger(BasicDicomImage.class);
	static {
		//DCM4CHE2Util.setImageIOSettings();
		ImageIO.scanForPlugins();
		// Check if DicomImageReader is available
		Iterator<ImageReader> iter = ImageIO.getImageReadersByFormatName("DICOM");
		if (!iter.hasNext()) {
			log.error("Unable to create a DicomImageReader!");
		}
	}
	private final ImageReader reader;
	private final DicomObject dicom;
	private long lastModified = Long.MIN_VALUE;
	private int slices = 0;

	public static ImageReader getDicomImageReader() {
		Iterator<ImageReader> iter = ImageIO.getImageReadersByFormatName("DICOM");
		if (!iter.hasNext()) {
			throw new RuntimeException("Unable to get the DICOM image reader!");
		}
		return iter.next();
	}

	public BasicDicomImage(byte[] dicom) throws IOException, DicomImageException {
		if (dicom == null)
			throw new NullPointerException("dicom is null");
		// Create an image reader for that data
		ImageReader reader = getDicomImageReader();
		reader.setInput(ImageIO.createImageInputStream(new ByteArrayInputStream(dicom)));
		// Yes, that's right, they didn't even ATTEMPT to map any meta data onto the ImageIO APIs
		DicomObject metadata = ((DicomStreamMetaData)(reader.getStreamMetadata())).getDicomObject();
		// See whether or not we're going to bother with the reader long-term
		String transferSyntax = metadata.getString(Tag.TransferSyntaxUID);
		try {
			ImageReaderFactory.getInstance().getReaderForTransferSyntax(transferSyntax);
		} catch (ConfigurationError e) {
			log.debug("Using ImageIO on encapsulated images to load DICOM images");
			reader = null;
		} catch (UnsupportedOperationException e) {
			log.debug("Using DCM4CHE to load DICOM images");
			// Ignore this, it's how DCM4CHE internally signals it should process
			// the data
		}
		this.reader = reader;
		if (reader == null) {
			// We're going to ignore the reader, which means regenerating the DICOM
			metadata = new DicomInputStream(new ByteArrayInputStream(dicom)).readDicomObject();
		}
		this.dicom = metadata;
		Date date = DicomMetadataUtil.getContentDate(metadata);
		if (date != null) {
			lastModified = date.getTime();
		}
		DicomElement frames = metadata.get(Tag.NumberOfFrames);
		if (frames != null) {
			slices = frames.getInt(false);
		}
	}

	public BasicDicomImage(InputStream in) throws IOException, DicomImageException {
		this(readFully(in));
	}

	private static byte[] readFully(InputStream in) throws IOException {
		ByteArrayOutputStream buf = new ByteArrayOutputStream(10*1024*1024);
		byte[] b = new byte[1024*8];
		while (true) {
			int r = in.read(b);
			if (r < 0)
				break;
			buf.write(b, 0, r);
		}
		return buf.toByteArray();
	}

	public DicomObject getDicomObject() {
		return dicom;
	}

	@Override
	public DicomID getDicomID() {
		// TODO (maybe): Cache this
		return new DicomID(dicom);
	}

	@Override
	public long getLastModifiedTime() {
		return lastModified;
	}

	/**
	 * Read a given image. This method is synchronized because the underlying
	 * image reader isn't thread safe, and creating multiple image readers is
	 * incredibly slow and inefficient.
	 * 
	 * @param slice
	 *            the slice to read
	 * @return the image that represents that slice
	 * @throws DicomImageException
	 *             if an error occurred while loading the slice
	 */
	@Override
	public synchronized BufferedImage getSlice(int slice) throws DicomImageException {
		if (this.reader == null) {
			// If we're here, the data is (hopefully) encapsulated, and we're going
			// to ignore DCM4CHE's broken implementation where they hard-code in
			// the JNI JPEG2000 decoder
			DicomElement pixelData = dicom.get(Tag.PixelData);
			if (pixelData == null)
				throw new DicomImageException("No pixel data found");
			int max = pixelData.countItems();
			if (max < 1)
				throw new DicomImageException("DCM4CHE2 couldn't decode the data, and the image data isn't encapsulated so it can't be passed to ImageIO directly");
			if (slice > max)
				throw new DicomImageException("Bad slice " + slice + ", max is " + max);
			byte[] data = pixelData.getFragment(slice+1);
			try {
				return ImageIO.read(new ByteArrayInputStream(data));
			} catch (IOException e) {
				throw new DicomImageException(e);
			}
		} else {
			try {
				BufferedImage result = reader.read(slice);
				if (result == null)
					throw new DicomImageException("DCM4CHE could not read slice " + slice + " (ImageReader.read returned null)");
				return result;
			} catch (IOException e) {
				throw new DicomImageException(e);
			}
		}
	}

	@Override
	public int getSliceCount() {
		return slices;
	}

	/**
	 * Get the appropriate aspect ratio to use to display this image.
	 * @return the appropriate aspect ratio to use to display this image
	 */
	public double getAspectRatio() {
		return DicomMetadataUtil.findAspectRatio(dicom);
	}
}
