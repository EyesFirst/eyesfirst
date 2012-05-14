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
package org.mitre.eyesfirst.viewer.web;

import static org.junit.Assert.*;

import java.io.IOException;

import org.dcm4che2.data.DicomObject;
import org.junit.Test;
import org.mitre.eyesfirst.viewer.DicomTestUtil;
import org.mitre.eyesfirst.viewer.web.DicomJPEGConverter;

public class DicomJPEGConverterTest {

	/**
	 * Very basic test that just makes sure we get A result.
	 * @throws IOException
	 */
	@Test
	public void testConvertSliceToJPEG() throws IOException {
		// Grab the test DICOM image
		DicomObject obj;
		try {
			obj = DicomTestUtil.loadTestDicom();
		} catch (Exception e) {
			System.err.println("Unable to load test DICOM - skipping this test for now!");
			e.printStackTrace();
			return;
		}
		assertNotNull("Unable to convert DICOM", DicomJPEGConverter.convertSliceToJPEG(obj, 1));
	}

}
