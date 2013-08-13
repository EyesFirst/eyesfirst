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
package org.mitre.eyesfirst.common;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.slf4j.LoggerFactory;

public class MimeTypes {
	private static Map<String, String> extensionToMimeType;
	private static Map<String, String> mimeTypeToExtension;
	static {
		extensionToMimeType = new HashMap<String, String>();
		mimeTypeToExtension = new HashMap<String, String>();
		Properties p = new Properties();
		try {
			p.load(MimeTypes.class.getResourceAsStream("MimeTypes.properties"));
			// This is two-pass: first, generate the extensions to mime types
			// and automatically fill in the reverse.
			for (Map.Entry<?,?> e : p.entrySet()) {
				Object o = e.getKey();
				if (o == null)
					continue;
				String key = o.toString();
				// Extensions must always be lower-case
				key = key.toLowerCase(Locale.US);
				o = e.getValue();
				if (o == null)
					continue;
				String value = o.toString();
				if (key.indexOf('/') < 0) {
					extensionToMimeType.put(key, value);
					mimeTypeToExtension.put(value, key);
				}
			}
			// Once that's done, repeat, overriding any reverse mappings with
			// their more specific versions
			for (Map.Entry<?,?> e : p.entrySet()) {
				Object o = e.getKey();
				if (o == null)
					continue;
				String key = o.toString();
				o = e.getValue();
				if (o == null)
					continue;
				String value = o.toString();
				if (key.indexOf('/') >= 0) {
					// Override whatever we had
					mimeTypeToExtension.put(key, value);
				}
			}
		} catch (IOException e) {
			LoggerFactory.getLogger(MimeTypes.class).error("Unable to load mime type database", e);
		}
	}

	public static final String BINARY_MIME_TYPE = "application/octet-stream";

	private MimeTypes() {
	}

	/**
	 * Gets the MIME type for a given extension. The extension is defined as
	 * "any text past the last {@code '.'} in the given string" and will be
	 * pulled out of the given string.
	 * @param extension
	 * @return
	 */
	public static String getMimeType(String extension) {
		int i = extension.lastIndexOf('.');
		if (i >= 0) {
			extension = extension.substring(i+1);
		}
		// For the extension to lower case if it isn't already
		extension = extension.toLowerCase(Locale.US);
		return extensionToMimeType.get(extension);
	}

	public static String getExtension(String mimeType) {
		return mimeTypeToExtension.get(mimeType);
	}
}
