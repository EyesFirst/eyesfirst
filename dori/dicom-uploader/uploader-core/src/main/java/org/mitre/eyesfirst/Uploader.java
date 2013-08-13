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

package org.mitre.eyesfirst;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.util.EntityUtils;
import org.dcm4che2.data.DicomObject;
import org.mitre.eyesfirst.dcm4che2.DCM4CHE2Util;
import org.mitre.eyesfirst.dori.MergeZeissDicom;
import org.mitre.eyesfirst.ui.NullProgressMonitor;
import org.mitre.eyesfirst.ui.ProgressMonitor;
import org.mitre.eyesfirst.ui.ShrinkingLargeProgressMonitor;

import com.google.common.io.Files;

/**
 * The Uploader class deals with state required to upload actual files.
 * 
 * @author jsutherland
 * @author dpotter
 */
public class Uploader {
	static {
		// Fix DCM4CHE2 on classload
		DCM4CHE2Util.setImageIOSettings();
	}

	private String doriURL;
	private Map<String, String> queryMap;
	private Map<String, String> studyUidMap, seriesUidMap;
	private List<FundusPhoto> photos = new ArrayList<FundusPhoto>();
	private File outFolder;
	private File fundusDir;
	private AbstractHttpClient httpClient;

	public Uploader() {
		this("http://localhost:8080/doriweb");
	}

	public Uploader(String doriURL) {
		if (doriURL == null)
			throw new NullPointerException();
		setSessionInformation(doriURL, null);
		httpClient = new DefaultHttpClient();
	}

	/**
	 * Attempt to login to DORI with the given username and password.
	 * @param name
	 * @param password
	 * @throws IOException if something fails during the process
	 */
	public void login(String name, String password) throws IOException {
		// Login is an obnoxious process. First, GET /login/index to generate
		// the session.
		HttpGet get = new HttpGet(doriURL + "/login/index");
		HttpResponse response = httpClient.execute(get);
		int status = response.getStatusLine().getStatusCode();
		// The response we expect is 302 Moved Temporarily
		if (status != 200 && status != 302) {
			//response.getEntity().writeTo(System.out);
			throw new IOException("Unexpected response from server: " + response.getStatusLine() + " Expected a 200 OK or 302 Moved Temporarily response to the login page.");
		}
		// Ignore the URL
		EntityUtils.consume(response.getEntity());

		HttpPost post = new HttpPost(doriURL + "/j_spring_security_check");

		BasicHttpEntity entity = new BasicHttpEntity();
		entity.setContentType("application/x-www-form-urlencoded; charset=UTF-8");
		StringBuilder postData = new StringBuilder();
		postData.append("j_username=");
		postData.append(URLEncoder.encode(name, "UTF-8"));
		postData.append("&j_password=");
		postData.append(URLEncoder.encode(password, "UTF-8"));
		entity.setContent(new ByteArrayInputStream(postData.toString().getBytes("UTF-8")));
		post.setEntity(entity);
		response = httpClient.execute(post);
		status = response.getStatusLine().getStatusCode();
		if (status != 200 && status != 302) {
			//response.getEntity().writeTo(System.out);
			throw new IOException("Unexpected response from server: " + response.getStatusLine() + " Expected a 200 OK or 302 Moved Temporarily response to the login page. (Did the login fail?)");
		}
		// Sadly the only real way to know whether or not the login succeeded is
		// if the session cookie is set, and conceptually, we don't know what
		// that is.
		EntityUtils.consume(response.getEntity());
		/*
		System.out.println("Response: " + status);
		System.out.println("Cookies:");
		for (Cookie cookie : httpClient.getCookieStore().getCookies()) {
			System.out.println("  - " + cookie);
		}
		*/
	}

	/**
	 * Sets the session information required to upload data back to the server.
	 * @param hostname the hostname
	 * @param cookie
	 */
	public void setSessionInformation(String doriURL, String cookie) {
		if (doriURL.endsWith("/")) {
			// Chop off any trailing "/"
			doriURL = doriURL.substring(0, doriURL.length() - 1);
		}
		this.doriURL = doriURL;
		if (cookie != null) {
			// If we have a cookie to add, add it to the client cookie store.
			int i = cookie.indexOf('=');
			String name, value;
			if (i >= 0) {
				name = cookie.substring(0, i);
				value = cookie.substring(i+1);
			} else {
				name = "JSESSIONID";
				value = cookie;
			}
			httpClient.getCookieStore().addCookie(new BasicClientCookie(name, value));
		}
	}

