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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.http.entity.mime.content.FileBody;

/**
 * An extension of FileBody that monitors progress of writing data to the
 * underlying stream.
 * @author dpotter
 *
 */
public class MonitoredFileBody extends FileBody {
	private final LargeProgressMonitor monitor;

	public MonitoredFileBody(File file, String filename, String mimeType,
			String charset, LargeProgressMonitor monitor) {
		super(file, filename, mimeType, charset);
		if (monitor == null)
			throw new NullPointerException("progress monitor");
		this.monitor = monitor;
	}

	public MonitoredFileBody(File file, String mimeType, String charset, LargeProgressMonitor monitor) {
		super(file, mimeType, charset);
		if (monitor == null)
			throw new NullPointerException("progress monitor");
		this.monitor = monitor;
	}

	public MonitoredFileBody(File file, String mimeType, LargeProgressMonitor monitor) {
		super(file, mimeType);
		if (monitor == null)
			throw new NullPointerException("progress monitor");
		this.monitor = monitor;
	}

	public MonitoredFileBody(File file, LargeProgressMonitor monitor) {
		super(file);
		if (monitor == null)
			throw new NullPointerException("progress monitor");
		this.monitor = monitor;
	}

	@Override
	public void writeTo(OutputStream out) throws IOException {
		if (out == null)
			throw new NullPointerException();
		InputStream in = new FileInputStream(getFile());
		try {
			byte[] buf = new byte[4*1024];
			// In order to defer the last blast of progress update until
			// AFTER we've flushed, we maintain a "last worked" and only send it
			// after we've read in the next chunk.
			int worked = 0, r = 0;
			while (true) {
				r = in.read(buf);
				if (r < 0) {
					// Break now, we've read in everything, we'll inform the
					// progress monitor of the last worked AFTER we flush.
					break;
				}
				// Otherwise, inform the progress monitor of our work.
				monitor.worked(worked);
				out.write(buf, 0, r);
				// Update the worked for the next pass through the loop.
				worked = r;
			}
			out.flush();
			monitor.worked(worked);
		} finally {
			in.close();
		}
	}
}
