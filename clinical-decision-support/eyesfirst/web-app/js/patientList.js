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
 * Main JavaScript file for the patient list.
 */

/**
 * @constructor
 * Creates the patient list UI.
 */
function PatientList(eyesfirst) {
	this.ui = $('<div/>').attr('id', 'patient-list');
	this.header = $('<header/>');
	this.header.append("<h1>Active Patients</h1>");
	this.ui.append(this.header);
	this._createJumpList();
	this.contentTable = $('<table><thead><tr><th></th><th>Name</th><th>Total</th><th>Last visit</th></tr></thead><tbody></tbody></table>');
	this.ui.append(this.contentTable);
	this.loading = $('<div class="loading">Loading...</div>');
	this.ui.append(this.loading);
	this.eyesfirst = eyesfirst;
}

PatientList.prototype = {
	_createJumpList: function() {
		this.jumpListElements = {};
		this.jumpListList = $('<ol id="patient-list-alphabet"></ol>');
		for (var c = 0; c < 26; c++) {
			var letter = String.fromCharCode(c+65);
			var li = $('<li/>').addClass("disabled").text(letter);
			this.jumpListList.append(li);
			this.jumpListElements[letter] = li;
		}
		this.header.append(this.jumpListList);
	},
	getUI: function() {
		return this.ui;
	},
	getHash: function() {
		return "";
	},
	onshow: function() {
		this.loadPatients();
	},
	loadPatients: function() {
		var me = this;
		this.loading.show();
		$.ajax({
			url: "patient/list",
			// FIXME: Since we don't actually do pagination right now, set the
			// page size to be very large. Clearly the right solution is to
			// actually do pagination.
			data: { "ps": 1000 },
			dataType: 'json',
			complete: function() {
				me.loading.hide();
			},
			error: function(jqXHR, textStatus, errorThrown) {
				// FIXME: Do something more intelligent than this
				alert("Error loading list: " + textStatus + "\n" + errorThrown);
			},
			success: function(data, textStatus, jqXHR) {
				// Build the display now that we have everything.
				me.showPatients(data);
			},
			//timeout: 60, // timeout
			type: 'GET'
		});
	},
	showPatients: function(patients) {
		var tbody = this.contentTable.find('tbody');
		tbody.empty();
		// Patients should be an array
		for (var i = 0; i < patients.length; i++) {
			var row = $('<tr/>');
			// FIXME: Add jump headers as appropriate
			row.append('<th/>');
			var patient = patients[i];
			var cell = $('<td/>');
			var link = $('<a/>').attr('href', '#patient=' + patient['id']);
			link.click((function(patient, eyesfirst) {
				return function() {
					// Switch to the most recent artifact for that patient
					eyesfirst.showPatient(patient['id']);
					// Go ahead, "navigate" to there
					return true;
				}
			})(patient, this.eyesfirst));
			link.text(patient['firstName'] ? patient['lastName'] + ", " + patient['firstName'] : patient['lastName']);
			cell.append(link);
			row.append(cell);
			row.append($('<td/>').text(patient['artifactCount']));
			var lastVisit = "";
			if (patient['lastVisit']) {
				lastVisit = patient['lastVisit'];
			}
			row.append($('<td/>').text(lastVisit));
			tbody.append(row);
		}
	}
}