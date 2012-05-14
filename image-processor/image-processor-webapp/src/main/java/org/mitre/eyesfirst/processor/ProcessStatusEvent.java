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
package org.mitre.eyesfirst.processor;

import java.util.EventObject;

public class ProcessStatusEvent extends EventObject {
	/**
	 * 
	 */
	private static final long serialVersionUID = -6024746454733662741L;
	private final AbstractProcess.Status oldStatus;
	private final AbstractProcess.Status newStatus;

	public ProcessStatusEvent(AbstractProcess source, AbstractProcess.Status oldStatus, AbstractProcess.Status newStatus) {
		super(source);
		this.oldStatus = oldStatus;
		this.newStatus = newStatus;
	}

	public AbstractProcess.Status getOldStatus() {
		return oldStatus;
	}

	/**
	 * Gets the new status (the status that the source model currently is if
	 * it were
	 * @return
	 */
	public AbstractProcess.Status getNewStatus() {
		return newStatus;
	}
}
