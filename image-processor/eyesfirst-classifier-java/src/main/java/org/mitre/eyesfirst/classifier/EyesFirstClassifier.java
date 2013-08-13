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
package org.mitre.eyesfirst.classifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.Charset;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.dcm4che2.data.DicomElement;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.SpecificCharacterSet;
import org.dcm4che2.data.Tag;
import org.dcm4che2.io.DicomInputStream;
import org.mitre.eyesfirst.classifierml.EyesFirstMLClassifier;
import org.mitre.eyesfirst.common.DicomSend;
import org.mitre.eyesfirst.dicom.DicomID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mathworks.toolbox.javabuilder.MWException;

public class EyesFirstClassifier implements Runnable {
	public static final int BUFFER_SIZE=1024*8;
	private URI wadoURI;
	private DicomID dicomID;
	//private URI fundusURI;
	private File destDir;
	private String dicomURI;
	private URI callbackURI;
	private String callbackKey;
	private HttpClient httpClient;
	private Logger log = LoggerFactory.getLogger(getClass());

	public EyesFirstClassifier(URI wadoURI, File destDir, String dicomURI, URI callbackURI, String callbackKey) throws URISyntaxException {
		this.wadoURI = wadoURI;
		if (!this.wadoURI.getScheme().equals("http")) {
			throw new RuntimeException("EyesFirst classifier only supports HTTP URLs.");
		}
		dicomID = DicomID.fromQueryString(this.wadoURI.getRawQuery());
		if (callbackURI != null && !("http".equals(callbackURI.getScheme()))) {
			throw new RuntimeException("EyesFirst classifier only supports HTTP URLs.");
		}
		this.destDir = destDir;
		if (!destDir.exists()) {
			if (!destDir.mkdirs()) {
				throw new RuntimeException("Unable to create destination directory");
			}
		} else if (!destDir.isDirectory()) {
			throw new RuntimeException("Destination directory " + destDir + " is not a directory");
		}
		this.dicomURI = dicomURI;
		this.callbackURI = callbackURI;
		this.callbackKey = callbackKey;
		httpClient = new DefaultHttpClient();
	}

	private String getContentType(HttpResponse response) throws IOException {
		Header[] headers = response.getHeaders("Content-type");
		if (headers.length > 0) {
			// If there are multiple, just use the last one?
			return headers[headers.length-1].getValue();
		} else {
			return null;
		}
	}

