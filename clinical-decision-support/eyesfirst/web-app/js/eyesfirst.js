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
 *
 * Master class for the main UI controller.
 */

/**
 * @constructor Creates a new EyesFirst UI.
 */
function EyesFirst(container) {
	// Create our two container divs, the "menu-column" and the "content-column".
	this.container = $(container);
	this.menuColumn = $('<div id="menu-column"></div>');
	// Add the icons.
	this._createIcons();
	this.contentColumn = $('<div id="content-column"></div>');
	this.container.append(this.menuColumn, this.contentColumn);
	// Create the patient list. We only need one instance of this.
	this.patientList = new PatientList(this);
	// Create the artifact viewer. Again, only one instance of this.
	this.artifactViewer = new ArtifactViewer(this);
	$(window).bind('hashchange', (function(me) {
		return function() {
			me.showHash(window.location.hash);
		}
	})(this));
}

EyesFirst.MENU_ICONS = {
	patientList: { name: "Patient List", ui: "patientList" },
	calendarView: { name: "Calendar View" },
	photos: { name: "Photos?" }, // FIXME: What is this for?
	artifactViewer: { name: "Artifact Viewer", ui: "artifactViewer" }
};

EyesFirst.prototype = {
	_createIcons: function() {
		var list = $("<ul/>");
		this.menuColumn.append(list);
		function doNothing() { return false; }
		for (var id in EyesFirst.MENU_ICONS) {
			var icon = EyesFirst.MENU_ICONS[id];
			var li = $('<li/>').addClass("icon" + id.substr(0,1).toUpperCase() + id.substr(1));
			var a = $('<a/>').attr('href', '#').text(icon['name']);
			li.append(a);
			if (icon.ui) {
				a.bind('click', (function(me, ui) {
					return function() {
						// FIXME: This will only work without compressing the JS
						if (ui in me) {
							me.showUI(me[ui]);
						}
						return false;
					};
				})(this, icon.ui));
			} else {
				a.bind('click', doNothing);
				li.addClass('disabled');
			}
			list.append(li);
		}
	},
	/**
	 * "Starts" the UI. Checks the location hash and displays the appropriate
	 * UI based on that.
	 */
	start: function() {
		this.showHash(window.location.hash);
	},
	showHash: function(hash) {
		if (hash.charAt(0) == '#') {
			hash = hash.substr(1);
		}
		// Changing the UI can cause the hash to change, and we want to ignore
		// this change, so ignore the hash if it's being changed to the
		// hash we ourselves changed it to.
		if (hash == this.ignoreHash)
			return;
		this.ignoreHash = hash;
		// Parse the string
		if (hash.length == 0) {
			// Immediately show the patient list
			this.showUI(this.patientList);
			return;
		}
		var qs = EyesFirst.parseQueryString(hash);
		if ('patient' in qs) {
			this.showPatient(qs['patient']);
		} else {
			this.showUI(this.patientList);
		}
	},
	showUI: function(ui) {
		if (ui.canShow && (!ui.canShow())) {
			// Refuse to show the UI if it can't be shown
			return;
		}
		if (this.activeUI && typeof this.activeUI.onhide == 'function') {
			this.activeUI.onhide(this);
		}
		this.activeUI = ui;
		this.contentColumn.empty();
		this.contentColumn.append(ui.getUI());
		if (typeof ui.onshow == 'function') {
			ui.onshow(this);
		}
		if (typeof ui.getHash == 'function') {
			// Tell the hash change listener NOT to pay attention to the
			// upcoming hash change event
			this.ignoreHash = ui.getHash();
			window.location.hash = this.ignoreHash;
		}
	},
	/**
	 * Changes the UI to display the given patient.
	 */
	showPatient: function(patientId) {
		this.artifactViewer.showPatient(patientId);
		this.showUI(this.artifactViewer);
	}
};

EyesFirst.parseQueryString = function(str) {
	var params = str.split(/[&;]/);
	var rv = {}, idx, k, v;
	for (var i = 0; i < params.length; i++) {
		idx = params[i].indexOf('=');
		if (idx >= 0) {
			k = params[i].substr(0, idx);
			v = params[i].substr(idx+1);
		} else {
			k = params[i];
			v = "";
		}
		k = decodeURIComponent(k);
		v = decodeURIComponent(v);
		if (k in rv) {
			if (typeof rv[k] == 'string') {
				rv[k] = [ rv[k], v ];
			} else {
				rv[k].push(v);
			}
		} else {
			rv[k] = v;
		}
	}
	return rv;
}