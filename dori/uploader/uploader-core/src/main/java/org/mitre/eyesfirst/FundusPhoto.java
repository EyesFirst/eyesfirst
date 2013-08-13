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

package org.mitre.eyesfirst;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import com.google.common.base.Joiner;
import com.google.common.io.Files;

public class FundusPhoto {

	private final String REGEX = "^(R|L)_[0-9]{2}-[0-9]{2}-[0-9]{4}\\.(jpg|JPG|png|PNG)";

	private File fundusFile;
	private ArrayList<String> associatedQueryStrings;
	private GregorianCalendar date;
	private String laterality;
	private String format;

	public FundusPhoto(File f) throws IOException {
		associatedQueryStrings = new ArrayList<String>();

		String filename = f.getName();
		if (!filename.matches(REGEX)) {
			throw new IllegalArgumentException("Filename '" + f.getName()
					+ "'does not conform to the defined format: " + REGEX);
		}

		String[] decomp = filename.split("_|\\.");
		laterality = decomp[0];
		String[] dateDecomp = decomp[1].split("-");
		date = new GregorianCalendar(Integer.parseInt(dateDecomp[2]),
				Integer.parseInt(dateDecomp[0]) - 1, // Months are 0-indexed
				Integer.parseInt(dateDecomp[1]));
		String rawFormat = decomp[2].toUpperCase();

		if (rawFormat.equals("JPG"))
			format = "image/jpeg";
		if (rawFormat.equals("PNG"))
			format = "image/png";

		fundusFile = new File(f.getParentFile(), f.hashCode() + "." + rawFormat);
		Files.move(f, fundusFile);
	}

	public String createKey() {
		Joiner joiner = Joiner.on(",");
		return joiner.join(associatedQueryStrings);
	}

	public File getFundusFile() {
		return fundusFile;
	}

	public void addQueryString(String s) {
		System.out.println("Associating " + fundusFile.getPath() + " with " + s);
		associatedQueryStrings.add(s);
	}

	public boolean isSameDay(Date otherDate) {
		//DateFormat format = DateFormat.getDateTimeInstance();
		//System.out.println("Comparing " + format.format(otherDate) + " with my date of " + format.format(date.getTime()));
		GregorianCalendar otherCalendar = new GregorianCalendar();
		otherCalendar.setTime(otherDate);
		//System.out.println("Comparing " + otherCalendar + " with my calendar of " + date);
		return date.get(Calendar.YEAR) == otherCalendar.get(Calendar.YEAR)
				&& date.get(Calendar.DAY_OF_YEAR) == otherCalendar
				.get(Calendar.DAY_OF_YEAR);
	}

	public boolean isSameLaterality(String laterality2) {
		System.out.println("Checking [" + laterality2 + "] against our laterality of [" + laterality + "]");
		return laterality.equals(laterality2);
	}

	public String getFormat() {
		return format;
	}

}
