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
package org.mitre.eyesfirst.dori;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.ByteOrder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageInputStream;

import org.dcm4che2.data.BasicDicomObject;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.UID;
import org.dcm4che2.data.VR;
import org.dcm4che2.image.ColorModelFactory;
import org.dcm4che2.image.LookupTable;
import org.dcm4che2.imageio.plugins.dcm.DicomStreamMetaData;
import org.dcm4che2.imageioimpl.plugins.dcm.DicomImageReaderSpi;
import org.dcm4che2.imageioimpl.plugins.dcm.DicomImageWriterSpi;
import org.dcm4che2.util.CloseUtils;
import org.mitre.eyesfirst.Uploader;

import com.sun.media.imageio.plugins.jpeg2000.J2KImageWriteParam;
import com.sun.media.imageio.stream.RawImageInputStream;

/**
 * @author David Smiley - dsmiley@mitre.org
 */
public class Zeiss2Dcm extends Dcm2Dcm {
	private static final int[] OFFSETS_0 = {0};

	private File imgFile;
	private boolean flipWidthHeight = true;
	private boolean verticalFlip = true;
	private boolean horizontalFlip = true;

	private int targetLossyCompressionRatio = 10;
	private int width;
	private int height;
	int frames;
	private int allocated;
	private int samples = 1;

	private int dataType; //DataBuffer
	private DicomObject ds;

	private static Random random = new Random();

	/**
	 * Ex: P20100416133423953_Macular Cube 512x128_3-4-2010_10-41-25_OS_sn0195_cube_raw.img
	 */
	public static String getPartialImgNameFromDcm(DicomObject dco) {
		// This is the time stamp string, based on the acquisition time
		// information. The full time stamp format is "M-d-yyyy_H-m-s" - and
		// those really are supposed to be non-zero padded numbers.
		String timeStamp;
		// There are two ways the acquisition time can be stored.
		// First, try AcquisitionDateTime.
		Date acquisitionTime = dco.getDate(Tag.AcquisitionDateTime);
		if (acquisitionTime == null) {
			// Not there. So, instead, use the combination of AcquisitionDate
			// and AcquisitionTime.
			acquisitionTime = dco.getDate(Tag.AcquisitionDate);
			if (acquisitionTime == null)
				throw new NullPointerException("No acquisition date available at all");
			// Due to weirdness with time zones, rather than trying to combine
			// the two separate fields into a single date object, just
			// concatenate the strings together. It's easier that way.
			timeStamp = new SimpleDateFormat("M-d-yyyy_").format(acquisitionTime);
			acquisitionTime = dco.getDate(Tag.AcquisitionTime);
			if (acquisitionTime == null) {
				// I'm just going to assume "0-0-0" is OK.
				timeStamp += "0-0-0";
			} else {
				timeStamp += new SimpleDateFormat("H-m-s").format(acquisitionTime);
			}
		} else {
			DateFormat dateFormat = new SimpleDateFormat("M-d-yyyy_H-m-s");//yep; 1 digit time
			timeStamp = dateFormat.format(acquisitionTime);
		}
		// "P" + (zeroId ? "000" : dco.getString(Tag.PatientID))
		return "_" + dco.getString(new int[]{Tag.PerformedProtocolCodeSequence, 0, Tag.CodeMeaning}) + "_" +
		timeStamp + "_" + dco.getString(Tag.Laterality) + "_";
	}

	public DicomObject convert(File src, File dest, String efid) throws IOException {
		return recodeImage(src, dest, efid);
	}

