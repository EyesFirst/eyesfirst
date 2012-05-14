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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;

import org.junit.Test;

public class EyesFirstProcessTest {
	private static class EyesFirstTestProcess extends EyesFirstProcess {
		private ByteArrayOutputStream stdout = new ByteArrayOutputStream();
		public EyesFirstTestProcess() {
			super("--run-simple-test");
			setExceptionOnProcessFailure(false);
		}

		@Override
		protected void runProcess() throws Exception {
			// Generally we do the same thing, but...
			super.runProcess();
			// ...after running, check stdout
			BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(stdout.toByteArray())));
			assertEquals("Result from child", "Working!", reader.readLine());
			assertEquals("Exit code from child", 42, getResult());
		}

		@Override
		protected void receiveStdOut(byte[] b, int length) {
			stdout.write(b, 0, length);
		}
	}

	@Test
	public void testRunProcess() throws Exception {
		new EyesFirstTestProcess().runProcess();
	}

}
