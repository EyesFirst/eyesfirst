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

import java.io.IOException;

import name.pachler.nio.file.WatchEvent;

/**
 * Root class of items that the service watches.
 * @author dpotter
 */
public abstract class WatchedItem {
	/**
	 * Receive notification that the watched item had something created in it.
	 * @param event the event that occurred
	 * @throws IOException if an I/O error occurs while handling this
	 */
	public abstract void watchedItemCreated(EyesFirstUploaderBackgroundService sender, WatchEvent<?> event) throws IOException;
	/**
	 * Receive notification that the watched item had something removed from it.
	 * @param event the event that occurred
	 * @throws IOException if an I/O error occurs while handling this
	 */
	public abstract void watchedItemRemoved(EyesFirstUploaderBackgroundService sender, WatchEvent<?> event) throws IOException;
}
