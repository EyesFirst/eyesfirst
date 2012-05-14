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

import static org.junit.Assert.*;

import org.junit.Test;

public class ShrinkingLargeProgessMonitorTest {
	private static class TestProgressMonitor implements ProgressMonitor {
		public TestProgressMonitor(int... expected) {
			this.expectedWorkUnits = expected;
		}
		private int index;
		private int[] expectedWorkUnits;
		@Override
		public void subTask(String message) {
			// Does nothing
		}

		@Override
		public void startTask(int totalUnits, String task) {
			// Does nothing
		}

		@Override
		public void worked(int workUnits) throws IllegalArgumentException {
			assertEquals("worked " + index, expectedWorkUnits[index], workUnits);
			index++;
		}

		public void assertEvent(int expectedIndex) {
			assertEquals("worked call count", expectedIndex, index);
		}

		public void reset() {
			index = 0;
		}
	}

	@Test
	public void testStartTask() {
		ShrinkingLargeProgessMonitor pm = new ShrinkingLargeProgessMonitor(new NullProgressMonitor());
		pm.startTask(0x7FFFFFFFL, "");
		assertEquals("Shift", pm.shift, 0);
		pm.startTask(0x80000000L, "");
		assertEquals("Shift", pm.shift, 1);
		pm.startTask(0x100000000L, "");
		assertEquals("Shift", pm.shift, 2);
		pm.startTask(0x200000000L, "");
		assertEquals("Shift", pm.shift, 3);
	}

	@Test
	public void testWorked() {
		TestProgressMonitor test = new TestProgressMonitor(1, 2, 3, 4);
		ShrinkingLargeProgessMonitor pm = new ShrinkingLargeProgessMonitor(test);
		pm.startTask(100, "");
		pm.worked(1);
		test.assertEvent(1);
		pm.worked(2);
		test.assertEvent(2);
		pm.worked(3);
		test.assertEvent(3);
		pm.worked(4);
		test.assertEvent(4);
		test.reset();
		pm.startTask(0x100000000L, "");
		assertEquals("Shift", pm.shift, 2);
		pm.worked(1);
		test.assertEvent(0);
		pm.worked(3);
		test.assertEvent(1);
		pm.worked(8);
		test.assertEvent(2);
		pm.worked(12);
		test.assertEvent(3);
		pm.worked(16);
		test.assertEvent(4);
	}
}
