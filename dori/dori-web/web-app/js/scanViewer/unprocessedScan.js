/**
 * @license Copyright 2012 The MITRE Corporation
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
 * JavaScript for displaying the UI for the unprocessed scan.
 */

/**
 * List of diagnoses to display in the unprocessed view
 */
DICOMViewer.UNPROCESSED_DIAGNOSES = {
	abnormalRetinalThickness: { name: 'Abnormal Thickness' },
	hardExudates: { name: 'Hard Exudates' },
	neovascularization: { name: 'Neovascularization' },
	microaneurysms: { name: 'Microaneurysms' }
};

// Technically, the following UI is also used as part of the processed scan
// view. But it makes it easier to have it here:

/**
 * @constructor
 */
function ClinicianReviewUI(container, cls, title, url, key, dicomURL, diagnoses, yesLabel, noLabel, unsureLabel) {
	this._url = url;
	this._key = key;
	this._dicomURL = dicomURL;
	this._diagnoses = diagnoses;
	this._yesLabel = yesLabel ? yesLabel : "Yes";
	this._noLabel = noLabel ? noLabel : "No";
	this._unsureLabel = unsureLabel ? unsureLabel : "Unsure";
	if ('notes' in this._diagnoses) {
		// Special configuration for the notes field
		this._notesKey = this._diagnoses['notes']['field'];
		delete this._diagnoses['notes'];
	}
	var div = $('<div/>').addClass(cls);
	container.append(div);
	var section = $('<div/>').addClass('clinician-review');
	div.append(section);
	section.append($('<h1/>').text(title));
	this._loading = $('<div class="loading-indicator"><div class="throbber"></div> Loading...</div>');
	section.append(this._loading);
	this._container = div;
	var me = this;
	var data = {};
	data[key] = dicomURL;
	$.ajax({
		url: url + '/show',
		data: data,
		dataType: 'json',
		error: function(jqXHR, textStatus, errorThrown) {
			// But load defaults anyway
			me._createUI();
			if (jqXHR.status == 404) {
				// 404 is OK, it just means nothing was set yet. Any other
				// response, flag as an error.
				me._loading.remove();
			} else {
				me._loading.attr('class', '');
				me._loading.text("Unable to load feedback response. Server said: " + jqXHR.status + " " + jqXHR.statusText);
			}
		},
		success: function(data, textStatus, jqXHR) {
			// Create our fields
			me._createUI();
			// Kill the loading indicator
			me._loading.remove();
			// And apply the answers, if any
			for (var k in data) {
				if (k in me._answers) {
					me._answers[k].setAnswer(data[k]);
				}
			}
			if (me._notesKey in data)
				me._notes.prop('value', data[me._notesKey]);
		},
		//timeout: 60, // timeout
		type: 'GET'
	});
}

ClinicianReviewUI.prototype = {
	_notesKey: "notes",
	_createUI: function() {
		var ul = $('<ul class="answer-list"></ul>');
		this._answers = {};
		for (var d in this._diagnoses) {
			var diag = this._diagnoses[d];
			var field = ('field' in diag ? diag['field'] : d);
			this._answers[field] = new ClinicianAnswer(ul, d, diag.name, this._yesLabel, this._noLabel, this._unsureLabel);
		}
		this._loading.after(ul);
		var section = $('<div class="clinician-notes"></section>');
		section.append('<h1>Notes:</h1>');
		this._notes = $('<textarea name="notes" rows="5"></textarea>');
		section.append(this._notes);
		this._container.append(section);
		var div = $('<div/>').addClass('clinician-review-buttons');
		this._container.append(div);
		this._save = $('<button>Save</button>').button();
		div.append(this._save);
		this._save.click((function(me){
			return function() { me.save(); };
		})(this));
	},
	save: function() {
		var data = {};
		data[this._key] = this._dicomURL;
		for (var k in this._answers) {
			var a = this._answers[k].getAnswer();
			if (a == null) {
				a = "null";
			}
			data[k] = a;
		}
		data[this._notesKey] = this._notes.prop('value');
		this.setDisabled(true);
		var me = this;
		$.ajax({
			url: this._url + '/save',
			data: data,
			dataType: 'json',
			error: function(jqXHR, textStatus, errorThrown) {
				me.setDisabled(false);
				alert("Failed to save responses: Server said: " + jqXHR.status + " " + jqXHR.statusText);
			},
			success: function(data, textStatus, jqXHR) {
				me.setDisabled(false);
			},
			//timeout: 60, // timeout
			type: 'POST'
		})
	},
	setDisabled: function(disabled) {
		this._container.find('input, textarea').each(function() {
			$(this).prop('disabled', disabled);
		});
		this._save.button('option', 'disabled', disabled);
	}
};

/**
 * A single clinician answer.
 * @constructor
 */
function ClinicianAnswer(container, id, name, yesLabel, noLabel, unsureLabel) {
	var li = $('<li/>');
	li.append($('<span class="label"></span>').text(name));
	this._radioYes = this._createRadio(li, id, 'yes', yesLabel, false);
	this._radioNo = this._createRadio(li, id, 'no', noLabel, false);
	this._radioUnsure = this._createRadio(li, id, 'unsure', unsureLabel, true);
	container.append(li);
}

ClinicianAnswer.prototype = {
	_createRadio: function(container, id, aid, label, checked) {
		var res = $('<span/>').addClass('answer-' + aid);
		var input = $('<input type="radio">').attr({
			name: id,
			id: id + '-' + aid,
			value: aid
		});
		input.prop('checked', checked);
		res.append(input);
		res.append($('<label/>').attr('for', id + '-' + aid).text(label));
		container.append(res);
		return input;
	},
	getAnswer: function() {
		if (this._radioYes.prop('checked'))
			return true;
		else if (this._radioNo.prop('checked'))
			return false;
		else
			return null;
	},
	setAnswer: function(answer) {
		if (answer === true) {
			this._radioYes.prop('checked', true);
		} else if (answer === false) {
			this._radioNo.prop('checked', true);
		} else {
			this._radioUnsure.prop('checked', true);
		}
	}
};