	void delTree(File dir) {
		for (File f : dir.listFiles()) {
			if (f.isDirectory()) {
				delTree(f);
			}
			f.delete();
		}
	}

	// FIXME: I have no idea what the old series/old studies maps are used for.
	public String getOldSeriesUID(String currentUID) {
		return seriesUidMap.get(currentUID);
	}

	public boolean hasOldSeriesUID(String uid) {
		return seriesUidMap.containsKey(uid);
	}

	public void putOldSeriesUID(String oldUID, String newUID) {
		seriesUidMap.put(oldUID, newUID);
	}

	public String getOldStudyUID(String currentUID) {
		return studyUidMap.get(currentUID);
	}

	public boolean hasOldStudyUID(String uid) {
		return studyUidMap.containsKey(uid);
	}

	public void putOldStudyUID(String oldUID, String newUID) {
		studyUidMap.put(oldUID, newUID);
	}

	public void associateQueryString(String name, String queryString) {
		queryMap.put(name, queryString);
	}

	/**
	 * Process a collection of files.
	 * 
	 * @param dcmFiles
	 *            the DICOM files to process
	 * @param imgFiles
	 *            the IMG files to process
	 * @param efid
	 *            the EFID to use
	 * @param monitor
	 *            a progress monitor used to monitor state
	 * @return a list of generated DicomObjects
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public List<DicomObject> process(FileCollection dcmFiles, FileCollection imgFiles, String efid, ProgressMonitor monitor) throws IOException {
		return process(new MergeFileCollection(dcmFiles, imgFiles), efid, monitor);
	}
	public List<DicomObject> process(FileCollection files, String efid, ProgressMonitor monitor) throws IOException {
		outFolder = Files.createTempDir();

		monitor.startTask(0, "Preparing DICOM files for upload...");

		queryMap = new HashMap<String, String>();
		studyUidMap = new HashMap<String, String>();
		seriesUidMap = new HashMap<String, String>();
		List<DicomObject> results = new ArrayList<DicomObject>();

		MergeZeissDicom.convert(this, files, outFolder,
				"1.2.840.10008.1.2.4.90", efid, monitor, results);
		return results;
	}

	public List<FundusPhoto> processFundus(File fundusFile) throws IOException {
		if (!fundusFile.isDirectory()) {
			fundusDir = extractFolder(new ZipFile(fundusFile), new NullProgressMonitor());
		} else {
			fundusDir = fundusFile;
		}

		for (File f : fundusDir.listFiles()) {
			if (f.isDirectory()) {
				processFundus(f);
			} else {
				photos.add(new FundusPhoto(f));
			}
		}
		return photos;
	}

	/**
	 * This method should not be static.
	 * @param date
	 * @param queryString
	 * @param laterality
	 */
	public void associateFundus(Date date, String queryString, String laterality) {
		if (photos != null) {
			for (FundusPhoto f : photos) {
				if (f.isSameDay(date) || f.isSameLaterality(laterality)) {
					f.addQueryString(queryString);
				}
			}
		}
	}

//	public void associateFundus(Date date, String queryString, String laterality) {
		
//	}

	public void upload(String efid, ProgressMonitor progress) throws IOException {
		HttpPost post = new HttpPost(doriURL + "/upload/receive");

		MultipartEntity entity = new MultipartEntity();

		ShrinkingLargeProgressMonitor largeProgress = new ShrinkingLargeProgressMonitor(progress);
		File[] files = outFolder.listFiles();
		long total = 0;
		for (File f : files) {
			System.out.println("Adding " + f.getName());
			entity.addPart(queryMap.get(f.getName()), new MonitoredFileBody(f, largeProgress));
			total += f.length();
		}
		largeProgress.startTask(total, files.length == 1 ? "Uploading DICOM file..." : "Uploading " + files.length + " DICOM files...");

		System.out.println("Sending for EFID " + efid);
		entity.addPart("efid", new StringBody(efid));
		entity.addPart("timestamp", new StringBody(new Date().toString()));

		post.setEntity(entity);
		System.out.println("Sending " + entity.getContentLength() + " to "
				+ post.getURI().toString());

		long t1 = System.currentTimeMillis();
		HttpResponse response = httpClient.execute(post);

		System.out.println("Upload took "
				+ (System.currentTimeMillis() - t1)
				+ "ms");
		System.out.println(response.getStatusLine().toString());
		response.getEntity().writeTo(System.out);
		checkUploadResponse(response);
	}

