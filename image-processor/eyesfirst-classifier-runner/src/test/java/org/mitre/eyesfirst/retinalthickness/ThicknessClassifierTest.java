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
package org.mitre.eyesfirst.retinalthickness;

import static org.junit.Assert.*;

import org.dcm4che2.data.BasicDicomObject;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.VR;
import org.junit.Test;

public class ThicknessClassifierTest {

	/**
	 * Very basic test to make sure the URL is being generated properly.
	 */
	@Test
	public void testCreateDicomResultURI() {
		DicomObject testObject = new BasicDicomObject();
		testObject.putString(Tag.StudyInstanceUID, VR.UI, "1.2.826.0.1.3680043.2.139.3.6.33166195690.200803141441141090");
		testObject.putString(Tag.SeriesInstanceUID, VR.UI, "1.2.826.0.1.3680043.2.139.3.6.33166195690.200803141441141400");
		testObject.putString(Tag.SOPInstanceUID, VR.UI, "1.2.826.0.1.3680043.8.1302.20080314144114.1671730634998957899");
		assertEquals("URL",
				"studyUID=1.2.826.0.1.3680043.2.139.3.6.33166195690.200803141441141090&seriesUID=1.2.826.0.1.3680043.2.139.3.6.33166195690.200803141441141400&objectUID=1.2.826.0.1.3680043.8.1302.20080314144114.1671730634998957899",
				ThicknessClassifier.createDicomResultURI(testObject));
	}

}
