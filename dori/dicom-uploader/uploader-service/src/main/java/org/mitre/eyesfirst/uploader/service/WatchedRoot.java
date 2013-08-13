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

import name.pachler.nio.file.WatchEvent;

/**
 * This object represents a watched root directory and is used to maintain
 * state about that. When new directories are created in it, it creates a new
 * {@link WatchedDirectory} to handle looking at that.
 * @author dpotter
 *
 */
public class WatchedRoot extends WatchedItem {
	// TODO: When using the Java API, use the Path API properly
	private final File path;

	public WatchedRoot(String path) {
		if (path == null)
			throw new NullPointerException();
		this.path = new File(path);
	}

	@Override
	public void watchedItemCreated(EyesFirstUploaderBackgroundService sender, WatchEvent<?> event) throws IOException {
		File child = new File(path, event.context().toString());
		if (child.isDirectory()) {
			// Start watching that, too.
			sender.startWatching(child);
		}
	}

	@Override
	public void watchedItemRemoved(EyesFirstUploaderBackgroundService sender,
			WatchEvent<?> event) throws IOException {
		File child = new File(path, event.context().toString());
		// We can't know whether or not this was a directory - it's been deleted.
		// Just try and remove it regardless.
		sender.stopWatching(child);
	}
}
