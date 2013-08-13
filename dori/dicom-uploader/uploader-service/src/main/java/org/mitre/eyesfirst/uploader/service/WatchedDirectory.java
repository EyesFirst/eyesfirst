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

package org.mitre.eyesfirst.uploader.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import name.pachler.nio.file.WatchEvent;
import name.pachler.nio.file.WatchKey;

import org.mitre.eyesfirst.FileCollection;
import org.mitre.eyesfirst.MergeFileCollection;
import org.mitre.eyesfirst.Uploader;
import org.mitre.eyesfirst.ZipFileCollection;
import org.mitre.eyesfirst.ui.LoggingProgressMonitor;
import org.mitre.eyesfirst.ui.ProgressMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WatchedDirectory extends WatchedItem {
	/**
	 * Individual files inside the watched directory, used to determine when a
	 * file has been "finished" so that we can try and figure out when a copy
	 * has finished.
	 * @author dpotter
	 *
	 */
	private static class WatchedFile {
		private File file;
		private long oldSize;
		private long lastModified;
		public WatchedFile(File file) {
			this.file = file;
			// The old size is set to -1 so that checking hasChanged for a newly
			// created file is ALWAYS true the first time.
			oldSize = -1;
			// The last modified time is set to -1 so that checking hasChanged
			// for a newly changed file is ALWAYS true the first time.
			lastModified = 0;
		}

		/**
		 * Determines if the file has changed since the last time this method
		 * was called.
		 * @return
		 */
		public boolean hasChanged() {
			// Note: Checking to see if the size has changed doesn't work under
			// Windows. Apparently the copy operation sets the file to the new
			// size, and then writes the copied data over. I guess this makes
			// sense as a "fail-fast" way of making sure a copy won't fail due
			// to a full disk?
			long size = file.length();
			long modified = file.lastModified();
			// Rather than check to see if it's newer or anything, just check if
			// it's different. This allows for weird edge cases like the clock
			// going backwards.
			if (size != oldSize || modified != lastModified) {
				oldSize = size;
				lastModified = modified;
				return true;
			} else {
				return false;
			}
		}
	}

	private final Logger log = LoggerFactory.getLogger(getClass());
	// TODO: When using the Java API, use the Path API properly
	private final File path;
	private final EyesFirstUploaderBackgroundService service;
	private final List<WatchedFile> watchedFiles;
	private final WatchKey key;
	private String efid;
	private String status;
	private Thread pollThread;
	private boolean pollThreadAlive;

	public WatchedDirectory(EyesFirstUploaderBackgroundService service, File path, WatchKey key) {
		if (path == null || service == null)
			throw new NullPointerException();
		this.path = path;
		this.service = service;
		this.key = key;
		watchedFiles = new ArrayList<WatchedFile>();
		scan();
	}

	synchronized void stopWatching() {
		log.info("Canceling monitoring of {} (was removed)", path.getPath());
		if (key != null)
			key.cancel();
		pollThreadAlive = false;
		if (pollThread != null)
			pollThread.interrupt();
	}

	public String getPath() {
		return path.getPath();
	}

	@Override
	public void watchedItemCreated(EyesFirstUploaderBackgroundService sender, WatchEvent<?> event) throws IOException {
		// Throw the file over to checkFile to see what we do with it
		checkFile(new File(path, event.context().toString()));
		checkUpload();
	}

	@Override
	public void watchedItemRemoved(EyesFirstUploaderBackgroundService sender,
			WatchEvent<?> event) throws IOException {
		checkFileRemoved(new File(path, event.context().toString()));
	}

	/**
	 * Scans the directory for files that can be uploaded via the uploader. This
	 * is done automatically when the directory is created and will actually
	 * try and "guess" if ZIP files in the directory match the IMG/DCM zips.
	 */
	public synchronized void scan() {
		File[] files = path.listFiles();
		for (File f : files) {
			checkFile(f);
		}
		// Now that we've checked files, see if we want to upload something.
		checkUpload();
	}

	/**
	 * Check a given file and take action based on what it is.
	 * @param f
	 */
	private synchronized void checkFile(File f) {
		String name = f.getName();
		log.debug("Checking {}", name);
		if (name.equals("efid.txt")) {
			// The file contents are the EFID for this patient. Use it as we
			// end up having to generate it prior to ACTUALLY attempting the
			// upload.
			String newEfid = readFile(f);
			// Because the EFID gets set by the upload thread and WILL trigger
			// us in this thread, lock on the upload thread so nothing weird
			// happens.
			synchronized(this) {
				if (efid != null) {
					if (!efid.equals(newEfid)) {
						log.warn("A new EFID file was found which changes the EFID from {} to {}! Using the new one!", efid, newEfid);
					}
				}
				efid = newEfid;
				log.debug("EFID set to {} from efid.txt", efid);
			}
		} else if (name.equals("status.txt")) {
			// Check this file to see what the upload status is. This file tells
			// us whether we've already attempted the upload or not.
			// Lock for the same reasons EFID is locked.
			synchronized (this) {
				status = readFile(f);
				log.debug("Status is {}", status);
			}
		} else if (name.length() > 4 && name.substring(name.length()-4).equalsIgnoreCase(".ZIP")) {
			// Throw this into the ZIP file list to be monitored.
			watchedFiles.add(new WatchedFile(f));
		}
	}
	/**
	 * Check a given file and take action based on what it is.
	 * @param f
	 */
	private synchronized void checkFileRemoved(File f) {
		String name = f.getName();
		log.debug("Checking removed file {}", name);
		if (name.equals("efid.txt")) {
			// Indicates the EFID file was deleted - so clear the EFID to
			// indicate it should be regenerated.
			synchronized(this) {
				efid = null;
				log.debug("EFID set to null (efid.txt deleted)");
			}
		} else if (name.equals("status.txt")) {
			// Status was removed
			synchronized (this) {
				status = null;
				log.debug("Status is set to null (status.txt deleted)");
			}
		} else if (name.length() > 4 && name.substring(name.length()-4).equalsIgnoreCase(".ZIP")) {
			log.debug("Removing {} as it no longer exists.", f.getPath());
			// Find the watched file that contains this path and remove it
			Iterator<WatchedFile> iter = watchedFiles.iterator();
			while (iter.hasNext()) {
				WatchedFile wf = iter.next();
				if (wf.file.equals(f)) {
					iter.remove();
				}
			}
		}
	}

	private synchronized void checkUpload() {
		if (watchedFiles.size() > 0) {
			if (status != null && status.equals("Complete")) {
				log.info("Status is marked as complete, so not attempting to reupload files. (Delete status.txt to force a reattempt.)");
				return;
			}
			// If we have any files to attempt an upload with, go for it.
			// Well, maybe: first see if we have a thread waiting. If we do, we
			// just don't do anything.
			if (pollThread != null) {
				log.debug("Have files to watch, but not doing anything because there is an update thread waiting.");
				return;
			}
			// Otherwise, create the poll thread and let it do whatever it's
			// going to do.
			pollThread = new Thread(new PollRunnable());
			pollThread.start();
		} else {
			log.debug("Not attempting an upload because there are no files to upload.");
		}
	}

	/**
	 * Runnable for actually doing the upload and polling for updates.
	 * @author dpotter
	 *
	 */
	private class PollRunnable implements Runnable {
		public void run() {
			try {
				boolean alive = true;
				synchronized(WatchedDirectory.this) {
					pollThreadAlive = true;
				}
				// See if we can open all the files. Thankfully with ZIP files
				// (assuming they weren't streamed) even if we are allowed to
				// open the files while they're still being copied, the ZIP
				// header is at the end and so it should still fail ANYWAY if
				// we can't "really" open the files.
				long lastChangeTime = System.currentTimeMillis();
				while (alive) {
					WatchedFile[] files;
					synchronized(WatchedDirectory.this) {
						files = watchedFiles.toArray(new WatchedFile[watchedFiles.size()]);
						alive = pollThreadAlive;
					}
					if (!alive)
						break;
					/*
					 * FIXME: The old method of trying to detect file changes
					 * didn't work, but rather than try the new method, let's
					 * just not bother checking for changes
					boolean haveChanged = false;
					for (int i = 0; i < files.length; i++) {
						if (files[i].hasChanged()) {
							haveChanged = true;
							// Keep going anyway to make sure the "last known
							// size" is updated in all the watched files.
						}
					}
					if (haveChanged) {*/
						log.debug("Reattempting to open...");
						FileCollection collection = attemptOpen(files);
						// Update change time
						lastChangeTime = System.currentTimeMillis();
						if (collection != null) {
							log.info("All files in {} were able to be opened, attempting upload.", path.getPath());
							try {
								attemptUpload(collection);
							} catch (Exception e) {
								log.warn("Upload attempt failed! Giving up.", e);
							} finally {
								// Try and close the collection
								try {
									collection.close();
								} catch (IOException e) {
									log.warn("Unable to close file collection.", e);
								}
							}
							break;
						}
					/*} else {*/
						if (System.currentTimeMillis() - lastChangeTime > service.getMaxWaitTimeForCompletion()) {
							log.warn("Maximum wait time for files to complete has passed, giving up!");
							break;
						}
					/*}*/
					log.debug("Unable to open everything, waiting...");
					try {
						Thread.sleep(service.getUpdatePollInterval());
					} catch (InterruptedException e) {
						// Ignore
					}
				}
			} finally {
				synchronized(WatchedDirectory.this) {
					// When we terminate, blank out the pollThread so it can be
					// recreated if necessary.
					pollThread = null;
				}
			}
		}
		private FileCollection attemptOpen(WatchedFile[] files) {
			ZipFileCollection[] collections = new ZipFileCollection[files.length];
			int compact = 0;
			for (int i = 0; i < files.length; i++) {
				try {
					collections[i] = new ZipFileCollection(files[i].file);
				/*
				 * Apparently Windows will throw a FileNotFoundException when
				 * with an error description indicating that the file is in use
				 * by another process. WTF?!
				} catch (FileNotFoundException fnfe) {
					// Oops - file was deleted and we didn't detect
					log.warn("File {} was not found, ignoring it!", files[i].file.getPath());
					removeWatchedFile(files[i]);
					compact++;*/
				} catch (IOException e) {
					log.debug("Unable to open file", e);
					// At this point, we need to close whatever we did create.
					for (i--; i >= 0; i--) {
						try {
							collections[i].close();
						} catch (IOException e2) {
							log.debug("Unable to close ZIP", e2);
						}
					}
					return null;
				}
			}
			if (compact > 0) {
				// We need to remove dead entries
				if (compact == files.length) {
					// Woops.
					return null;
				}
				ZipFileCollection[] compactedCollections = new ZipFileCollection[files.length - compact];
				int compactIndex = 0;
				for (int i = 0; i < collections.length; i++) {
					if (collections[i] != null) {
						compactedCollections[compactIndex] = collections[i];
						compactIndex++;
					}
				}
				collections = compactedCollections;
			}
			return new MergeFileCollection(collections);
		}
	}

	private synchronized void removeWatchedFile(WatchedFile file) {
		watchedFiles.remove(file);
	}

	private void attemptUpload(FileCollection files) throws IOException {
		Uploader uploader = new Uploader(service.getDoriURL());
		log.debug("Attempting to log in to DORI...");
		uploader.login(service.getDoriUser(), service.getDoriPassword());
		String localEfid;
		synchronized(this) {
			// Lock to get the EFID...
			localEfid = efid;
		}
		// Do NOT lock while we attempt to create an EFID off the network, if
		// necessary
		if (localEfid == null) {
			log.debug("Attempting to generate EFID");
			localEfid = uploader.createEFID();
			log.debug("Generated EFID {}", localEfid);
			// Lock while we write the EFID to a file so that we don't clobber
			// it when the file is created
			synchronized(this) {
				efid = localEfid;
				// Write the EFID to the directory
				File efidFile = new File(path, "efid.txt");
				OutputStream out = new FileOutputStream(efidFile);
				try {
					out.write(localEfid.getBytes("UTF-8"));
				} finally {
					out.close();
				}
			}
		}
		// Now that we have the EFID, we can attempt the upload
		ProgressMonitor monitor = new LoggingProgressMonitor(log);
		uploader.process(files, localEfid, monitor);
		uploader.upload(localEfid, monitor);
		synchronized (this) {
			// Lock while we write the status file
			// Write the EFID to the directory
			File statusFile = new File(path, "status.txt");
			Writer writer = new OutputStreamWriter(new FileOutputStream(statusFile), "UTF-8");
			try {
				writer.write("Complete");
				String nl = System.getProperty("line.separator", "\n");
				writer.write(nl);
				writer.write(nl);
				writer.write("The upload has been completed under EFID ");
				writer.write(localEfid);
				writer.write('.');
				writer.write(nl);
				writer.write("If you would like to reupload these files, you need to delete this file.");
			} finally {
				writer.close();
			}
		}
	}

	private String readFile(File f) {
		BufferedReader reader = null;
		String result = null;
		try {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"));
			result = reader.readLine();
			// Anything after the first line can be ignored, so ignore it.
		} catch (IOException e) {
			log.error("Error reading " + f.getPath(), e);
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					log.warn("Unable to close " + f.getPath(), e);
				}
			}
		}
		return result;
	}
}
