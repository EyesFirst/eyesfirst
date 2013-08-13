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

package org.mitre.eyesfirst.dicom.image;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import org.mitre.eyesfirst.dicom.BasicDicomImage;
import org.mitre.eyesfirst.dicom.DicomImage;
import org.mitre.eyesfirst.dicom.DicomImageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple WADO implementation for retrieving a DICOM object.
 *
 * @author dpotter
 */
public class WADOService implements DicomAccessService {
	private Logger logger = LoggerFactory.getLogger(getClass());
	private URI wadoURI;

	public WADOService(String wadoURI) throws URISyntaxException {
		this(new URI(wadoURI));
	}
	
	public WADOService(URI wadoURI) {
		if (wadoURI == null)
			throw new NullPointerException();
		this.wadoURI = wadoURI;
	}

	@Override
	public DicomImage retrieveDicomObject(String studyUID, String seriesUID,
			String objectUID) throws DicomAccessException {
		// Q: Isn't a query string supposed to replace the existing query
		// string, so resolving "?quz" against "http://example.com/foo?bar"
		// becomes "http://example.com/foo?quz"?
		// A: According to RFC3986, section 5.3, yes. According to Java, it's
		// "http://example.com/?quz".
		// So we can't just build the query string and resolve against the URI.
		//
		// OK, fine, I'll just clone the URI and set the new query part.
		//
		// Q: Can you clone a URI?
		// A: Not in Java.
		//
		// ......
		StringBuilder request = new StringBuilder(wadoURI.toString());
		try {
			request.append("?requestType=WADO&contentType=application/dicom&studyUID=");
			request.append(URLEncoder.encode(studyUID, "UTF-8"));
			request.append("&seriesUID=");
			request.append(URLEncoder.encode(seriesUID, "UTF-8"));
			request.append("&objectUID=");
			request.append(URLEncoder.encode(objectUID, "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			throw new Error("UTF-8 must be supported");
		}
		URI requestURI;
		try {
			requestURI = new URI(request.toString());
		} catch (URISyntaxException e1) {
			throw new DicomAccessException("Unable to create request URI", e1);
		}
		logger.debug("Retrieve DICOM URI {}", requestURI);
		try {
			URL url = requestURI.toURL();
			URLConnection connection = url.openConnection();
			connection.connect();
			int size = connection.getContentLength();
			if (size < 0) {
				// 10MB isn't a bad guess for a DICOM image, sadly
				size = 10*1024*1024;
			}
			InputStream in = connection.getInputStream();
			ByteArrayOutputStream buffer = new ByteArrayOutputStream(size);
			byte[] b = new byte[1024];
			while (true) {
				int r = in.read(b);
				if (r < 0)
					break;
				buffer.write(b, 0, r);
			}
			return new BasicDicomImage(buffer.toByteArray());
		} catch (FileNotFoundException e) {
			return null;
		} catch (IOException e) {
			throw new DicomAccessException(e);
		} catch (DicomImageException e) {
			throw new DicomAccessException(e);
		}
	}

	@Override
	public void destroy() {
		// Nothing to do.
	}

}
