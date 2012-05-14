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

import netscape.javascript.JSObject;

public class JSObjectProgressMonitor implements ProgressMonitor {
	private final JSObject object;

	public JSObjectProgressMonitor(JSObject object) {
		this.object = object;
	}

	@Override
	public void subTask(String message) {
		object.call("subTask", new Object[] { message });
	}

	@Override
	public void startTask(int totalUnits, String task) {
		object.call("startTask", new Object[] { totalUnits, task });
	}

	@Override
	public void worked(int workUnits) {
		object.call("worked", new Object[] { workUnits });
	}
}
