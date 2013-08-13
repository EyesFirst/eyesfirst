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
 * This "shrinks" a large progress monitor to fit into a normal progress
 * monitor, which is generally speaking the easiest way to deal with tasks
 * that generate an enormous number of work units.
 * @author dpotter
 *
 */
public class ShrinkingLargeProgressMonitor implements LargeProgressMonitor {
	private final ProgressMonitor wrapped;
	private long totalWorked = 0;
	/**
	 * The total number of work units we've sent to the lower progress monitor.
	 */
	private int intTotalWorked = 0;
	/**
	 * Number of bits to shift (this is package-level for the test case).
	 */
	int shift = 0;

	/**
	 * The progress monitor to send work units to.
	 * @param wrapped
	 */
	public ShrinkingLargeProgressMonitor(ProgressMonitor wrapped) {
		this.wrapped = wrapped;
	}

	@Override
	public void subTask(String message) {
		wrapped.subTask(message);
	}

	@Override
	public void startTask(long totalUnits, String task) {
		if (totalUnits < 0)
			throw new IllegalArgumentException("Total works units must be positive.");
		// Calculate the shift. This is inversely related to the number of
		// leading zeros: if we start off with 33 leading zeros, we need no shift,
		// because the value is an int. If we have 32, we need to shift one bit
		// over to fit it into an int. And so on.
		shift = 33 - Long.numberOfLeadingZeros(totalUnits);
		// If it already fits in an int, we use 0.
		if (shift < 0)
			shift = 0;
		// Reset total worked.
		totalWorked = 0;
		intTotalWorked = 0;
		wrapped.startTask((int)(totalUnits >> shift), task);
	}

	@Override
	public void worked(long workUnits) {
		if (workUnits < 0)
			throw new IllegalArgumentException("Going backwards? (Work units may not be negative)");
		// Add up the total number of work units we've worked:
		totalWorked += workUnits;
		// And see how that translates to an int
		int intUnits = (int)(totalWorked >> shift);
		if (intUnits > intTotalWorked) {
			wrapped.worked(intUnits - intTotalWorked);
			intTotalWorked = intUnits;
		}
	}
}
