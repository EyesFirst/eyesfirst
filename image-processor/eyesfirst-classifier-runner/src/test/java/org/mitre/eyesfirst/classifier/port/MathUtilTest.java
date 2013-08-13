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
package org.mitre.eyesfirst.classifier.port;

import static org.junit.Assert.*;

import org.junit.Test;

public class MathUtilTest {

	@Test
	public void testRound() {
		assertEquals("round 0.5", 1L, MathUtil.round(0.5));
		assertEquals("round -0.5", -1L, MathUtil.round(-0.5));
		assertEquals("round 50.5", 51L, MathUtil.round(50.5));
		assertEquals("round -50.5", -51L, MathUtil.round(-50.5));
		assertEquals("round +Infinity", Long.MAX_VALUE, MathUtil.round(Double.POSITIVE_INFINITY));
		assertEquals("round -Infinity", Long.MIN_VALUE, MathUtil.round(Double.NEGATIVE_INFINITY));
		assertEquals("round NaN", 0L, MathUtil.round(Double.NaN));
	}

}