	private void downloadTo(HttpResponse response, File dest) throws IOException {
		HttpEntity entity = response.getEntity();
		long length = entity.getContentLength();
		log.info("Downloading {} bytes to {}...", (length > 0 ? Long.toString(length) : "an unknown number of"), dest.getPath());
		byte[] buf = new byte[BUFFER_SIZE];
		FileOutputStream out = new FileOutputStream(dest);
		InputStream in = null;
		IOException ioe = null;
		try {
			in = entity.getContent();
			while (true) {
				int r = in.read(buf);
				if (r < 0)
					break;
				out.write(buf, 0, r);
			}
			out.close();
		} catch (IOException e) {
			// Defer
			ioe = e;
		} finally {
			try {
				if (in != null)
					in.close();
			} catch (IOException e) {
				// Defer
				if (ioe != null)
					ioe = e;
			}
			try {
				if (out != null)
					out.close();
			} catch (IOException e) {
				// Defer
				if (ioe != null)
					ioe = e;
			}
		}
		if (ioe != null)
			throw ioe;
	}
	/**
	 * Download the DICOM file.
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	private void runDownload() throws ClientProtocolException, IOException {
		HttpGet request = new HttpGet(wadoURI);
		HttpResponse response = httpClient.execute(request);
		// Make sure the response is successful
		if (response.getStatusLine().getStatusCode() != 200) {
			throw new IOException("Unable to retrieve WADO URL: Server said " + response.getStatusLine());
		}
		// Just make sure the response entity is an application/ type.
		String type = getContentType(response);
		if (type != null && (!type.startsWith("application/"))) {
			throw new IOException("Bad content type " + type + " for DICOM image");
		}
		downloadTo(response, new File(destDir, "original.dcm"));
	}

	private void runMatlabProcess() throws MWException, ProcessorException {
		// This is slightly more complicated, since it needs to run within the
		// Matlab context.
		EyesFirstMLClassifier classifier = new EyesFirstMLClassifier();
		classifier.call_eyesfirst_classifier(destDir.getAbsolutePath());
		// Since the thickness classifier traps the various errors, check to
		// make sure the various artifacts were actually created and abort here
		// if they weren't. (Really, the MATLAB code shouldn't trap all errors,
		// but due to the way it's currently written, it sort of has to. It's
		// intended to allow the process to go through as far as possible when
		// run as a batch process. As a side effect, this means that the process
		// failing allows us to continue with previously generated outputs,
		// which may be desired anyway.)
		if (!checkFileExists(getProcessedDicom())) {
			throw new ProcessorException("Output DICOM (" + getProcessedDicom().getAbsolutePath() + ") was not generated.");
		}
		if (!checkFileExists(getThicknessMap())) {
			throw new ProcessorException("Output thickness map (" + getThicknessMap().getAbsolutePath() + ") was not generated.");
		}
		if (!checkFileExists(getSynthesizedFundus())) {
			throw new ProcessorException("Output synthesized FUNDUS photo (" + getSynthesizedFundus().getAbsolutePath() + ") was not generated.");
		}
		if (!checkFileExists(getDiagnosesJSON())) {
			throw new ProcessorException("Output diagnostic information (" + getDiagnosesJSON().getAbsolutePath() + ") was not generated.");
		}
	}

	private boolean checkFileExists(File f) {
		// I'm unclear if isFile can return true for a path that doesn't specify
		// a file that doesn't exist, but whatever.
		return f.exists() && f.isFile();
	}

	private void runSendDicom() throws Exception {
		if (dicomURI == null)
			return;

		DicomSend dicomSend = new DicomSend();
		dicomSend.setDicomURL(dicomURI);
		dicomSend.sendFile(getProcessedDicom());
	}

	private File getProcessedDicom() {
		return new File(destDir, "storeOutput" + File.separator + "original_processed.dcm");
	}

	private File getThicknessMap() {
		return new File(destDir, "storeOutput" + File.separator + "original_thickness_map.png");
	}

	private File getSynthesizedFundus() {
		return new File(destDir, "storeOutput" + File.separator + "original_synthesized_fundus.png");
	}

	private File getDiagnosesJSON() {
		return new File(destDir, "storeOutput" + File.separator + "original_results.json");
	}

	/**
	 * Sends the results to the callback URI, if there are any.
	 * @throws IOException if an I/O exception occurs while sending the file
	 */
	private void runCallback() throws IOException {
		if (callbackURI == null) {
			// Nothing to do.
			return;
		}
		HttpPost request = new HttpPost(callbackURI);
		MultipartEntity entity = new MultipartEntity();
		Charset utf8;
		try {
			utf8 = Charset.forName("UTF-8");
			entity.addPart("key", new StringBody(callbackKey, "text/plain", utf8));
			entity.addPart("rawDicomId", new StringBody(dicomID.toQueryString(), "text/plain", utf8));
			String dicomResultURI = findDicomResultURI();
			if (dicomResultURI != null) {
				entity.addPart("processedDicomId", new StringBody(dicomResultURI, "text/plain", utf8));
			}
			// Sending the classifier diagnoses as a FileBody breaks the classifer.
			// Since it's just text, load it up as text on our end.
			StringBuilder sb = new StringBuilder();
			Reader reader = new FileReader(getDiagnosesJSON());
			char[] buf = new char[1024];
			while (true) {
				int r = reader.read(buf);
				if (r < 0)
					break;
				sb.append(buf, 0, r);
			}
			reader.close();
			entity.addPart("classifierDiagnoses", new StringBody(sb.toString(), "text/plain", utf8));
		} catch (UnsupportedEncodingException e) {
			// Since we always use UTF-8, this should never happen
			// (Using an error is done because the Java libraries themselves do
			// that when UTF-8 is missing)
			throw new Error("UTF-8 unsupported");
		}
		entity.addPart("thicknessMap", new FileBody(getThicknessMap(), "image/png"));
		entity.addPart("fundusPhoto", new FileBody(getSynthesizedFundus(), "image/png"));
		request.setEntity(entity);
		log.info("Invoking callback...");
		httpClient.execute(request);
	}