	/**
	 * Checks to make sure the server response is a 200 OK, throwing an
	 * exception if it isn't.
	 * 
	 * @param response
	 *            the response to check
	 * @throws IOException
	 *             if the response indicates an error occurred while handling
	 *             the upload
	 */
	private static void checkUploadResponse(HttpResponse response) throws IOException {
		if (response.getStatusLine().getStatusCode() != 200) {
			if (response.getStatusLine().getStatusCode() == 302) {
				throw new IOException("Received 302 from server - have you been logged out?");
			} else {
				throw new IOException("Received error response from server (" + response.getStatusLine() + ")");
			}
		}
	}

	/**
	 * Extracts the files in the given zip file to a temporary output directory.
	 * @param zipFile the zip file to extract
	 * @param monitor
	 * @return the directory where the files where output to
	 * @throws ZipException
	 * @throws IOException
	 */
	public static File extractFolder(ZipFile zipFile, ProgressMonitor monitor)
			throws ZipException, IOException {

		int BUFFER = 2048;

		File outDir = Files.createTempDir();
		Enumeration<? extends ZipEntry> zipFileEntries = zipFile.entries();

		// Process each entry
		while (zipFileEntries.hasMoreElements()) {
			// grab a zip file entry
			ZipEntry entry = zipFileEntries.nextElement();
			String currentEntry = entry.getName();
			monitor.subTask(currentEntry);
			File destFile = new File(outDir, currentEntry);
			// destFile = new File(newPath, destFile.getName());
			File destinationParent = destFile.getParentFile();

			// create the parent directory structure if needed
			destinationParent.mkdirs();

			if (!entry.isDirectory()) {
				BufferedInputStream is = new BufferedInputStream(
						zipFile.getInputStream(entry));
				int currentByte;
				// establish buffer for writing file
				byte data[] = new byte[BUFFER];

				// write the current file to disk
				FileOutputStream fos = new FileOutputStream(destFile);
				BufferedOutputStream dest = new BufferedOutputStream(fos,
						BUFFER);

				// read and write until last byte is encountered
				while ((currentByte = is.read(data, 0, BUFFER)) != -1) {
					dest.write(data, 0, currentByte);
				}
				dest.flush();
				dest.close();
				is.close();
			}
			monitor.worked(1);
		}
		return outDir;
	}

	/**
	 * @param progressMonitor
	 * @throws IOException
	 */
	public void uploadFundus(ProgressMonitor progressMonitor) throws IOException {
		HttpPost post = new HttpPost(doriURL + "/upload/receiveFundus");

		MultipartEntity entity = new MultipartEntity();

		progressMonitor.startTask(0, "Uploading " + photos.size() + " Fundus Photos...");

		for (FundusPhoto f : photos) {
			entity.addPart(f.createKey(),
					new FileBody(f.getFundusFile(), f.getFormat()));
		}
		post.setEntity(entity);
		System.out.println("Sending " + entity.getContentLength() + " to "
				+ post.getURI().toString());

		long t1 = System.currentTimeMillis();
		HttpResponse response = httpClient.execute(post);

		System.out.println("upload took "
				+ (System.currentTimeMillis() - t1) + "ms");
		System.out.println(response.getStatusLine().toString());
		response.getEntity().writeTo(System.out);
		checkUploadResponse(response);
	}

	/**
	 * Reset all current data, preparing for another upload attempt.
	 */
	public void reset() {
		photos.clear();
	}

	/**
	 * Contacts the DORI service to get an EFID.
	 * @return
	 */
	public String createEFID() throws IOException {
		HttpGet get = new HttpGet(doriURL + "/efid/issue");
		HttpResponse response = httpClient.execute(get);
		// The response we expect is 302 Moved Temporarily
		if (response.getStatusLine().getStatusCode() != 200) {
			throw new IOException("Unexpected response from server: " + response.getStatusLine());
		}
		// Grab the data from the response
		InputStream data = response.getEntity().getContent();
		// Just assume it's UTF-8 and read the first line
		BufferedReader in = new BufferedReader(new InputStreamReader(data, "UTF-8"));
		String result = in.readLine();
		in.close();
		return result;
	}
}
