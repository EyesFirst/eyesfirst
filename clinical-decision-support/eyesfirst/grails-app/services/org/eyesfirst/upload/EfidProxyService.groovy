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
package org.eyesfirst.upload

import org.apache.commons.logging.LogFactory;

class EfidProxyService {
	private static final log = LogFactory.getLog(this);
	def grailsApplication;

	def issueEfid() {
		def issueURL = new URL(grailsApplication.config.eyesfirst.efidService + "/issue");
		log.info("Sending request to $issueURL");
		// Generate a POST request
		URLConnection connection = issueURL.openConnection();
		HttpURLConnection httpConnection;
		if (!(connection instanceof HttpURLConnection)) {
			throw new RuntimeException("Wanted an HTTP URL for EFID service, got " + issueURL);
		}
		httpConnection = (HttpURLConnection)connection;
		httpConnection.requestMethod = "POST";
		connection.setRequestProperty("Content-Type","application/x-www-form-urlencoded");
		connection.doOutput = true;
		connection.setRequestProperty("Content-Length", "0");
		// The post body is actually empty, but grab it and flush it anyway.
		OutputStream postStream = connection.outputStream;
		postStream.flush();
		// And finally, connect
		connection.connect();
		if (httpConnection.responseCode != 200) {
			throw new IOException("Unable to issue ID: error response from server (HTTP " + httpConnection.responseCode + " " + httpConnection.responseMessage + ")");
		}
		// FIXME: Really, I should be grabbing the character set from the
		// server's response, but since there's STILL no API for that in
		// Java...
		def result = new StringBuilder();
		InputStream inStream = null;
		try {
			inStream = connection.inputStream;
			def reader = new InputStreamReader(inStream, "UTF-8");
			def buf = new char[1024];
			while (true) {
				int r = reader.read(buf);
				if (r < 0)
					break;
				result.append(buf, 0, r);
			}
			return result.toString();
		} finally {
			// You'd think there'd be a close method on the connection itself,
			// but no, there isn't. Hopefully this closes the connection, but
			// who knows?
			if (inStream != null) {
				inStream.close();
			}
		}
	}

	def verifyEfid(String efid) {
		// This is much easier than the issue, because it's a simple GET request.
		def verifyURL = new URL(grailsApplication.config.eyesfirst.efidService + "/verify?id=" + URLEncoder.encode(efid, "UTF-8"));
		// Still want the connection...
		URLConnection connection = issueURL.openConnection();
		HttpURLConnection httpConnection;
		if (!(connection instanceof HttpURLConnection)) {
			throw new RuntimeException("Wanted an HTTP URL for EFID service, got " + issueURL);
		}
		connection.connect();
		// And close the connection, the only way that you apparently can:
		connection.inputStream.close();
		if (httpConnection.responseCode == 200) {
			return true;
		} else if (httpConnection.responseCode == 404) {
			return false;
		} else {
			throw new IOException("Unable to verify EFID: error response from server (HTTP " + httpConnection.responseCode + " " + httpConnection.responseMessage + ")");
		}
	}
}