	private String findDicomResultURI() throws IOException {
		DicomInputStream dicomIn = new DicomInputStream(getProcessedDicom());
		DicomObject metadata = dicomIn.readDicomObject();
		// From this, we should be able to get the various pieces of metadata we need
		String rv = createDicomResultURI(metadata);
		dicomIn.close();
		return rv;
	}

	private static Logger logger() {
		return LoggerFactory.getLogger(EyesFirstClassifier.class);
	}

	static String createDicomResultURI(DicomObject dicom) {
		SpecificCharacterSet sc = dicom.getSpecificCharacterSet();
		try {
			StringBuilder result = new StringBuilder("studyUID=");
			DicomElement e = dicom.get(Tag.StudyInstanceUID);
			if (e == null) {
				// No study ID? Give up
				logger().debug("No StudyInstanceUID found");
				return null;
			}
			// I'm pretty sure the URL encoder is never really needed, but, well,
			// better safe than sorry.
			result.append(URLEncoder.encode(e.getString(sc, false), "UTF-8"));
			result.append("&seriesUID=");
			e = dicom.get(Tag.SeriesInstanceUID);
			if (e == null) {
				// No series ID? Give up
				logger().debug("No SeriesInstanceUID found");
				return null;
			}
			result.append(URLEncoder.encode(e.getString(sc, false), "UTF-8"));
			result.append("&objectUID=");
			e = dicom.get(Tag.SOPInstanceUID);
			if (e == null) {
				// No object ID? Give up
				logger().debug("No SOPInstanceUID found");
				return null;
			}
			result.append(URLEncoder.encode(e.getString(sc, false), "UTF-8"));
			return result.toString();
		} catch (UnsupportedOperationException e) {
			logger().warn("Unable to create result URI (bad DICOM file?)", e);
			return null;
		} catch (UnsupportedEncodingException e) {
			// I think this can be triggered by DicomElement.getString(..), so
			// log this and return null
			logger().warn("Unable to create result URI", e);
			return null;
		}
	}

	public void runAll() throws IOException, MWException, Exception {
		//System.out.println("STATUS:{\"message\":\"Downloading fundus file...\"}");
		//runBloodVesselExtraction();
		System.out.println("STATUS:{\"message\":\"Downloading DICOM file...\"}");
		runDownload();
		System.out.println("STATUS:{\"message\":\"Starting EyesFirst MATLAB classifier...\"}");
		runMatlabProcess();
		System.out.println("STATUS:{\"message\":\"Sending DICOM file to DCM4CHEE...\"}");
		runSendDicom();
		System.out.println("STATUS:{\"message\":\"Uploading results to DORI web...\"}");
		runCallback();
		System.out.println("STATUS:{\"message\":\"Done.\"}");
	}

	/**
	 * Runs the entire EyesFirst classifier process. Since this implements
	 * {@link Runnable} this won't throw exceptions. Use {@link #runAll()} if
	 * you want to catch those.
	 */
	public void run() {
		try {
			runAll();
		} catch (Exception e) {
			if (e instanceof RuntimeException) {
				throw (RuntimeException) e;
			}
			throw new RuntimeException("Error executing thickness classifier", e);
		}
	}
}
