/*
 * Provides scripts for dealing with the EyesFirst processes.
 */

(function($) {
	function ErrorBox() {
		// Creates an error box:
		var box = $('<div class="ui-widget">' +
			'<div class="ui-state-error ui-corner-all" style="padding: 0 .7em;">' +
				'<p>' +
					'<span class="ui-icon ui-icon-alert" style="float: left; margin-right: .3em;"></span>' +
				'</p></div></div>');
		var p = box.find('p');
		for (var i = 0; i < arguments.length; i++) {
			var a = arguments[i];
			if (typeof a != 'string') {
				a = a == null ? 'null' : a.toString();
			}
			p.append($('<span/>').text(a));
		}
		return box;
	}
	/**
	 * @constructor
	 */
	function ProcessManager(container) {
		this._container = $(container);
		this._serverError = ErrorBox("Unable to load process list from the server.");
		this._serverError.hide();
		this._container.append(this._serverError);
		this._processTable = $('<table class="process-table"><tr><th class="process-id">ID</th><th class="process-name">Name</th><th class="process-status">Status</th><th class="process-text">Status Text</th></table>');
		this._container.append(this._processTable);
		this._processes = {};
		this._start();
	}
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
					if (!me._serverError.is(':visible')) {
						me._serverError.show('drop', {direction:'up'});
					}
				},
				success: function(data, textStatus, jqXHR) {
					if (me._serverError.is(':visible')) {
						me._serverError.hide('drop', {direction:'up'});
					}
					me._addProcesses(data);
				}
			});
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
			this._statusLabel.text(formatStatus(this._status));
			this._statusIcon.attr('class', 'process-status-icon process-status-' + this._status.toLowerCase());
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
			this._statusCell.append(this._statusIcon = $('<div/>'));
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
	window['ProcessManager'] = ProcessManager;
})(jQuery);