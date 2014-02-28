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
 * Creates a jQuery UI error box.
 * 
 * @param obj
 *            the object to place the error box within
 * @param message
 *            the error message to display (as HTML)
 * @param errorName
 *            if given, the name of the exception that occurred
 * @param errorMessage
 *            if given, the exception message
 * @param trace
 *            if given, the stack trace
 */
function errorBox(obj, message, errorName, errorMessage, trace) {
	var div = $('<div class="ui-widget"></div>');
	var inner = $('<div class="ui-state-error ui-corner-all" style="padding: 0 .7em;"></div>');
	div.append(inner);
	var p = $('<p><span class="ui-icon ui-icon-alert" style="float: left; margin-right: .3em;"></span>' + message + '</p>');
	inner.append(p);
	var m = null;
	if (errorName) {
		var m = errorName;
	}
	if (errorMessage) {
		if (m) {
			m = m + " - " + errorMessage;
		} else {
			m = errorMessage;
		}
	}
	if (m || trace) {
		var detailsButton = $('<a href="#">Details</a>');
		inner.append($('<p/>').append(detailsButton));
		var details = $('<div/>');
		if (m) {
			details.append($('<p/>').text(m));
		}
		if (trace) {
			details.append($('<pre/>').text(trace));
		}
		details.hide();
		inner.append(details);
		detailsButton.click(function() {
			if (details.is(':visible')) {
				details.hide();
				detailsButton.button('option', 'icons', {"secondary":"ui-icon-triangle-1-s"});
			} else {
				details.show();
				detailsButton.button('option', 'icons', {"secondary":"ui-icon-triangle-1-n"});
			}
			return false;
		});
		detailsButton.button({
			"icons": {
				"secondary": "ui-icon-triangle-1-s"
			}
		});
	}
	obj.append(div);
}