	/**
	 * Recodes the images from the source transfer syntax, as read from the src
	 * file, to the specified destination syntax.
	 */
	public DicomObject recodeImage(File src, File dest, String efid)
			throws IOException {
		if (!imgFile.exists())
			throw new FileNotFoundException(imgFile.toString());
		ImageInputStream input = null;
		ImageInputStream output = null;
		ImageInputStream img_iis = null;
		boolean success = false;
		try {
			input = new FileImageInputStream(src);
			ImageReader reader = new DicomImageReaderSpi().createReaderInstance();
			reader.setInput(input);

			DicomStreamMetaData streamMeta = (DicomStreamMetaData) reader
					.getStreamMetadata();
			ds = streamMeta.getDicomObject();

			File outputFile = new File(dest, String.valueOf(random.nextInt())
					+ ".dcm");
			output = new FileImageOutputStream(outputFile);
			ImageWriter writer = new DicomImageWriterSpi()
			.createWriterInstance();
			writer.setOutput(output);

			System.out.println(ds.getString(Tag.MediaStorageSOPClassUID));

			//verify we can handle this image
			if (!ds.getString(Tag.MediaStorageSOPClassUID).equals("1.2.840.10008.5.1.4.1.1.12.77"))
				throw new IllegalStateException("Unexpected media storage sop class");

			verifyDcmImgCorrelation(src);

			DicomStreamMetaData writeMeta = (DicomStreamMetaData) writer
					.getDefaultStreamMetadata(null);
			DicomObject newDs = new BasicDicomObject();
			// newDs = whitelistDicom(ds);
			ds.copyTo(newDs);

			writeMeta.setDicomObject(newDs);
			frames = ds.getInt(Tag.NumberOfFrames, 1);
			LookupTable lut = prepareBitStrip(writeMeta, reader);
			newDs.putString(Tag.TransferSyntaxUID, VR.UI, destinationSyntax.uid());
			newDs.putString(Tag.PatientName, null, efid);
			newDs.putString(Tag.PatientID, null, efid);

			width = ds.getInt(Tag.Columns);
			height = ds.getInt(Tag.Rows);
			if (flipWidthHeight) {//flip width/height
				int wtemp = width;
				width = height;
				height = wtemp;
				newDs.putInt(Tag.Columns, null, width);
				newDs.putInt(Tag.Rows, null, height);
			}

			allocated = ds.getInt(Tag.BitsAllocated, 8);
			dataType = allocated <= 8 ? DataBuffer.TYPE_BYTE
					: DataBuffer.TYPE_USHORT;

			if (overwriteObject != null)
				overwriteObject.copyTo(newDs);

			// Save only for Fundus association
			Date originalDate = newDs.getDate(Tag.AcquisitionDateTime);

			Calendar cal = Calendar.getInstance();
			cal.setTime(originalDate);
			System.out.println(cal.get(Calendar.MONTH) + "-"
					+ cal.get(Calendar.DAY_OF_MONTH) + "-"
					+ cal.get(Calendar.YEAR));

			generalizeDates(newDs);
			MakeDcmSup110.updateDicomObject(this, newDs);
			blacklistDicom(newDs);
			String laterality = newDs.getString(Tag.Laterality);
			String queryString = "studyUID="
					+ newDs.getString(Tag.StudyInstanceUID) + "&seriesUID="
					+ newDs.getString(Tag.SeriesInstanceUID) + "&objectUID="
					+ newDs.getString(Tag.SOPInstanceUID);
			Uploader.queryMap.put(outputFile.getName(), queryString);
			Uploader.associateFundus(originalDate, queryString, laterality);

			//StreamMapper
			writer.prepareWriteSequence(writeMeta);
			configureJpeg2000(writer);//must be called after prepareWriteSequence()

			img_iis = new FileImageInputStream(imgFile);
			ImageReader img_reader = makeImgImageReaderFromImageInputStream(img_iis);

			for (int i = 0; i < frames; i++) {
				WritableRaster r = (WritableRaster) img_reader.readRaster(i, null);
				ColorModel cm = ColorModelFactory.createColorModel(ds);
				BufferedImage bi = new BufferedImage(cm, r, false, null);
				bi = flipImage(bi);
				if (lut != null)
					lut.lookup(bi.getRaster(), bi.getRaster());
				IIOImage iioimage = new IIOImage(bi, null, null);
				writer.writeToSequence(iioimage, null);
			}

			img_reader.dispose();
			writer.endWriteSequence();
			success = true;
			return newDs;
		} finally {
			CloseUtils.safeClose(img_iis);
			CloseUtils.safeClose(output);
			if (!success)
				dest.delete();
			CloseUtils.safeClose(input);
		}
	}

	private void generalizeDates(DicomObject newDs) {
		Date date = newDs.getDate(Tag.AcquisitionDateTime);
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.set(cal.get(Calendar.YEAR), 0, 0);
		newDs.putDate(Tag.AcquisitionDateTime, null, cal.getTime());
		newDs.putDate(Tag.AcquisitionDate, null, cal.getTime());
		newDs.putDate(Tag.StudyDate, null, cal.getTime());
		newDs.putDate(Tag.SeriesDate, null, cal.getTime());
		newDs.putDate(Tag.ContentDate, null, cal.getTime());
		newDs.putDate(Tag.StudyTime, null, cal.getTime());
		newDs.putDate(Tag.SeriesTime, null, cal.getTime());
		newDs.putDate(Tag.AcquisitionTime, null, cal.getTime());
		newDs.putDate(Tag.ContentDate, null, cal.getTime());
		newDs.putDate(Tag.ContentTime, null, cal.getTime());
		newDs.putDate(Tag.PerformedProcedureStepStartDate, null, cal.getTime());
		newDs.putDate(Tag.PerformedProcedureStepStartTime, null, cal.getTime());

		cal.setTime(newDs.getDate(Tag.PatientBirthDate));
		cal.set(cal.get(Calendar.YEAR), 0, 0);
		newDs.putDate(Tag.PatientBirthDate, null, cal.getTime());

	}

	private void blacklistDicom(DicomObject d) {

		int[] list = { Tag.AccessionNumber,
				Tag.InstitutionName, Tag.ReferringPhysicianName, 0x00081155,
				Tag.PatientName,
				Tag.StudyID, Tag.FrameOfReferenceUID, Tag.BurnedInAnnotation,
				Tag.UID, Tag.StorageMediaFileSetUID,
				Tag.ReferencedFrameOfReferenceUID,
				Tag.RelatedFrameOfReferenceUID };
		for (int i = 0; i < list.length; i++) {
			d.putNull(list[i], null);
		}
	}

