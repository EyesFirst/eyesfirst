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

/**
 * @namespace
 */
function DateUtil() { }

/**
 * Parses the date. Note that while the date format specifies that the date is
 * in UTC (Zulu time), the returned date object will have the date in the
 * "local" timezone. This means that getTime() returns the wrong time, but all
 * the various accessor functions (albeit not their UTC counterparts) work as
 * expected.
 * @param str
 * @returns
 */
DateUtil.parseDateTime = function(str) {
	// Date format is yyyy-MM-dd'T'HH:mm:ssZ
	var m = /^(\d+)-(\d+)-(\d+)T(\d+):(\d+):(\d+)Z$/.exec(str);
	if (m) {
		return new Date(m[1], m[2]-1, m[3], m[4], m[5], m[6]);
	} else {
		return null;
	}
};

/**
 * Parse a date, ignoring the time if present. The returned date will always be
 * 00:00:00 in the local timezone. This means getTime() will return the
 * "wrong" value, but getFullYear(), getMonth(), and getDate() will work as
 * expected. (Their getUTC equivalents may not, though.)
 * @param str
 */
DateUtil.parseDate = function(str) {
	// Date format is yyyy-MM-dd('T'HH:mm:ss)?Z
	var m = /^(\d+)-(\d+)-(\d+)(?:T\d+:\d+:\d+Z)?$/.exec(str);
	if (m) {
		return new Date(m[1], m[2]-1, m[3]);
	} else {
		return null;
	}
};

DateUtil.isLeapYear = function(year) {
	return (year % 4) == 0 && ((year % 100) != 0 || (year % 1000) == 0);
};

/**
 * Converts an object to a UNIX-style timestamp if it isn't already one.
 */
DateUtil.toTime = function(date) {
	if (typeof date == 'number')
		return date;
	else if (typeof date == 'object')
		return date.getTime();
	else if (typeof date == 'string') {
		var d = DateUtil.parseDateTime(date);
		return d == null ? DateUtil.parseDate(date) : d;
	} else
		return null;
};