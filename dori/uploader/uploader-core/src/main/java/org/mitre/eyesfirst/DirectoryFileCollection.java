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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class DirectoryFileCollection extends FileCollection {
	private final File basedir;
	private boolean closed = false;

	public DirectoryFileCollection(File basedir) {
		if (basedir == null)
			throw new NullPointerException();
		if (!basedir.isDirectory())
			throw new IllegalArgumentException("Path " + basedir.getPath() + " is not a directory");
		this.basedir = basedir;
	}

	@Override
	public void close() throws IOException {
		closed = true;
	}

	@Override
	public List<String> getFileNames() throws IOException {
		if (closed)
			throw new IOException("Collection is closed");
		List<String> res = new ArrayList<String>();
		listFiles(basedir, "", res);
		return res;
	}

	private static void listFiles(File dir, String prefix, List<String> list) {
		for (File f : dir.listFiles()) {
			if (f.isDirectory()) {
				// Don't add it directly, but:
				listFiles(f, prefix + f.getName() + "/", list);
			} else {
				list.add(prefix + f.getName());
			}
		}
	}

	@Override
	public InputStream openFileAsStream(String name) throws IOException {
		if (closed)
			throw new IOException("Collection is closed");
		File f = openFile(name);
		if (f == null)
			return null;
		else
			return new FileInputStream(f);
	}

	@Override
	public File openFile(String name) throws IOException {
		if (closed)
			throw new IOException("Collection is closed");
		File f = new File(basedir, fileCollectionPathToHostPath(name));
		return f.exists() ? f : null;
	}

	/**
	 * Converts a host path to the crossplatform version (translates
	 * File.separator chars to '/')
	 * @param path
	 * @return
	 */
	public static String hostPathToFileCollectionPath(String path) {
		if (File.separatorChar == '/')
			return path;
		else
			return path.replace(File.separatorChar, '/');
	}

	/**
	 * Converts a host path to the crossplatform version (translates
	 * '/' to File.separator)
	 * @param path
	 * @return
	 */
	public static String fileCollectionPathToHostPath(String path) {
		if (File.separatorChar == '/')
			return path;
		else
			return path.replace('/', File.separatorChar);
	}
}