	private void verifyDcmImgCorrelation(File src) {
		//		String imgPatientId;
		//		if (imgFile.getName().startsWith("P000_"))
		//			imgPatientId = "000";
		//		else
		//			imgPatientId = ds.getString(Tag.PatientID);
		//		String expectedImgPrefix = "P" + imgPatientId + getPartialImgNameFromDcm(ds);
		//		if (!imgFile.getName().startsWith(expectedImgPrefix))
		//			throw new IllegalStateException("img file " + imgFile + " should start with " + expectedImgPrefix +
		//					" for dcm " + src);
		String partialName = getPartialImgNameFromDcm(ds);
		if (!imgFile.getName().contains(partialName))
			throw new IllegalStateException("img file " + imgFile + " should contain " + partialName +
					" for dcm " + src);
	}

	private void configureJpeg2000(ImageWriter writer) {
		boolean jp2k = destinationSyntax.uid().equals(UID.JPEG2000);
		boolean jp2kll = destinationSyntax.uid().equals(UID.JPEG2000LosslessOnly);
		if (jp2k || jp2kll) {
			J2KImageWriteParam param;
			//writer.writeParam is not public; use reflection to set it.
			try {
				Class<? extends ImageWriter> cls = writer.getClass();
				Field fld = cls.getDeclaredField("writeParam");
				fld.setAccessible(true);
				param = (J2KImageWriteParam) fld.get(writer);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			// http://www.google.com/codesearch/p?hl=en#Ze0e5oS74AU/dcm4che14/samples/java/Transcoder.java&q=encodingRate%20J2KImageWriteParam%20lang:java&sa=N&cd=3&ct=rc
			param.setWriteCodeStreamOnly(true);
			if (jp2k) {
				param.setLossless(false);
				param.setEncodingRate((double) allocated / (double) targetLossyCompressionRatio);
			}

		} else {
			System.out.println("No Jpeg2000 UID");
		}
	}

	// Some code copied from DicomImageReader

	ImageReader makeImgImageReaderFromImageInputStream(ImageInputStream iis) throws IOException {
		long[] frameOffsets = new long[frames];
		int frameLen = width * height * samples * (allocated >> 3);
		final long pixelDataPos = 0L;
		frameOffsets[0] = pixelDataPos;
		for (int i = 1; i < frameOffsets.length; i++) {
			frameOffsets[i] = frameOffsets[i - 1] + frameLen;
		}
		Dimension[] imageDimensions = new Dimension[frames];
		Arrays.fill(imageDimensions, new Dimension(width, height));
		ImageInputStream riis = new RawImageInputStream(iis,
				createImageTypeSpecifier(), frameOffsets, imageDimensions);
		riis.setByteOrder(ByteOrder.LITTLE_ENDIAN);//?
		ImageReader reader = ImageIO.getImageReadersByFormatName("RAW").next();
		reader.setInput(riis);
		return reader;
	}

	public BufferedImage flipImage(BufferedImage img) {
		WritableRaster r = Raster.createWritableRaster(img.getSampleModel(), null);
		BufferedImage dimg = new BufferedImage(img.getColorModel(), r, false, null);
		Graphics2D g = dimg.createGraphics();
		int w = img.getWidth();
		int h = img.getHeight();
		int sx1 = (horizontalFlip ? w : 0);
		int sy1 = (verticalFlip ? h : 0);
		int sx2 = (horizontalFlip ? 0 : w);
		int sy2 = (verticalFlip ? 0 : h);
		boolean done = g.drawImage(img, 0, 0, w, h, sx1, sy1, sx2, sy2, null);
		assert done;
		g.dispose();
		return dimg;
	}

	/**
	 * Create an image type specifier for the entire image
	 */
	protected ImageTypeSpecifier createImageTypeSpecifier() {
		ColorModel cm = ColorModelFactory.createColorModel(ds);
		SampleModel sm = createSampleModel();
		return new ImageTypeSpecifier(cm, sm);
	}

	private SampleModel createSampleModel() {
		if (samples == 1) {
			return new PixelInterleavedSampleModel(dataType, width, height, 1,
					width, OFFSETS_0);
		}
		throw new IllegalStateException("samples != 1");
	}


	public void setImgFile(File imgFile) {
		this.imgFile = imgFile;
	}

	public void setFlipWidthHeight(boolean flipWidthHeight) {
		this.flipWidthHeight = flipWidthHeight;
	}

	public void setTargetLossyCompressionRatio(int targetLossyCompressionRatio) {
		this.targetLossyCompressionRatio = targetLossyCompressionRatio;
	}

	public void setVerticalFlip(boolean verticalFlip) {
		this.verticalFlip = verticalFlip;
	}

	public void setHorizontalFlip(boolean horizontalFlip) {
		this.horizontalFlip = horizontalFlip;
	}
}
