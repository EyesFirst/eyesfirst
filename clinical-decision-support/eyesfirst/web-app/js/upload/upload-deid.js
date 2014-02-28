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
 * The de-ID portion of the uploader.
 */

(function($) {
	/**
	 * @constructor
	 */
	function VerifyDeIDPage() {
		this._files = [];
		this._done = false;
		this._confirmed = false;
		this.setNext(new UploadStatusPage());
	}

	VerifyDeIDPage.prototype = new Page;

	VerifyDeIDPage.prototype.render = function() {
		var div = $('<div/>');
		div.append(this._taskMessage = $('<h1/>'));
		// FIXME: Status-checking is used to get the spinner. Should probably
		// be something else.
		div.append(this._statusMessage = $('<h2/>').addClass('status-checking'));
		div.append(this._subTaskMessage = $('<h3/>'));
		div.append(this._taskProgress = $('<div/>'));
		this._taskProgress.progressbar();
		this._taskProgress.hide();
		div.append(this._descriptionText = $('<div/>'));
		div.append(this._form = $('<form method="GET" action="" onsubmit="return false;"></form>'));
		div.append(this._fileTable = $('<table/>').addClass('deid-results'));
		div.append(this._confirmDialog =
			$('<div title="Confirm File Upload">' +
					'<p>I have reviewed this data and as the data owner I ' +
					'confirm that it does not contain any personally ' +
					'identifiable information. I am ready to upload ' +
					'this data to the server.</p></div>').hide());
		return div;
	};

	VerifyDeIDPage.prototype.showPage = function(uploader, container, forwards) {
		Page.prototype.showPage.call(this, uploader, container, forwards);
		if (forwards) {
			this._uploader = uploader;
			uploader.setWizardButtonsVisible(false);
			this._taskMessage.text("Processing ZIP files");
			this._statusMessage.show();
			this._subTaskMessage.hide();
			this._descriptionText.text('');
			var me = this;
			uploader.setStep("deid-run");
			var index = 1;
			var totalUnits = 0;
			var worked = 0;
			uploader.applet.processDeID({
				subTask: function(message) {
					me._subTaskMessage.text(message);
					console.log(message);
				},
				startTask: function(total, task) {
					me._statusMessage.text(task);
					console.log(task);
					worked = 0;
					totalUnits = total;
					if (totalUnits > 0) {
						me._taskProgress.progressbar('option', 'value', 0);
						$.favicon('percent', 0);
						me._taskProgress.show();
					} else {
						$.favicon('spinner', true);
						me._taskProgress.hide();
					}
				},
				worked: function(workUnits) {
					worked += workUnits;
					if (totalUnits > 0) {
						var percent = (worked / totalUnits) * 100;
						$.favicon('percent', percent);
						me._taskProgress.progressbar('option', 'value', percent);
					}
				},
				displayDicomFile: function(json, hasFundus) {
					me._addDicomFile('DICOM File ' + (index++), $.parseJSON(json));
				},
				done: function() {
					$.favicon('reset');
					me._statusMessage.hide();
					me._subTaskMessage.hide();
					me._taskProgress.hide();
					if (me._files.length == 0) {
						me._taskMessage.text("No Suitable DICOM Files Found");
						me._descriptionText.text('No suitable DICOM files were found in the files provided. Please try again with different files.');
						uploader.setWizardButtonsVisible(true, false);
						return;
					}
					me._taskMessage.text("Verify Anonymized Data");
					me._descriptionText.text('Please verify that the following metadata has had personally identifying information removed.');
					me._done = true;
					uploader.setWizardButtonsVisible(false, true);
					uploader.setNextText("Upload Anonymized Files");
					uploader.setStep("deid-verify");
				},
				error: function(error, message, trace) {
					$.favicon('reset');
					me._statusMessage.text("Processing failed.");
					var p = $('<p/>');
					errorBox(p, "Processing has failed. No data has been sent to the server at this point in time.", error, message, trace);
					me._statusMessage.after(p);
					// Failure makes everything OK :(
					window.onbeforeunload = null;
				}
			});
			// Once we're here, add a beforeunload event handler
			window.onbeforeunload = function(event) {
				var message = "The upload is only partially complete. If you leave now, it will not be completed. Are you sure you want to leave?";
				event.returnValue = message;
				return message;
			};
		}
	}

	VerifyDeIDPage.prototype.isNextAvailable = function() {
		if (this._done) {
			for (var i = 0; i < this._files.length; i++) {
				if (!this._files[i].verified)
					return false;
			}
			return true;
		} else {
			return false;
		}
	}

	VerifyDeIDPage.prototype._addDicomFile = function(name, json) {
		var row = $('<tr/>');
		this._fileTable.append(row);
		var nameCell = $('<td/>').text(name).addClass('dicom-deid-file');
		row.append(nameCell);
		var file = {
			name: name,
			nameCell: nameCell,
			metadata: json,
			verified: false
		}
		this._files.push(file);
		var cell = $('<td/>');
		row.append(cell);
		var button = $('<a href="#">Verify Data</a>');
		cell.append(button);
		button.click((function(me,file) {
			return function() {
				if (file.window && (!file.window.closed)) {
					file.window.focus();
				} else {
					file.window = window.open('', file.name, 'resizable=yes,scrollbars=yes');
					var doc = file.window.document;
					doc.writeln('<html><head><title>Verify Anonymized DICOM Metadata</title>');
					// Copy our stylesheets over
					$('link').each(function() {
						if (this.getAttribute('rel') == 'stylesheet') {
							// Clone that to the new page (as best as we can)
							doc.write('<link');
							for (var i = 0; i < this.attributes.length; i++) {
								doc.write(' ');
								doc.write(this.attributes[i].name);
								doc.write('="');
								doc.write(escapeHTML(this.attributes[i].value));
								doc.write('"');
							}
							doc.writeln('>');
						}
					});
					doc.writeln('</head><body>');
					doc.writeln('<h1>' + escapeHTML(name) + ' Metadata</h1><p>Please review the following data and confirm that it has been successfully de-identified.</p>');
					doc.writeln('<p id="metadata">Loading metadata...</p>');
					doc.writeln('<form method="GET" action="" onsubmit="return false;">');
					doc.write('<input type="checkbox" name="verified" id="verified"');
					if (file.verified) {
						// Not really sure why you'd ever want to de-verify, but whatever, go ahead and do it.
						doc.write(' checked="checked"');
					}
					doc.writeln('><label for="verified"> I have reviewed this data and as the data owner I confirm that it does not contain any personally identifiable information.</label>');
					doc.writeln('</form>');
					doc.writeln('</body></html>');
					doc.close();
					doc.getElementById('verified').onclick = function() {
						file.verified = this.checked;
						file.nameCell[file.verified ? 'addClass' : 'removeClass']('verified');
						if (file.verified) {
							// Close if it's been verified, otherwise, leave it up
							file.window.close();
						}
						me._uploader.updateWizardButtons();
					};
					// Insert the metadata HTML *last* as it's quite slow
					doc.getElementById('metadata').innerHTML = me.createMetaDataTable(file.metadata);
				}
				return false;
			}
		})(this, file));
	};

	/**
	 * Creates the HTML required to display the DICOM metadata.
	 */
	VerifyDeIDPage.prototype.createMetaDataTable = function(metadata) {
		var table = ['<table><tr><th>Tag</th><th>Value</th></tr>'];
		this._createMetaDataTable(metadata, table, '');
		table.push('</table>');
		return table.join('');
	};
	VerifyDeIDPage.prototype._createMetaDataTable = function(elements, table, depth) {
		var length = elements.length;
		for (var i = 0; i < length; i++) {
			var e = elements[i];
			table.push('<tr><td title="');
			table.push(escapeHTML(e.tag + ' (' + e.vr + ')'));
			table.push('">');
			if (depth) {
				table.push(depth);
				table.push(' ');
			}
			table.push(escapeHTML(e.tagName));
			table.push('</td><td>');
			if (e.value != null && typeof e.value == 'object') {
				table.push('</td></tr>');
				this._createMetaDataTable(e.value, table, depth + '\u00a0\u00a0\u00a0\u00a0');
			} else {
				table.push(escapeHTML(e.value));
				table.push('</td></tr>');
			}
		}
	};
	VerifyDeIDPage.prototype.onNext = function() {
		// This is about the only place where this functionality is even used -
		// confirm that the user REALLY wants to move along.
		if (this._confirmed) {
			return true;
		}
		var me = this;
		this._confirmDialog.dialog({
			resizable: false,
			modal: true,
			buttons: {
				"Upload Scans": function() {
					$(this).dialog("close");
					me._confirmed = true;
					me._uploader.next();
				},
				Cancel: function() {
					$(this).dialog("close");
				}
			}
		});
		return false;
	}

	/**
	 * Escape HTML entities. Why is this not a standard part of the JavaScript
	 * API by now?
	 */
	function escapeHTML(text) {
		if (text == null)
			return 'null';
		if (typeof text != 'string')
			text = text.toString();
		return text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
	}

	// Export:
	window['VerifyDeIDPage'] = VerifyDeIDPage;
})(jQuery);