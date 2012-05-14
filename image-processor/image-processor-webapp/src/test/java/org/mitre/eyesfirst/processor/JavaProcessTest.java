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

import java.io.File;
import java.util.List;

import org.junit.Test;

public class JavaProcessTest {
	/**
	 * A base implementation of a {@code JavaProcess} as we're putting an
	 * abstract class under test.
	 * @author dpotter
	 */
	private static class JavaProcessTestImpl extends JavaProcess {
		public JavaProcessTestImpl() {
			super();
			setExceptionOnProcessFailure(false);
		}

		@Override
		protected String getClassName() {
			return getClass().getPackage().getName() + ".ChildJava";
		}

		public int getResult() {
			return super.getResult();
		}

		@Override
		protected void receiveStdOut(byte[] b, int length) {
			System.out.write(b, 0, length);
		}

		@Override
		protected void receiveStdErr(byte[] b, int length) {
			System.err.write(b, 0, length);
		}
	}

	@Test
	public void testParseClassPath() {
		List<File> res = JavaProcess.parseClassPath("foo" + File.pathSeparator + "bar" + File.pathSeparator + File.pathSeparator + "baz");
		assertEquals("Number of entries found", 3, res.size());
		assertEquals("Result[0]", "foo", res.get(0).getPath());
		assertEquals("Result[1]", "bar", res.get(1).getPath());
		assertEquals("Result[2]", "baz", res.get(2).getPath());
	}

	@Test
	public void testRunProcess() throws Exception {
		JavaProcessTestImpl test = new JavaProcessTestImpl();
		test.runProcess();
		assertEquals("Exit code", 42, test.getResult());
	}

}
