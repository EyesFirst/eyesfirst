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
/*
 * Provides scripts for dealing with the EyesFirst processes.
 */

(function($) {
	/**
	 * @constructor
	 */
	function ProcessManager(container) {
		this._container = $(container);
		this._processTable = $('<table class="table table-bordered process-table"><tr><th class="process-id">ID</th><th class="process-name">Name</th><th class="process-status">Status</th><th class="process-text">Status Text</th></table>');
		this._container.append(this._processTable);
		this._processes = {};
		this._start();
	}
	ProcessManager.STATUS_ICONS = {
		"WAITING": "icon-time",
		"RUNNING": "icon-play",
		"FAILED": "icon-remove",
		"COMPLETED": "icon-ok"
	};
	ProcessManager.prototype = {
		/**
		 * Check interval in milliseconds
		 */
		_interval: 5000,
		/**
		 * The process request, if any.
		 */
		_processRequest: null,
		/**
		 * Current generation (used to remove dead processes).
		 */
		_generation: 0,
		/**
		 * Checks for processes immediately (unless already checking in the
		 * background).
		 */
		checkProcesses: function(callback) {
			if (this._processRequest) {
				return;
			}
			var me = this;
			this._processRequest = $.ajax({
				url: '../process',
				dataType: 'json',
				cache: false,
				complete: function(jqXHR, textStatus) {
					me._processRequest = null;
					callback(me);
				},
				error: function(jqXHR, textStatus, errorThrown) {
					me._showError();
				},
				success: function(data, textStatus, jqXHR) {
					me._hideError();
					me._addProcesses(data);
				}
			});
		},
		_showError: function() {
			if (this._serverError != null)
				return;
			this._serverError = $('<div class="alert hide fade in"><strong>Unable to load processes list from the server.</strong> This may be a temporary error (if the server is restarting or you have disconnected from the network.)</div>');
			this._processTable.before(this._serverError);
			this._serverError.alert();
			this._serverError.fadeIn();
		},
		_hideError: function() {
			if (this._serverError == null)
				return;
			this._serverError.alert("close");
			this._serverError = null;
		},
		/**
		 * Starts checking for new processes in the background.
		 */
		_start: function() {
			var me = this;
			this.checkProcesses(function() {
				setTimeout(function() {
					me._start();
				}, me._interval);
			});
		},
		_addProcesses: function(json) {
			this._generation++;
			if ('processes' in json) {
				var ps = json['processes'];
				for (var i = 0; i < ps.length; i++) {
					var p = ps[i];
					if ('id' in p) {
						var pr = this._getOrCreateProcess(p['id']);
						pr._update(p, this._generation);
					}
				}
				// Remove any dead processes (processes that weren't included
				// in that list).
				for (var id in this._processes) {
					var p = this._processes[id];
					if (p._generation != this._generation) {
						p._remove();
						delete this._processes[id];
					}
				}
			}
		},
		_getOrCreateProcess: function(id) {
			pid = 'pid_' + id;
			if (pid in this._processes) {
				return this._processes[pid];
			} else {
				var p = new Process(id);
				this._processes[pid] = p;
				// Now, add it to the HTML
				p._createUI(this._processTable);
				return p;
			}
		}
	};
	/**
	 * @constructor
	 */
	function Process(id) {
		this._id = id;
		this._name = "Process " + id;
	}
	Process.prototype = {
		_status: "Unknown",
		_statusString: "Unknown",
		_update: function(json, generation) {
			this._generation = generation;
			if ('name' in json) {
				this._name = json['name'];
			}
			if ('status' in json) {
				this._status = json['status'];
			}
			if ('statusString' in json) {
				this._statusString = json['statusString'];
			}
			this._idCell.text(this._id);
			this._nameCell.text(this._name);
			this._statusLabel.text(' ' + formatStatus(this._status));
			var icon = ProcessManager.STATUS_ICONS[this._status];
			if (icon == null)
				icon = "icon-question-sign";
			this._statusIcon.attr('class', 'process-status-icon ' + icon);
			this._statusTextCell.text(this._statusString);
		},
		/**
		 * Creates the UI, but does NOT populate it with values. (Use
		 * update() for that.)
		 * @param table
		 */
		_createUI: function(table) {
			var tr = $('<tr/>');
			this._row = tr;
			tr.append(this._idCell = $('<td/>').addClass('process-id'));
			tr.append(this._nameCell = $('<td/>').addClass('process-name'));
			tr.append(this._statusCell = $('<td/>').addClass('process-status'));
			this._statusCell.append(this._statusIcon = $('<i/>'));
			this._statusCell.append(this._statusLabel = $('<span/>'));
			tr.append(this._statusTextCell = $('<td/>').addClass('process-text').text(this._statusString));
			table.append(tr);
		},
		_remove: function() {
			this._row.remove();
		}
	};
	function formatStatus(status) {
		// At some point this may be loaded from a list, rather than just
		// attempting to title-case the text. Whatever.
		return status.substring(0,1) + status.substring(1).toLowerCase();
	}
	function showBootstrapAlert(title, message) {
		var dialog = $('<div class="modal hide fade"></div>');
		var head = $('<div class="modal-header"><button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button><h3></h3></div>');
		dialog.append(head);
		head.find("h3").text(title);
		var body = $('<div class="modal-body"></div>');
		if (typeof message == 'string' && message.charAt(0) != '<') {
			body.append($('<p/>').text(message));
		} else {
			body.append(message);
		}
		dialog.append(body);
		dialog.append($('<div class="modal-footer"><a href="#" class="btn btn-primary" data-dismiss="modal" aria-hidden="true">Close</a></div>'));
		$("body").append(dialog);
		dialog.modal({'show': true});
	}
	function sendProcessRequest(reallySend) {
		// Screw form.serialize(), since we have additional crap now
		data = {
			"studyUID": $("#studyUID").prop('value'),
			"seriesUID": $("#seriesUID").prop('value'),
			"objectUID": $("#objectUID").prop('value')
		};
		var key = $("#key").prop('value');
		if (key) {
			data['key'] = key;
		}
		if (!reallySend) {
			data["query"] = "true";
		}
		// Submit the form using AJAX
		try {
			var submitButtons = $('#process-form input[type=submit]');
			submitButtons.prop('disable', true);
			$.ajax({
				url: $('#process-form').attr('action'),
				type: 'POST',
				data: data,
				dataType: 'json',
				complete: function(jqXHR, textStatus) {
					submitButtons.prop('disable', false);
				},
				error: function(jqXHR, textStatus, errorThrown) {
					var m = "Server responsed with an error: ";
					if (textStatus == null) {
						if (errorThrown == null)
							m += "no details given.";
						else
							m += errorThrown;
					} else {
						m = textStatus;
						if (errorThrown != null) {
							m += " - " + errorThrown;
						}
					}
					showBootstrapAlert("Error Starting Process", m);
				},
				success: function(data, textStatus, jqXHR) {
					if ("dataDirectory" in data) {
						var p = $("<p>The data for the requested image would be stored in </p>");
						p.append($("<code/>").text(data.dataDirectory));
						p.append(".");
						showBootstrapAlert("Data Directory", p);
					}
				}
			});
		} catch (ex) {
			alert("An error occurred sending the form: " + ex);
		}
		return false;
	}
	// Once ready, hook into stuff:
	$(function() {
		$("#fullURL").css("display", "block");
		new ProcessManager('#processes');
		$('#process-form').submit(function() { return false; });
		$("#processSubmitButton").click(function() { sendProcessRequest(true); });
		$("#processQueryButton").click(function() { sendProcessRequest(false); });
	});
})(jQuery);

function parseDicomURL(url) {
	// This is intended to be overly forgiving of "bad" URLs - to the point where
	// the generated process title (like
	// "EyesFirst: StudyUID = 1.2; SeriesUID = 1.2; ObjectUID = 1.2") should
	// work.
	var studyId = null, seriesId = null, objectId = null;
	var re = /([^\?;&=\s]+)\s*=\s*([^;&\s]*)/g;
	var m;
	while (m = re.exec(url)) {
		try {
			if (m[1].toLowerCase() == 'studyuid')
				studyId = decodeURIComponent(m[2]);
			if (m[1].toLowerCase() == 'seriesuid')
				seriesId = decodeURIComponent(m[2]);
			if (m[1].toLowerCase() == 'objectuid')
				objectId = decodeURIComponent(m[2]);
		} catch (e) {
			// Ignore, it's most likely a bad URI component
		}
	}
	if (studyId) {
		document.forms.process.studyUID.value = studyId;
	}
	if (seriesId) {
		document.forms.process.seriesUID.value = seriesId;
	}
	if (objectId) {
		document.forms.process.objectUID.value = objectId;
	}
}