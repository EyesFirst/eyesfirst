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
/*
 * The upload pages that deal with starting the upload (querying the user for
 * parameters).
 */

(function($){
	/** @define */
	var APPLET_VERSION_NAME = "uploader-applet-0.0.2-SNAPSHOT.jar";

	/**
	 * @constructor
	 */
	function HIPAAPage() {
		this._hipaa = $('#hipaa');
		this._hipaa.detach();
		this._hipaa.show();
		this.setContents(this._hipaa);
		this.setNext(new AppletPage());
	}

	HIPAAPage.prototype = new Page;

	HIPAAPage.prototype.showPage = function(uploader, container, forwards) {
		Page.prototype.showPage.call(this, uploader, container, forwards);
		uploader.setWizardButtonsVisible(false, true);
		uploader.setNextText("I Understand");
	};

	/**
	 * "Fake" page used to create the Java applet and verify everything is
	 * working.
	 * @constructor
	 */
	function AppletPage() {
		this.setNext(new ChooseEFIDPage());
	}

	AppletPage.prototype = new Page;

	AppletPage.prototype.render = function(uploader) {
		var div = $('<div/>');
		$('body').append(div);
		// Create a root URL.
		var root = location.pathname;
		var i = root.indexOf('/', 1);
		root = i > 0 ? root.substr(0, i) : root;
		// Create the object HTML
		var html = '<object tabindex="0" id="uploaderApplet" type="application/x-java-applet" height="300" width="300">' +
			'<param name="archive"   value="' + APPLET_VERSION_NAME + '" />' +
			'<param name="codebase"  value="' + root + '/upload/applet/" />' +
			'<param name="code"      value="org.mitre.eyesfirst.applet.UploaderApplet" />' +
			'<param name="MAYSCRIPT" value="true">' +
			'<param name="Cookie"    value="JSESSIONID=' + Uploader.SESSION_ID + '" />' +
			'<param name="hostname"  value="' + location.protocol + '//' + location.host + root + '" />' +
			'<param name="java_arguments" value="-Djnlp.packEnabled=true" />' +
			'</object>';
		// Appending the HTML will IMMEDIATLEY attempt to create the applet,
		// which is NOT what we want, so defer that a bit. This allows the UI
		// to display that it's loading.
		setTimeout((function(me){
			return function() {
				div.append(html);
				me._applet = uploader.applet = document.getElementById('uploaderApplet');
				me._checkReady(uploader);
			}
		})(this), 10);
		this._div = $('<div/>').html('<p>Please wait, initializing the ' +
				'uploader applet...</p><p>You may be asked to allow the ' +
				'applet privileged access. Please choose "Allow" or "Run" in ' +
				'order to enable upload access.</p>' +
				'<p>The applet requires privileged access in order to ' +
				'anonymize the DICOM files prior to uploading them to the ' +
				'server.</p>');
		return this._div;
	};

	AppletPage.prototype._checkReady = function(uploader) {
		this.__checkReady(uploader, 0);
	};

	AppletPage.prototype.__checkReady = function(uploader, attempts) {
		if (!this._applet) {
			throw Error("Applet not defined yet");
		}
		if (attempts > 20) {
			// 20 attempts have been made, just assume it's failed
			this._div.text("The applet has failed to initialize.");
			return;
		}
		try {
			// FIXME: There's supposed to be error handling code here if the
			// applet has failed to initialize (not found, for example). However,
			// Firefox and Safari appear to handle that on their own:
			// by crashing outright. In Safari, this just means the tab is dead.
			// In Firefox, you get to reload your browser. And your session.
			// Go Firefox.
			if (this._applet.isActive) {
				// Once we're here, "hide" the applet
				this._applet.width = 0;
				this._applet.height = 0;
				try {
					var me = this;
					this._applet.checkReady({success: function() {
						try {
							// Just move on to the next page
							uploader.setWizardButtonsVisible(true);
							uploader.replacePage(me.getNext());
						} catch (ex) {
							console.log(ex);
						}
					},
					error: function(message) {
						me._div.text("The uploader failed to initialize: " + message);
					}});
				} catch (ex) {
					console.log(ex);
				}
				return;
			}
		} catch (e) {
			// Ignore this for now, assume we're not ready
			console.log("Exception checking isActive");
			console.log(e);
		}
		setTimeout((function(me){
			return function() {
				me.__checkReady(uploader, attempts++);
			}
		})(this), 50);
	};

	AppletPage.prototype.showPage = function(uploader, container, forwards) {
		if (uploader.applet) {
			uploader.replacePage(this.getNext());
		} else {
			// Otherwise, handle as normal
			Page.prototype.showPage.call(this, uploader, container, forwards);
			// ...almost...
			uploader.setWizardButtonsVisible(false);
		}
	};

	AppletPage.prototype.isNextAvailable = function() {
		return false;
	};

	/**
	 * @constructor
	 */
	function ChooseEFIDPage() {
		var nextPage = new ChooseDicomFilesPage();
		this.setStep("choose-efid");
		this._newPage = new ChooseNewEFIDPage(nextPage);
		this._existingPage = new ChooseExistingEFIDPage(nextPage);
	}

	ChooseEFIDPage.prototype = new Page;

	ChooseEFIDPage.prototype.render = function() {
		var div = $('<div/>').addClass("choose-efid");
		div.append('<h1>Upload Retinal Images</h1>');
		div.append(this._newButton = $('<div class="new-patient button">For a New Patient</div>'));
		div.append(this._existingButton = $('<div class="existing-patient button">For an Existing Patient</div>'));
		this._newButton.button();
		this._existingButton.button();
		this._newButton.click((function(me){
			return function() {
				me._uploader.setWizardButtonsVisible(true);
				me._uploader.showPage(me._newPage);
			}
		})(this));
		this._existingButton.click((function(me){
			return function() {
				me._uploader.setWizardButtonsVisible(true);
				me._uploader.showPage(me._existingPage);
			}
		})(this));
		return div;
	};

	ChooseEFIDPage.prototype.showPage = function(uploader, container) {
		Page.prototype.showPage.call(this, uploader, container);
		uploader.setWizardButtonsVisible(false);
		this._uploader = uploader;
	};

	/**
	 * @constructor
	 */
	function ChooseNewEFIDPage(next) {
		this.setStep("choose-efid-new");
		this.setNext(next);
	}

	ChooseNewEFIDPage.prototype = new Page;

	ChooseNewEFIDPage.prototype.render = function(uploader) {
		if (!Uploader.EFID_ISSUER_URL) {
			return "EFID issuer URL was not properly set, cannot fetch an existing URL. (This is an internal configuration issue.)";
		}
		var div = $('<div/>');
		div.append('<h1>New Patient EFID</h1>');
		div.append(this._patientDiv = $('<div/>'));
		div.append(this._instructionsDiv = $("<p>Please write down this EFID for your records. This EFID will be used by the DORI system to correlate scans belonging to this patient.</p>"));
		this._instructionsDiv.css('visibility', 'hidden');
		return div;
	}

	ChooseNewEFIDPage.prototype.showPage = function(uploader, container, forwards) {
		Page.prototype.showPage.call(this, uploader, container, forwards);
		if (Uploader.EFID_ISSUER_URL) {
			if (forwards) {
				// Start the AJAX call
				var me = this;
				this._patientDiv.text("Fetching a new EFID...");
				this._instructionsDiv.css('visibility', 'hidden');
				$.ajax({
					url: Uploader.EFID_ISSUER_URL,
					dataType: 'text',
					error: function(jqXHR, textStatus, errorThrown) {
						if (errorThrown) {
							me._patientDiv.text("Unable to fetch a new EFID: " + errorThrown);
						} else {
							me._patientDiv.text("Unable to fetch a new EFID: Server returned " + textStatus);
						}
					},
					success: function(data, textStatus, jqXHR) {
						me._efid = data;
						me._patientDiv.text("EFID: " + me._efid);
						me._instructionsDiv.css('visibility', 'visible');
						uploader.applet.setEFID(me._efid);
						uploader.updateWizardButtons();
					},
					//timeout: 60, // timeout
					type: 'GET'
				});
			}
		}
	};

	ChooseNewEFIDPage.prototype.isNextAvailable = function() {
		return this._efid != null;
	}

	/**
	 * @constructor
	 */
	function ChooseExistingEFIDPage(next) {
		this._verified = false;
		this.setStep("choose-efid-existing");
		this.setNext(next);
	}

	ChooseExistingEFIDPage.prototype = new Page;

	ChooseExistingEFIDPage.prototype.render = function(uploader) {
		this._uploader = uploader;
		var div = $('<div/>');
		div.append('<h1>Existing EFID</h1><p>Please enter the existing EFID for the patient you are uploading scans for.</p>');
		div.append(this._form = $('<form method="GET" action="" onsubmit="return false;"></form>'));
		this._form.append($('<span/>').text('EFID '));
		this._form.append(this._efidField = $('<input type="text" size="10">'));
		this._form.append(this._status = $('<span/>'));
		var me = this;
		this._efidField.textchange(function() { me._verify(); }, 500,
				function() {
					me._verified = false;
					uploader.updateWizardButtons();
				}
		);
		return div;
	};

	ChooseExistingEFIDPage.prototype.isNextAvailable = function() {
		return this._verified;
	};

	ChooseExistingEFIDPage.prototype._verify = function() {
		if (this._query) {
			this._query.abort();
		}
		var me = this;
		if (this._efidField.prop('value') == '') {
			// fast-fail
			me._status.attr('class', 'efid-status status-failed');
			me._status.text('Please enter EFID');
			return;
		}
		var efid = this._efidField.prop('value');
		me._status.attr('class', 'efid-status status-checking');
		me._status.text("Checking...");
		this._query = $.ajax({
			url: Uploader.EFID_VERIFIER_URL,
			data: { id: efid },
			dataType: 'text',
			error: function(jqXHR, textStatus, errorThrown) {
				me._status.attr('class', 'efid-status status-failed');
				if (jqXHR.status == 404) {
					// This is an expected result, it means - well, not found.
					me._status.text("EFID not found");
				} else {
					if (errorThrown) {
						me._status.text("Unable to verify EFID: " + errorThrown);
					} else {
						me._status.text("Unable to verify EFID: Server returned " + textStatus);
					}
				}
			},
			success: function(data, textStatus, jqXHR) {
				me._verified = true;
				me._status.attr('class', 'efid-status status-ok');
				me._status.text("OK");
				me._uploader.applet.setEFID(efid);
				me._uploader.updateWizardButtons();
			},
			//timeout: 60, // timeout
			type: 'GET'
		});
	};

	/**
	 * Base class of the various "choose files" pages, since they're so similar.
	 * @constructor
	 */
	function ChooseFilesPage(data) {
		this._data = data;
		this._files = [];
	}

	ChooseFilesPage.prototype = new Page;

	ChooseFilesPage.prototype.isNextAvailable = function() {
		for (var i = 0; i < this._files.length; i++) {
			if (!this._files[i].valid)
				return false;
		}
		return true;
	};

	ChooseFilesPage.prototype.render = function(uploader) {
		var div = $('<div/>');
		if ('title' in this._data) {
			div.append($('<h1/>').text(this._data['title']));
		}
		if ('instructions' in this._data) {
			div.append($('<p/>').html(this._data['instructions']));
		}
		div.append(this._form = $('<form method="GET" action="" onsubmit="return false;"></form>'));
		var files = 'files' in this._data ? this._data['files'] : this._data;
		this._files = [];
		var table = $('<table/>');
		this._form.append(table);
		for (var i = 0; i < files.length; i++) {
			var row, cell, file, status;
			row = $('<tr/>').addClass('choose-file');
			table.append(row);
			file = files[i];
			row.append(cell = $('<td/>'));
			if ('label' in file) {
				cell.append($('<span/>').text(file['label'] + ' '));
			}
			var field, browse;
			row.append(cell = $('<td/>'));
			cell.append(field = $('<input type="text" size="60">'));
			row.append(cell = $('<td/>'));
			cell.append(browse = $('<input type="button" value="Browse...">'));
			row.append(cell = $('<td/>'));
			cell.append(status = $('<span/>'));
			var fObj = {
				field: field, browse: browse, valid: false,
				optional: 'optional' in file ? file['optional'] : false
			};
			if (fObj.optional)
				fObj.valid = true;
			var verify = (function(fObj, status, appletFunction) {
				return function(obj) {
					var value = $(obj).prop('value');
					fObj.valid = uploader.applet[appletFunction](value);
					if (value == '' && fObj.optional) {
						fObj.valid = true;
					}
					status.attr('class', fObj.valid ? (fObj.optional && value == '' ? '' : 'status-ok') : 'status-failed');
					uploader.updateWizardButtons();
				}
			})(fObj, status, file['applet']);
			field.textchange(verify);
			this._files.push(fObj);
			browse.click((function(me, field, verify) {
				return function() {
					uploader.applet.browseForFiles({
						accepted: function(file) {
							field.prop('value', file);
							verify(field.get(0));
						},
						canceled: function() {
							// ignore
						}
					});
				}
			})(this, field, verify));
		}
		return div;
	};

	ChooseFilesPage.prototype.showPage = function(uploader, container, forwards) {
		Page.prototype.showPage.call(this, uploader, container, forwards);
		uploader.setWizardButtonsVisible(true, true);
	};

	/**
	 * @constructor
	 */
	function ChooseDicomFilesPage() {
		ChooseFilesPage.call(this, {
			'title': 'Upload Files', 
			'instructions': '<p>This process requests zip files of the data to be uploaded for processing. Data should be uploaded for a single patient at a time, though multiple scans may be uploaded for an individual patient. At least one OCT scan is required to be uploaded. Fundus photos are viewed alongside the OCT scans and are not processed by the classifiers and thus are optional.</p><p>In order to export OCT scans and DICOM metadata from the Carl Zeiss Cirrus HD-OCT instrument, use the Research Browser to separately export a DICOM file (as a zip) and IMG file. Remember to do this for a single patient only, though multiple scans may be exported for an individual patient.</p>',
			'files': [
				{
					'label': 'DICOM Zip', 
					'applet': 'setDicomFile'
				},
				{ 
					'label': 'Imagery Zip', 
					'applet': 'setImageryFile'
				}
			]
		});
		this.setStep("choose-files-dicom");
		this.setNext(new ChooseFundusFilePage());
	}

	ChooseDicomFilesPage.prototype = new ChooseFilesPage;

	/**
	 * @constructor
	 */
	function ChooseFundusFilePage() {
		ChooseFilesPage.call(this, {
			'title': '(Optional) Choose Fundus Photo', 
			'instructions': '<p>You may optional choose a fundus photo of the ' +
				'eye to display along with the imagery files. If you do not ' +
				'include a fundus photo, no photo will be displayed when ' +
				'viewing scans through DORI.</p>' +
				'<p>Fundus photo filenames must be in the following format: ' +
				'<code>[L|R]_[MM]-[DD]-[YYYY].[PNG|JPG]</code></p>' +
				'<p>For example: <code>L_03-23-2010.JPG</code></p>' +
				'<p>Remember to do this for a single patient only, though ' +
				'multiple photos may be exported for an individual patient.</p>',
			'files': [
				{
					'label': 'Fundus Photo',
					'applet': 'setFundusFile',
					'optional': true
				}
			]
		});
		this.setStep("choose-files-fundus");
		this.setNext(new VerifyDeIDPage());
	}

	ChooseFundusFilePage.prototype = new ChooseFilesPage;

	// FIXME: This export shouldn't really be needed?
	// Exports:
	window['HIPAAPage'] = HIPAAPage;
})(jQuery);