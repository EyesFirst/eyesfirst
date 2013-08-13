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

package org.mitre.eyesfirst.ui;

/**
 * The default progress monitor can only handle uploading up to 2GB before it
 * will overflow. This can handle up to 2<super>63</super> by using {@code long}
 * math.
 * 
 * @author dpotter
 */
public interface LargeProgressMonitor {
	/**
	 * Sets the current status message. This message generally appears below
	 * the current task message.
	 * 
	 * @param message
	 *            the message to show
	 */
	public void subTask(String message);
	
	/**
	 * Indicates that a new task is starting. (This will reset the progress
	 * monitor to 0.)
	 * 
	 * @param totalUnits
	 *            the total number of work units in this task, or 0 (or a
	 *            negative number) if the total is unknown
	 * @param task
	 *            the name of the task
	 */
	public void startTask(long totalUnits, String task);
	
	/**
	 * Indicate that a given number of work units has been performed.
	 * 
	 * @param workUnits
	 *            the number of work units completed
	 */
	public void worked(long workUnits);
}
