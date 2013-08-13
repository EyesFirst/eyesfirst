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
import java.util.Collections;
import java.util.List;

/**
 * Combines multiple file collections into a single "collection." Note that
 * each subcollection has an index added to the "paths" used by this collection,
 * so that even if two collections contain files of the same name, they will be
 * reported separately by this.
 * @author dpotter
 *
 */
public class MergeFileCollection extends FileCollection {
	private List<FileCollection> collections = new ArrayList<FileCollection>();

	public MergeFileCollection(FileCollection... collections) {
		for (FileCollection c : collections)
			addFileCollection(c);
	}

	public void addFileCollection(FileCollection subCollection) {
		if (subCollection == null)
			throw new NullPointerException();
		collections.add(subCollection);
	}

	@Override
	public List<String> getFileNames() throws IOException {
		if (collections.isEmpty())
			return Collections.emptyList();
		List<String> names = new ArrayList<String>();
		for (int i = 0; i < collections.size(); i++) {
			String prefix = i + "/";
			List<String> subnames = collections.get(i).getFileNames();
			for (String name : subnames) {
				names.add(prefix + name);
			}
		}
		return names;
	}

	@Override
	public InputStream openFileAsStream(String name) throws IOException {
		int i = name.indexOf('/');
		if (i < 0)
			return null;
		try {
			int collection = Integer.parseInt(name.substring(0, i));
			if (collection < 0 || collection >= collections.size())
				return null;
			return collections.get(collection).openFileAsStream(name.substring(i+1));
		} catch (NumberFormatException e) {
			return null;
		}
	}

	@Override
	public File openFile(String name) throws IOException {
		int i = name.indexOf('/');
		if (i < 0)
			return null;
		try {
			int collection = Integer.parseInt(name.substring(0, i));
			if (collection < 0 || collection >= collections.size())
				return null;
			return collections.get(collection).openFile(name.substring(i+1));
		} catch (NumberFormatException e) {
			return null;
		}
	}

	@Override
	public void close() throws IOException {
		List<IOException> errors = null;
		for (FileCollection collection : collections) {
			try {
				collection.close();
			} catch (IOException e) {
				if (errors == null)
					errors = new ArrayList<IOException>();
				errors.add(e);
			}
		}
		if (errors != null) {
			if (errors.size() == 1)
				throw errors.get(0);
			else {
				StringBuilder m = new StringBuilder("Multiple I/O exceptions occurred while closing streams! ");
				for (int i = 0; i < errors.size(); i++) {
					if (i > 0)
						m.append(", ");
					m.append(errors.get(i).toString());
				}
				throw new IOException(m.toString());
			}
		}
	}
}
