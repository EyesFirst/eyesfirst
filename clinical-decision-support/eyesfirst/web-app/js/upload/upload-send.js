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
 * Final part of the uploader that sends the data to the server.
 */
(function($){
	/**
	 * @constructor
	 */
	function UploadStatusPage() {
		this.setNext(new CompletedPage());
	}

	UploadStatusPage.prototype = new Page;

	UploadStatusPage.prototype.render = function() {
		var div = $('<div/>');
		div.append('<h1>Uploading Files...</h1>');
		div.append(this._statusMessage = $('<h2/>'));
		div.append(this._progressBar = $('<div/>').progressbar());
		this._progressBar.hide();
		return div;
	};

	UploadStatusPage.prototype.showPage = function(uploader, container, forwards) {
		Page.prototype.showPage.call(this, uploader, container, forwards);
		if (forwards) {
			// Kill the back/next buttons again
			uploader.setWizardButtonsVisible(false);
			uploader.setStep("upload-uploading");
			var me = this;
			var worked = 0, total = 0;
			uploader.applet.upload({
				subTask: function(message) {
					me._statusMessage.text(message);
					console.log(message);
				},
				startTask: function(totalUnits, task) {
					if (totalUnits > 0) {
						total = totalUnits;
						worked = 0;
						$.favicon('percent', 0);
						me._progressBar.show();
					} else {
						// TODO: Make indeterminate
						$.favicon('spinner', true);
						me._progressBar.hide();
					}
					me._statusMessage.text(task);
					console.log(task);
				},
				worked: function(workUnits) {
					worked += workUnits;
					//console.log("Worked " + worked + "/" + total);
					if (total > 0) {
						var percent = (worked / total) * 100;
						me._progressBar.progressbar('option', 'value', percent);
						$.favicon('percent', percent);
					}
				},
				done: function() {
					uploader.replacePage(me.getNext());
					$.favicon('reset');
				},
				error: function(error, message, trace) {
					me._statusMessage.text("Upload failed.");
					var p = $('<p/>');
					errorBox(p, "Uploading has failed. Some data may have been sent to the server.", error, message, trace);
					me._statusMessage.after(p);
					// Failure makes everything OK :(
					window.onbeforeunload = null;
				}
			});
		}
	};

	UploadStatusPage.prototype.isNextAvailable = function() {
		return false;
	};

	/**
	 * @constructor
	 */
	function CompletedPage() {
	}

	CompletedPage.prototype = new Page;

	CompletedPage.prototype.render = function() {
		var div = $('<div/>');
		div.append("<h1>Upload Complete!</h1>");
		var p = $('<p/>');
		div.append(p);
		p.append($('<a href="../browser/">DORI Browser</a>').button());
		p.append($('<a href="">Upload Another</a>').button().css('margin-left', '1em'));
		return div;
	};

	CompletedPage.prototype.showPage = function(uploader, container, forwards) {
		// Remove the onbeforeunload event handler
		window.onbeforeunload = null;
		Page.prototype.showPage.call(this, uploader, container, forwards);
		uploader.setStep("upload-complete");
	}

	window['UploadStatusPage'] = UploadStatusPage;
})(jQuery);