(function($) {
	var UNSAVED_PREFIX = "__unsaved_";
	function makeConfirmDialog(message, options) {
		var dialog = $('<div/>');
		if ('title' in options) {
			dialog.attr('title', options['title']);
		}
		dialog.append(message);
		var buttons = [ {}, {} ];
		var fireNoOnClose = true;
		var onyes = options['yes'];
		var onno = options['no'];
		function nop() { }
		if (typeof onyes != 'function') {
			onyes = nop;
		}
		if (typeof onno != 'function') {
			onno = nop;
		}
		buttons[0]['text'] = 'yesText' in options ? options['yesText'] : 'Yes';
		buttons[0]['click'] = function() {
			fireNoOnClose = false;
			onyes();
			$(this).dialog("close");
		};
		buttons[1]['text'] = 'noText' in options ? options['noText'] : 'No';
		buttons[1]['click'] = function() {
			fireNoOnClose = false;
			onno();
			$(this).dialog("close");
		};
		dialog.dialog({
			'modal': true,
			'buttons': buttons,
			'zIndex': 2000,
			'close': function() {
				if (fireNoOnClose) {
					onno();
				}
			}
		});
	};
	function Annotation(x, y, slice, width, height, depth) {
		if (arguments.length == 1) {
			var json = x;
			this.id = json['id'];
			this.x = json['x'];
			this.y = json['y'];
			this.slice = json['slice'];
			this.width = json['width'];
			this.height = json['height'];
			this.depth = json['depth'];
			this.annotation = json['annotation'];
		} else if (arguments.length == 6) {
			this.x = Math.floor(x+0.5);
			this.y = Math.floor(y+0.5);
			this.slice = Math.floor(slice+0.5);
			this.width = Math.floor(width+0.5);
			this.height = Math.floor(height+0.5);
			this.depth = Math.floor(depth+0.5);
			console.log(this);
		} else if (arguments.length == 0) {
			// Leave defaults
		} else {
			throw new Error("Bad number of arguments for new annotation");
		}
	}
	Annotation.prototype = {
		x: 0,
		y: 0,
		slice: 0,
		width: 10,
		height: 10,
		depth: 5,
		annotation: "",
		getBoundingBox: function() {
			return new Rectangle3D(this.x, this.y, this.slice, this.width, this.height, this.depth);
		},
		isInLayer: function(slice) {
			return this.slice <= slice && slice < this.slice + this.depth;
		},
		isUnsaved: function() {
			return typeof this.id == 'string' && this.id.substr(0, UNSAVED_PREFIX.length) == UNSAVED_PREFIX;
		},
		toString: function() {
			return "Annotation[id=" + this.id + ",x=" + this.x + ",y=" +
					this.y + ",slice=" + this.slice +",width=" + this.width +
					",height=" + this.height + ",depth=" + this.depth +
					",annotation=" + this.annotation + "]";
		}
	};
	/**
	 * @constructor
	 */
	function AnnotationsManager(studyUID, seriesUID, objectUID) {
		this.studyUID = studyUID;
		this.seriesUID = seriesUID;
		this.objectUID = objectUID;
		this.annotations = {};
		this._id = 0;
	}
	AnnotationsManager.prototype = {
		getBaseURL: function() {
			return 'slices/' + encodeURIComponent(this.studyUID) + '/' + encodeURIComponent(this.seriesUID) + '/' + encodeURIComponent(this.objectUID) + '/annotations';
		},
		loadAnnotations: function(callback, errorCallback) {
			var me = this;
			$.ajax({
				url: this.getBaseURL(),
				dataType: 'json',
				error: function(jqXHR, textStatus, errorThrown) {
					if (jqXHR.status == 404) {
						// This error is nonsensical, as the image should
						// exist prior to annotations being loaded, but whatever.
						errorCallback("Unable to load annotations: The image containing the annotations could not be found.");
					} else {
						errorCallback("Unable to load annotations. Server returned error: " + jqXHR.status + " " + jqXHR.statusText);
					}
				},
				success: function(data, textStatus, jqXHR) {
					if ('annotations' in data) {
						var annotations = data['annotations'];
						var asList = [];
						for (var i = 0; i < annotations.length; i++) {
							var annotation = new Annotation(annotations[i]);
							me.annotations[annotation.id] = annotation;
							asList.push(annotation);
						}
						callback(asList);
					}
				},
				//timeout: 60, // timeout
				type: 'GET'
			});
		},
		createAnnotation: function(x, y, slice, width, height, depth) {
			var a = new Annotation(x, y, slice, width, height, depth);
			a.id = UNSAVED_PREFIX + (this._id++);
			this.annotations[a.id] = a;
			return a;
		},
		saveAnnotation: function(annotation, callback, errorCallback) {
			var url = this.getBaseURL();
			if (annotation.isUnsaved()) {
				url = url + "/add";
			} else {
				url = url + "/edit/" + encodeURIComponent(annotation.id);
			}
			var form = {
				'x': annotation.x,
				'y': annotation.y,
				'slice': annotation.slice,
				'width': annotation.width,
				'height': annotation.height,
				'depth': annotation.depth,
				'annotation': annotation.annotation
			};
			$.ajax({
				'url': url,
				'data': form,
				'dataType': 'json',
				'error': function(jqXHR, textStatus, errorThrown) {
					if (errorCallback) {
						errorCallback(textStatus, errorThrown);
					}
				},
				'success': function(data, textStatus, jqXHR) {
					if ('result' in data && data['result']['success']) {
						// Update the annotation
						var updatedAnnotation = data['annotation'];
						if (updatedAnnotation) {
							annotation.id = updatedAnnotation['id'];
							annotation.x = updatedAnnotation['x'];
							annotation.y = updatedAnnotation['y'];
							annotation.slice = updatedAnnotation['slice'];
							annotation.width = updatedAnnotation['width'];
							annotation.height = updatedAnnotation['height'];
							annotation.depth = updatedAnnotation['depth'];
							annotation.annotation = updatedAnnotation['annotation'];
						}
						if (callback) {
							callback(annotation);
						}
					}
					console.log(data);
				},
				'type': 'POST'
			});
		},
		removeAnnotation: function(annotation, callback, errorCallback) {
			if (annotation.isUnsaved()) {
				// Can't delete something that was never there
				if (errorCallback) {
					errorCallback("Annotation was never saved.");
				}
				return;
			}
			$.ajax({
				'url': this.getBaseURL() + "/remove/" + encodeURIComponent(annotation.id),
				'dataType': 'json',
				'error': function(jqXHR, textStatus, errorThrown) {
					errorCallback(textStatus, errorThrown);
				},
				'success': function(data, textStatus, jqXHR) {
					if ('result' in data && data['result']['success']) {
						if (callback) {
							callback(annotation);
						}
					}
					console.log(data);
				},
				'type': 'POST'
			});
		}
	};
	/**
	 * @constructor
	 */
	function AnnotationsUI(toolbar, manager, sliceManager) {
		var me = this;
		this.annotationManager = manager;
		this.sliceManager = sliceManager;
		manager.loadAnnotations(function(annotations) {
			toolbar.html('<input type="checkbox" id="annotationAddButton" name="addingAnnotation"><label for="annotationAddButton"> Add Annotation</label>')
			var addButton = toolbar.find("#annotationAddButton");
			addButton.button();
			addButton.click(function() {
				// Unselect anything currently selected
				sliceManager.selectHardExudate(null);
				sliceManager.setAddingAnnotation(addButton.prop('checked'));
			});
			me.addButton = addButton;
			sliceManager.setAnnotationManager(me);
			sliceManager.setAnnotations(annotations);
			me.annotations = annotations;
		}, function(err, ex) {
			addButton.text(err);
			console.log(ex);
		});
	}
	AnnotationsUI.prototype = {
		/**
		 * Add an annotation at the given area.
		 */
		addAnnotation: function(x, y, slice, width, height, depth, pagePosition) {
			this.sliceManager.setAddingAnnotation(false);
			this.addButton.prop('checked', false).button("refresh");
			//console.log("Create annotation (" + x + "," + y + "," + slice + "), [" + width + "x" + height + "x" + depth + "]");
			var annotation = this.annotationManager.createAnnotation(x, y, slice, width, height, depth);
			// Create our UI for this annotation
			this.showAnnotation(annotation, pagePosition.left, pagePosition.top, pagePosition.width, pagePosition.height);
		},
		/**
		 * Show an annotation.
		 * @param annotation the annotation to show
		 * @param x the page x coordinate the annotation is currently shown at
		 * @param y the page y coordinate the annotation is currently shown at
		 * @param width the page width that the annotation is displayed
		 * @param height the page height that the annotation is displayed
		 */
		showAnnotation: function(annotation, x, y, width, height) {
			var dialog = $('<div class="annotation annotation-editable" title="Annotation"><textarea></textarea></div>');
			var textarea = dialog.find("textarea");
			textarea.text(annotation.annotation);
			var changed = false;
			textarea.on("change", function() { changed = true; });
			var me = this;
			function onfailure(title, message, ex) {
				console.log(message);
				console.log(ex);
				$('<div/>').attr('title', title).text(message).dialog({
					"modal": true,
					"buttons": {
						"OK": function() { $(this).dialog("close"); }
					}
				});
			}
			function save(event) {
				annotation.annotation = textarea.prop('value');
				changed = false;
				textarea.prop('disabled', true);
				$(event.target).text("Saving...").prop('disabled', true).button('refresh');
				me.annotationManager.saveAnnotation(annotation, function() {
					me.annotations.push(annotation);
					me.sliceManager.redraw();
					dialog.dialog("close");
				}, function(message, ex) {
					var m = "An error occurred while saving your annotation.";
					if (message) {
						m = " " + message;
					}
					// Restore the dialog...
					dialog.dialog("show");
					// ...and then show an error message.
					onfailure("Failed Saving Annotation", m, ex);
				});
			};
			function deleteAnnotation(event) {
				// Ask before deleting!
				makeConfirmDialog("Are you sure you want to delete this annotation? Once deleted, it cannot be recovered.", {
					'title': "Delete annotation?",
					'yes': function() {
						// Now that they've confirmed, kill the dialog.
						changed = false;
						dialog.dialog("close");
						me.annotationManager.removeAnnotation(annotation, function() {
							// Now for the fun part, where we remove it from our
							// list.
							for (var i = 0; i < me.annotations.length; i++) {
								if (me.annotations[i] == annotation) {
									me.annotations.splice(i, 1);
									break;
								}
							}
							me.sliceManager.redraw();
						}, function(message, ex) {
							var m = "An error occurred while deleting your annotation.";
							if (message) {
								m = " " + message;
							}
							onfailure("Failed Deleting Annotation", m, ex);
						});
					},
					'yesText': 'Delete',
					'noText': 'Cancel'
				});
			};
			var buttons = { "Save": save };
			if (!annotation.isUnsaved()) {
				buttons["Delete"] = deleteAnnotation;
			}
			// dialog wants x/y to be viewport relative, but they're page relative, so fix that
			x = x - $(window).scrollLeft();
			y = y - $(window).scrollTop();
			dialog.dialog({
				"buttons": buttons,
				"position": [x, y + height],
				"beforeClose": function() {
					// Force the text area to blur so change events happen
					textarea.blur();
					if (changed) {
						// Ask before closing!
						makeConfirmDialog("Are you sure you want to close this annotation without saving your changes?", {
							'title': "Discard changes?",
							'yes': function() {
								changed = false;
								dialog.dialog("close");
							},
							'yesText': 'Close without saving',
							'noText': 'Cancel'
						});
						return false;
					} else {
						return true;
					}
				},
				"close": function() {
					dialog.dialog("destroy");
				}
			});
		}
	};
	// Exports
	window['Annotation'] = Annotation;
	window['Annotation'].prototype['isUnsaved'] = Annotation.prototype.isUnsaved;
	window['AnnotationsManager'] = AnnotationsManager;
	window['AnnotationsUI'] = AnnotationsUI;
})(jQuery);