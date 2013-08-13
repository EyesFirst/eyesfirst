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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

public class ZipFileCollection extends FileCollection {
	private final ZipFile zip;

	public ZipFileCollection(File file) throws ZipException, IOException {
		this(new ZipFile(file));
	}

	public ZipFileCollection(ZipFile zip) {
		if (zip == null)
			throw new NullPointerException();
		this.zip = zip;
	}

	@Override
	public List<String> getFileNames() throws IOException {
		List<String> res = new ArrayList<String>(zip.size());
		Enumeration<? extends ZipEntry> e = zip.entries();
		while (e.hasMoreElements()) {
			res.add(e.nextElement().getName());
		}
		return res;
	}

	@Override
	public InputStream openFileAsStream(String name) throws IOException {
		ZipEntry entry = zip.getEntry(name);
		return entry == null ? null : zip.getInputStream(entry);
	}

	/**
	 * Close the zip file. This makes all future calls on this fail.
	 * @throws IOException
	 */
	public void close() throws IOException {
		zip.close();
	}
}
