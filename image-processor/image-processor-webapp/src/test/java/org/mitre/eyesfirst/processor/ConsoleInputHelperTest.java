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

import static org.junit.Assert.*;

import java.nio.charset.Charset;
import java.util.Arrays;

import org.junit.Test;

public class ConsoleInputHelperTest {

	@Test
	public void testReadLine() {
		// Purposely use a small buffer size.
		ConsoleInputHelper tested = new ConsoleInputHelper(Charset.forName("UTF8"), 256);
		runTest(tested, 48, 10);
		runTest(tested, 512, 4);
		runTest(tested, 638, 4);
	}

	@Test
	public void testReadVariousNewLineStyles() {
		ConsoleInputHelper tested = new ConsoleInputHelper(Charset.forName("UTF8"), 1024);
		byte[] testData = {
			65, 13, 66, 13, 10, 67, 10,
			65, 98, 99, 13, 68, 101, 102, 13, 10, 71, 104, 105, 10,
			13, 13, 10, 10, 65, 66, 67, 68, 69, 70
		};
		tested.write(testData, 0, testData.length);
		tested.finish();
		assertEquals("Line 1", "A", tested.readLine());
		assertEquals("Line 2", "B", tested.readLine());
		assertEquals("Line 3", "C", tested.readLine());
		assertEquals("Line 4", "Abc", tested.readLine());
		assertEquals("Line 5", "Def", tested.readLine());
		assertEquals("Line 6", "Ghi", tested.readLine());
		assertEquals("Line 7", "", tested.readLine());
		assertEquals("Line 8", "", tested.readLine());
		assertEquals("Line 9", "", tested.readLine());
		assertEquals("Line 10", "ABCDEF", tested.readLine());
	}

	@Test
	public void testReadSplitUTF8() {
		ConsoleInputHelper tested = new ConsoleInputHelper(Charset.forName("UTF8"), 1024);
		// \u263A is a smiley face, if you're curious.
		byte[] testData = new byte[] {
			(byte) 0xE2, (byte) 0x98, (byte) 0xBA, 0x0A
		};
		tested.write(testData, 0, 2);
		assertNull("After partial write", tested.readLine());
		tested.write(testData, 2, 2);
		assertEquals("Line 1", "\u263A", tested.readLine());
		tested.finish();
		assertNull("Final line not null", tested.readLine());
	}

	@Test
	public void testReadBrokenSplitUTF8() {
		ConsoleInputHelper tested = new ConsoleInputHelper(Charset.forName("UTF8"), 1024);
		// Same as above.
		byte[] testData = new byte[] {
			(byte) 0xE2, (byte) 0x98
		};
		tested.write(testData, 0, 2);
		tested.finish();
	}

	/**
	 * Internal method to run a test
	 * @param tested the helper under test
	 * @param lineLength the length of each test line
	 * @param numberLines the number of test lines to write
	 */
	private void runTest(ConsoleInputHelper tested, int lineLength, int numberLines) {
		// Create the test data:
		byte[] b = new byte[lineLength * numberLines];
		for (int line = 0; line < numberLines; line++) {
			int from = lineLength * line;
			int to = from + lineLength;
			Arrays.fill(b, from, to, (byte)(65 + line));
			b[lineLength*(line+1)-1] = '\n';
		}
		// And write it.
		tested.write(b);
		// Now, test it.
		char[] c = new char[lineLength-1];
		for (int i = 1; i <= numberLines; i++) {
			// Create the tested line.
			Arrays.fill(c, (char)('@' + i));
			String expected = new String(c);
			assertEquals("Line " + i, expected, tested.readLine());
		}
		assertNull(tested.readLine());
	}
}
