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

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.TimeZone;

import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.dcm4che2.data.DicomElement;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.SpecificCharacterSet;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.VR;

/**
 * Utility class for generating JSON objects based on DICOM meta data.
 *
 * @author dpotter
 */
public class DicomJSONConverter {
	private static JsonFactory jsonFactory = new JsonFactory();

	public static void convertToJSON(DicomObject dicom, OutputStream out) throws JsonGenerationException, IOException {
		JsonGenerator jg = jsonFactory.createJsonGenerator(out, JsonEncoding.UTF8);
		convertToJSON(dicom, jg);
		jg.flush();
	}
	public static void convertToJSON(DicomObject dicom, Writer out) throws JsonGenerationException, IOException {
		JsonGenerator jg = jsonFactory.createJsonGenerator(out);
		convertToJSON(dicom, jg);
		jg.flush();
	}

	/**
	 * Convert the given DICOM file to a JSON object. This is essentially a
	 * special form of {@link #convertToJSONValue(DicomObject, SpecificCharacterSet, JsonGenerator)}
	 * (in fact, {@code convertToJSONValue(DicomObject, SpecificCharacterSet, JsonGenerator)}
	 * is invoked to generate the contents of the {@code elements} field), with
	 * some additional metadata extracted.
	 * @param dicom
	 * @param out
	 * @throws JsonGenerationException
	 * @throws IOException
	 */
	public static void convertToJSON(DicomObject dicom, JsonGenerator out) throws JsonGenerationException, IOException {
		SpecificCharacterSet charset = dicom.getSpecificCharacterSet();
		out.writeStartObject(); // {
		out.writeFieldName("elements");
		convertToJSONValue(dicom, charset, out);
		DicomElement rows = dicom.get(Tag.Rows);
		if (rows != null) {
			out.writeNumberField("rows", rows.getInt(true));
		}
		DicomElement columns = dicom.get(Tag.Columns);
		if (columns != null) {
			out.writeNumberField("columns", columns.getInt(true));
		}
		DicomElement frames = dicom.get(Tag.NumberOfFrames);
		if (frames != null) {
			out.writeNumberField("frames", frames.getInt(true));
		}
		out.writeNumberField("aspectRatio", findAspectRatio(dicom));
		out.writeEndObject(); // }
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
	 * Determine whether or not the given tag will be included in the JSON
	 * representation.
	 * @param tag
	 * @return
	 */
	public static boolean includeTag(int tag) {
		return tag != Tag.PixelData;
	}

	/**
	 * Writes out all the fields in a DICOM object. Each individual element is
	 * written as the following object:
	 * <dl>
	 * <dt>{@code tag}</dt>
	 * <dd>The hex tag, as returned by
	 * {@link TagNameUtil#convertTagToHexString(int)}.</dd>
	 * <dt>{@code tagName}</dt>
	 * <dd>The hex tag as a human readable string, as returned by
	 * {@link TagNameUtil#toName(int)}.</dd>
	 * <dt>{@code vr}</dt>
	 * <dd>The VR of this element</dd>
	 * <dt>{@code value}</dt>
	 * <dd>The actual value conversion via
	 * {@link #convertToJSONValue(DicomElement, SpecificCharacterSet, JsonGenerator)}
	 * </dd>
	 * </dl>
	 * 
	 * @param object
	 *            the object to convert
	 * @param charset
	 *            the specific charset (see
	 *            {@link DicomObject#getSpecificCharacterSet()})
	 * @param out
	 *            the generator used to create JSON
	 * @throws JsonGenerationException
	 *             if the generator flags an exception
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public static void convertToJSONValue(DicomObject object,
			SpecificCharacterSet charset, JsonGenerator out)
					throws JsonGenerationException, IOException {
		Iterator<DicomElement> iter = object.iterator();
		out.writeStartArray(); // [
		while (iter.hasNext()) {
			DicomElement elem = iter.next();
			// Only bother writing elements that are metadata
			int tag = elem.tag();
			if (includeTag(tag)) {
				out.writeStartObject(); // {
				out.writeFieldName("tag");
				out.writeString(TagNameUtil.convertTagToHexString(tag));
				out.writeFieldName("tagName");
				out.writeString(TagNameUtil.toName(tag));
				out.writeFieldName("vr");
				out.writeString(elem.vr().toString());
				out.writeFieldName("value");
				convertToJSONValue(elem, charset, out);
				out.writeEndObject(); // }
			}
		}
		out.writeEndArray(); // ]
	}

	/**
	 * Attempts to convert a single {@code DicomElement} into a JSON value.
	 * 
	 * @param element
	 *            the element to convert
	 * @param charset
	 *            the specific charset (see
	 *            {@link DicomObject#getSpecificCharacterSet()})
	 * @param out
	 *            the generator used to create JSON
	 * @throws JsonGenerationException
	 *             if the generator flags an exception
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public static void convertToJSONValue(DicomElement element,
			SpecificCharacterSet charset, JsonGenerator out)
					throws JsonGenerationException, IOException {
		if (element == null) {
			out.writeNull();
			return;
		}
		try {
			VR vr = element.vr();
			if (vr == null) {
				out.writeString("(no VR for element)");
			} else if (vr.equals(VR.SS) || vr.equals(VR.SL) ||
					vr.equals(VR.US) || vr.equals(VR.UL)){
				// FIXME: UL should be unsigned
				out.writeNumber(element.getInt(false));
			} else if (vr.equals(VR.FD) || vr.equals(VR.FL) || vr.equals(VR.DS)) {
				out.writeNumber(element.getDouble(false));
			} else if (vr.equals(VR.SH) || vr.equals(VR.ST) ||
					vr.equals(VR.LO) || vr.equals(VR.LT) ||
					vr.equals(VR.OB) || vr.equals(VR.OF) || vr.equals(VR.OW) || // I think this is wrong
					vr.equals(VR.UI) || vr.equals(VR.UT) ||
					vr.equals(VR.AE) || vr.equals(VR.CS) || vr.equals(VR.IS) ||
					vr.equals(VR.PN)) {
				out.writeString(element.getString(charset, false));
			} else if (vr.equals(VR.DA)) {
				// Convert to a date
				out.writeString(getDateInstance().format(element.getDate(false)));
			} else if (vr.equals(VR.DT)) {
				// Convert to a date
				out.writeString(getDateTimeInstance().format(element.getDate(false)));
			} else if (vr.equals(VR.TM)) {
				// Convert to a date
				out.writeString(getTimeInstance().format(element.getDate(false)));
			} else if (vr.equals(VR.SQ)) {
				// Is a sequence of items, which is somewhat annoying to support.
				DicomObject object = element.getDicomObject();
				if (object == null)
					out.writeNull();
				else
					convertToJSONValue(object, charset, out);
			} else {
				// If we've fallen to here, we can't convert
				out.writeString("(unable to convert VR " + vr.toString() + ")");
			}
		} catch (UnsupportedOperationException e) {
			out.writeString("(unsupported operation exception attempting to convert this value)");
		}
	}

	private static SimpleDateFormat getDateInstance() {
		SimpleDateFormat res = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
		res.setTimeZone(TimeZone.getTimeZone("GMT"));
		return res;
	}

	private static SimpleDateFormat getDateTimeInstance() {
		SimpleDateFormat res = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S", Locale.US);
		res.setTimeZone(TimeZone.getTimeZone("GMT"));
		return res;
	}

	private static SimpleDateFormat getTimeInstance() {
		SimpleDateFormat res = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);
		res.setTimeZone(TimeZone.getTimeZone("GMT"));
		return res;
	}

	/**
	 * Get the content date out of a DICOM object.
	 * @param object the object to get
	 * @return the date
	 */
	public static Date getContentDate(DicomObject object) {
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
	}
}
