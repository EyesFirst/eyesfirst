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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * Interface used to provide basic access to "a collection of files."
 * @author dpotter
 */
public abstract class FileCollection {
	/**
	 * Close the file collection, releasing any resources related to it. Once
	 * close has been called, any further calls to any method in the
	 * collection must throw an {@code IOException}.
	 */
	public abstract void close() throws IOException;

	/**
	 * Get a list of all file names in this collection.
	 * @return
	 * @throws IOException
	 */
	public abstract List<String> getFileNames() throws IOException;

	/**
	 * Open a stream to the given file.
	 * 
	 * @param name
	 * @return a stream for reading the file data, or {@code null} if the
	 *         requested file does not exist
	 * @throws IOException
	 */
	public abstract InputStream openFileAsStream(String name) throws IOException;

	/**
	 * Open the given file as a file. <strong>Note:</strong> this may cause
	 * the file to be extracted or otherwise processed beyond what
	 * {@link #openFileAsStream(String)} would do. The default implementation
	 * creates a temp file and writes the contents of {@link #openFileAsStream(String)} to it.
	 * The temp file will be deleted on exit (deleteOnExit will have already
	 * been called).
	 * @param name the name of the file to open
	 * @return the data as a {@code File} or {@code null} if the file doesn't exist
	 * @throws IOException
	 */
	public File openFile(String name) throws IOException {
		// Try and grab the input stream FIRST, so that an "already closed"
		// exception or "not found" can be handled before creating the temp
		// file.
		InputStream in = openFileAsStream(name);
		if (in == null)
			return null;
		OutputStream out = null;
		try {
			String extension;
			String basename;
			int i = name.lastIndexOf('/');
			basename = i >= 0 ? name.substring(i+1) : name;
			i = basename.lastIndexOf('.');
			extension = i >= 0 ? basename.substring(i) : ".tmp";
			basename = i >= 0 ? basename.substring(0, i) : basename;
			File res = File.createTempFile(basename, extension);
			res.deleteOnExit();
			out = new FileOutputStream(res);
			byte[] b = new byte[1024];
			while (true) {
				int r = in.read(b);
				if (r < 0)
					break;
				out.write(b, 0, r);
			}
			return res;
		} finally {
			try {
				in.close();
			} catch (IOException e) {
				
			}
			try {
				if (out != null)
					out.close();
			} catch (IOException e) {
				
			}
		}
	}
}
