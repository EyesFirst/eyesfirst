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

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
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
		convertToJSONValue(dicom, dicom.getSpecificCharacterSet(), jg);
		jg.flush();
	}
	public static void convertToJSON(DicomObject dicom, Writer out) throws JsonGenerationException, IOException {
		JsonGenerator jg = jsonFactory.createJsonGenerator(out);
		convertToJSONValue(dicom, dicom.getSpecificCharacterSet(), jg);
		jg.flush();
	}
	/**
	 * Converts a given DICOM object into a JSON string.
	 * @param dicom
	 * @return
	 * @throws JsonGenerationException
	 * @throws IOException
	 */
	public static String convertToJSON(DicomObject dicom) throws JsonGenerationException, IOException {
		StringWriter writer = new StringWriter();
		convertToJSON(dicom, writer);
		return writer.toString();
	}

	public static void convertToViewerJSON(DicomObject dicom, Writer out) throws JsonGenerationException, IOException {
		JsonGenerator jg = jsonFactory.createJsonGenerator(out);
		convertToViewerJSON(dicom, jg);
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
	public static void convertToViewerJSON(DicomObject dicom, JsonGenerator out) throws JsonGenerationException, IOException {
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
		out.writeNumberField("aspectRatio", DicomMetadataUtil.findAspectRatio(dicom));
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
	 * @deprecated Use {@link DicomMetadataUtil#findAspectRatio(DicomObject)} instead
	 */
	public static double findAspectRatio(DicomObject dicom) {
		return DicomMetadataUtil.findAspectRatio(dicom);
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
			} else if (vr.equals(VR.FD) || vr.equals(VR.FL)) {
				out.writeNumber(element.getDouble(false));
			} else if (vr.equals(VR.DS)) {
				double[] values = element.getDoubles(false);
				out.writeStartArray(); // [
				for (double d : values) {
					out.writeNumber(d);
				}
				out.writeEndArray(); // ]
			} else if (vr.equals(VR.SH) || vr.equals(VR.ST) ||
					vr.equals(VR.LO) || vr.equals(VR.LT) ||
					vr.equals(VR.OB) || vr.equals(VR.OF) || vr.equals(VR.OW) || // I think this is wrong
					vr.equals(VR.UI) || vr.equals(VR.UT) ||
					vr.equals(VR.AE) || vr.equals(VR.CS) || vr.equals(VR.IS) ||
					vr.equals(VR.PN)) {
				out.writeString(element.getString(charset, false));
			} else if (vr.equals(VR.DA)) {
				// Convert to a date
				writeDate(out, getDateInstance(), element);
			} else if (vr.equals(VR.DT)) {
				// Convert to a date
				writeDate(out, getDateTimeInstance(), element);
			} else if (vr.equals(VR.TM)) {
				// Convert to a date
				writeDate(out, getTimeInstance(), element);
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

	private static void writeDate(JsonGenerator out, SimpleDateFormat format, DicomElement element) throws IOException {
		Date date;
		try {
			date = element.getDate(false);
		} catch (NumberFormatException e) {
			out.writeString("invalid date in date field");
			return;
		}
		if (date == null) {
			out.writeNull();
		} else {
			out.writeString(format.format(date));
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
	 * @deprecated Use {@link DicomMetadataUtil#getContentDate(DicomObject)} instead
	 */
	public static Date getContentDate(DicomObject object) {
		return DicomMetadataUtil.getContentDate(object);
	}
}
