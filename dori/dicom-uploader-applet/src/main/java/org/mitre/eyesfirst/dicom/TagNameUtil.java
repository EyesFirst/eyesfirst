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
package org.mitre.eyesfirst.dicom;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import org.dcm4che2.data.Tag;

/**
 * Converts a given tag to a human readable name.
 * FIXME: This is taken from the DORI-Web project and should use the same code.
 * @author dpotter
 */
public class TagNameUtil {
	private final static Map<Integer, String> names = new HashMap<Integer, String>();
	static {
		// Load the names off the Tag class.
		Field[] fields = Tag.class.getFields();
		for (int i = 0; i < fields.length; i++) {
			int m = fields[i].getModifiers();
			if (Modifier.isStatic(m) && Modifier.isPublic(m)) {
				if (fields[i].getType().equals(Integer.TYPE)) {
					// Use this
					try {
						names.put(fields[i].getInt(null), fields[i].getName());
					} catch (Exception e) {
						// Is this actually possible? Whatever.
						System.err.println("Exception loading tag names:");
						e.printStackTrace();
					}
				}
			}
		}
	}

	public static String toName(int tag) {
		String res = names.get(tag);
		return res == null ? TagNameUtil.convertTagToHexString(tag) : res;
	}

	private static final char[] PADDING = { '0', '0', '0', '0' };

	/**
	 * Converts a given tag into a hex string, split by a colon. For example,
	 * 0,0 becomes "0000:0000" and 15,100 becomes "000F:0064".
	 * @param tag
	 * @return
	 */
	public static String convertTagToHexString(int tag) {
		StringBuilder res = new StringBuilder(9);
		String s = Integer.toHexString((tag >> 16) & 0xFFFF);
		int l = s.length();
		if (l < 4)
			res.append(PADDING, 0, 4-l);
		res.append(s);
		res.append(':');
		s = Integer.toHexString(tag & 0xFFFF);
		l = s.length();
		if (l < 4)
			res.append(PADDING, 0, 4-l);
		res.append(s);
		return res.toString();
	}
}
