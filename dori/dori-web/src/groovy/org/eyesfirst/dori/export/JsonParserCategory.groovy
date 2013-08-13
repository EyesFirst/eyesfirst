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

package org.eyesfirst.dori.export

import org.codehaus.jackson.JsonParser
import org.codehaus.jackson.JsonToken
import org.mitre.eyesfirst.dori.export.DoriImportException

class JsonParserCategory {
	static Boolean readNullableBooleanField(JsonParser parser) {
		JsonToken token = parser.getCurrentToken();
		if (token == JsonToken.VALUE_NULL)
			return null;
		if (token == JsonToken.VALUE_TRUE)
			return Boolean.TRUE;
		if (token == JsonToken.VALUE_FALSE)
			return Boolean.FALSE;
		throw new DoriImportException("Expected boolean value (or null)");
	}
	static String getNullableString(JsonParser parser) {
		return parser.getCurrentToken() == JsonToken.VALUE_NULL ? null : parser.getText();
	}
}
