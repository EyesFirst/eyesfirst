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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import name.pachler.nio.file.ClosedWatchServiceException;
import name.pachler.nio.file.FileSystems;
import name.pachler.nio.file.Paths;
import name.pachler.nio.file.StandardWatchEventKind;
import name.pachler.nio.file.WatchEvent;
import name.pachler.nio.file.WatchKey;
import name.pachler.nio.file.WatchService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EyesFirstUploaderBackgroundService {
	private final Logger log = LoggerFactory.getLogger(getClass());
	private Thread thread;
	private WatchService watchService;
	private IdentityHashMap<WatchKey, WatchedItem> watched;
	private Map<String, WatchedDirectory> watchedDirectories;
	private AtomicBoolean alive = new AtomicBoolean(false);
	/**
	 * Root of the DORI webservice.
	 */
	private String doriURL = "http://localhost:8080/doriweb/";
	private String doriUser = "admin";
	private String doriPassword = "admin";
	/**
	 * Maximum amount of time in milliseconds to wait before deciding an upload
	 * we can't currently use has failed. The default is 5 minutes.
	 */
	private long maxWaitTimeForCompletion = 5L*60L*1000L;
	/**
	 * Amount of time to wait before retrying a file. The default is 5 seconds.
	 */
	private long updatePollInterval = 5L*1000L;

	public EyesFirstUploaderBackgroundService() {
		watchService = FileSystems.getDefault().newWatchService();
		watched = new IdentityHashMap<WatchKey, WatchedItem>();
		watchedDirectories = new HashMap<String, WatchedDirectory>();
	}

	/**
	 * Tells the service to watch the given path. Folders in this path will be
	 * watched to determine if they contain uploaded information.
	 * 
	 * @param path
	 *            the path to watch
	 * @throws IOException
	 *             if an I/O error occurs while attempting to start monitoring
	 *             the path
	 */
	public synchronized void watchPath(String path) throws IOException {
		WatchKey key = Paths.get(path).register(watchService, StandardWatchEventKind.ENTRY_CREATE, StandardWatchEventKind.ENTRY_DELETE);
		watched.put(key, new WatchedRoot(path));
		// Start watching any child folders in this path
		File watchedDir = new File(path);
		for (File f : watchedDir.listFiles()) {
			if (f.isDirectory()) {
				// Watch this
				startWatching(f);
			}
		}
	}

	void startWatching(File path) throws IOException {
		WatchKey key = Paths.get(path.getPath()).register(watchService, StandardWatchEventKind.ENTRY_CREATE, StandardWatchEventKind.ENTRY_DELETE);
		WatchedDirectory directory = new WatchedDirectory(this, path, key);
		watched.put(key, directory);
		watchedDirectories.put(path.getPath(), directory);
		log.info("Now monitoring {}", path.getPath());
	}

	void stopWatching(File path) throws IOException {
		// We very well might not have anything associated with this path, as
		// it's been removed. If we do, stop watching it.
		WatchedDirectory d = watchedDirectories.get(path.getPath());
		if (d != null) {
			d.stopWatching();
			log.info("Stopped monitoring {} (was removed)", path.getPath());
		}
	}

	/**
	 * Starts the service running.
	 */
	public synchronized void start() {
		// If we're already running, just return
		if (thread != null)
			return;
		// Otherwise, start up the system
		log.info("Starting up service...");
		thread = new Thread("EyesFirstUploaderBackgroundService") {
			public void run() {
				EyesFirstUploaderBackgroundService.this.run();
			}
		};
		alive.set(true);
		thread.start();
	}

	/**
	 * Stops the thread (if it's running), halting listening for new items.
	 */
	public synchronized void shutdown() {
		if (thread != null) {
			log.info("Telling service to terminate.");
			alive.set(false);
			thread.interrupt();
			thread = null;
		}
	}

	/**
	 * Polls for events.
	 */
	private void run() {
		log.info("Starting to monitor directories...");
		do {
			try {
				WatchKey key = watchService.take();
				// Try and grab our local context for this
				WatchedItem item = watched.get(key);
				if (item == null) {
					log.warn("Got a watch event for an unknown context.");
				} else {
					for (WatchEvent<?> event : key.pollEvents()) {
						// See what this key is for.
						try {
							if (event.kind() == StandardWatchEventKind.ENTRY_CREATE) {
								item.watchedItemCreated(this, event);
							} else if (event.kind() == StandardWatchEventKind.ENTRY_DELETE) {
								item.watchedItemRemoved(this, event);
							}
						} catch (Exception e) {
							log.warn("Error running watched event handler", e);
						}
					}
				}
				key.reset();
			} catch (ClosedWatchServiceException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// We don't care about this exception. It's probably caused by
				// shutdown().
				if (!alive.get())
					break;
			}
		} while (alive.get());
	}

	public String getDoriURL() {
		return doriURL;
	}

	public void setDoriURL(String doriURL) {
		this.doriURL = doriURL;
	}

	public String getDoriUser() {
		return doriUser;
	}

	public void setDoriUser(String doriUser) {
		this.doriUser = doriUser;
	}

	public String getDoriPassword() {
		return doriPassword;
	}

	public void setDoriPassword(String doriPassword) {
		this.doriPassword = doriPassword;
	}

	public long getMaxWaitTimeForCompletion() {
		return maxWaitTimeForCompletion;
	}

	public void setMaxWaitTimeForCompletion(long maxWaitTimeForCompletion) {
		this.maxWaitTimeForCompletion = maxWaitTimeForCompletion;
	}

	public long getUpdatePollInterval() {
		return updatePollInterval;
	}

	public void setUpdatePollInterval(long updatePollInterval) {
		this.updatePollInterval = updatePollInterval;
	}

	public static void main(String[] args) {
		EyesFirstUploaderBackgroundService service = new EyesFirstUploaderBackgroundService();
		for (int i = 0; i < args.length; i++) {
			try {
				service.watchPath(args[i]);
			} catch (IOException e) {
				System.err.println("Cannot watch path " + args[i] + ": " + e);
				e.printStackTrace();
			}
		}
		service.start();
	}
}
