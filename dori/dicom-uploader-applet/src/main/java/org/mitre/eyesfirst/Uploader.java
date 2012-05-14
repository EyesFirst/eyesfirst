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
package org.mitre.eyesfirst;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.dcm4che2.data.DicomObject;

import com.google.common.io.Files;

public class Uploader {
	public static String HOSTNAME = "";
	public static Map<String, String> queryMap;
	public static String cookieParam;
	public static Map<String, String> studyUidMap, seriesUidMap;
	/**
	 * @deprecated Don't store static data
	 */
	@Deprecated
	private static List<FundusPhoto> photos = new ArrayList<FundusPhoto>();
	private File outFolder;
	private File fundusDir;
	private HttpClient httpClient = new DefaultHttpClient();

	void delTree(File dir) {
		for (File f : dir.listFiles()) {
			if (f.isDirectory()) {
				delTree(f);
			}
			f.delete();
		}
	}

	public List<DicomObject> process(File dcmFile, File imgFile, String efid, ProgressMonitor monitor) throws IOException {
		outFolder = Files.createTempDir();

		ZipFile dcmZip = new ZipFile(dcmFile);
		ZipFile imgZip = new ZipFile(imgFile);
		monitor.startTask(dcmZip.size() + imgZip.size(), "Unzipping files...");

		File dcmOutDir = extractFolder(dcmZip, monitor);
		File imgOutDir = extractFolder(imgZip, monitor);

		monitor.startTask(0, "Preparing DICOM files for upload...");

		queryMap = new HashMap<String, String>();
		studyUidMap = new HashMap<String, String>();
		seriesUidMap = new HashMap<String, String>();
		List<DicomObject> results = new ArrayList<DicomObject>();

		MergeZeissDcmImg.convert(dcmOutDir, imgOutDir, outFolder,
				"1.2.840.10008.1.2.4.90", efid, monitor, results);
		return results;
	}

	public List<FundusPhoto> processFundus(File fundusFile) throws IOException {

		if (!fundusFile.isDirectory()) {
			fundusDir = extractFolder(new ZipFile(fundusFile), new NullProgressMonitor());
		} else {
			fundusDir = fundusFile;
		}

		// FIXME: These use the static reference, but a future version is likely
		// to change that to be an instance variable.
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
	@Deprecated
	public static void associateFundus(Date date, String queryString,
			String laterality) {
		if (photos != null) {
			for (FundusPhoto f : photos) {
				if (f.isSameDay(date) || f.isSameLaterality(laterality)) {
					f.addQueryString(queryString);
				}
			}
		}
	}

	public void upload(String efid, ProgressMonitor progress) throws IOException {
		HttpPost post = new HttpPost(HOSTNAME + "/upload/receive");

		MultipartEntity entity = new MultipartEntity();

		ShrinkingLargeProgessMonitor largeProgress = new ShrinkingLargeProgessMonitor(progress);
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
		post.setHeader("Cookie", cookieParam);
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
	 * @deprecated Replaced with {@link #uploadFundus(List, ProgressMonitor)}.
	 * @param progressMonitor
	 * @throws IOException
	 */
	@Deprecated
	public void uploadFundus(ProgressMonitor progressMonitor) throws IOException {
		uploadFundus(photos, progressMonitor);
	}
	public void uploadFundus(List<FundusPhoto> photos, ProgressMonitor progress) throws IOException {
		HttpPost post = new HttpPost(HOSTNAME + "/upload/receiveFundus");

		MultipartEntity entity = new MultipartEntity();

		progress.startTask(0, "Uploading " + photos.size() + " Fundus Photos...");

		for (FundusPhoto f : photos) {
			entity.addPart(f.createKey(),
					new FileBody(f.getFundusFile(), f.getFormat()));
		}
		post.setEntity(entity);
		post.setHeader("Cookie", cookieParam);
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
}
