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
 * Library for displaying DICOM based on the server.
 */

// Work around missing debug stuff in certain (IE) browsers (IE)
if (!console) { console = {}; }
if (!console.log) { console.log = function() { }; }

/*
 * COORDINATES NOTE:
 * For the purposes of the viewer:
 * x = x on the page (left/right) = DICOM columns
 * y = y on the page (top/down) = DICOM rows
 * z = DICOM layers
 * 
 * In the OCT scans (so far), this means x = fast time, y = axial, z = slow
 * time. I'm not sure if this will always be true.
 */

/**
 * Creates the DICOM viewer.
 * @constructor
 * @param container the containing element (as a jQuery selector) to add the
 * various DICOM elements
 * @param processed a boolean indicating whether or not the image was processed,
 * which determines which UI to display
 * @returns
 */
function DICOMViewer(container, processed) {
	this._container = $(container);
	this._showProcessedUI = processed;
	this._diagnoses = {};
}

DICOMViewer.setAppRoot = function(root) {
	DICOMViewer.APP_ROOT = root;
	var doriweb = root;
	DICOMViewer.PROCESSED_KEY = 'processedQueryString';
	DICOMViewer.UNPROCESSED_KEY = 'rawQueryString';
	DICOMViewer.PROCESSED_FUNDUS_URI = doriweb + '/upload/synthesizedFundusPhoto?processedQueryString=';
	DICOMViewer.FUNDUS_URI = doriweb + '/upload/fundusPhoto';
	DICOMViewer.PROCESSED_THICKNESS_MAP = doriweb + '/upload/thicknessMap?processedQueryString=';
	DICOMViewer.PROCESSED_DIAGNOSIS_URI = doriweb + '/upload/classifierDiagnoses?processedQueryString=';
	DICOMViewer.UNPROCESSED_DIAGNOSES_URI = doriweb + '/diagnosis';
	DICOMViewer.PROCESSED_DIAGNOSES_URI = doriweb + '/feedback';
	//*
	// For testing:
	//DICOMViewer.PROCESSED_FUNDUS_URI = 'canned_results/original_synthesized_fundus.png?q=';
	//DICOMViewer.UNPROCESSED_FUNDUS_URI = DICOMViewer.PROCESSED_FUNDUS_URI;
	//DICOMViewer.PROCESSED_THICKNESS_MAP = 'canned_results/original_thickness_map.png?q=';
	//DICOMViewer.PROCESSED_DIAGNOSIS_URI = 'data/original_results.json?q=';
	//*/
	// UI precache stuff
	// FIXME: Need to calculate this.
	new Image().src = root + "/plugins/jquery-ui-1.8.15/jquery-ui/themes/ui-lightness/images/ui-bg_gloss-wave_35_f6a828_500x100.png";
	new Image().src = root + "/images/large-spinner.gif";
};

/**
 * Loads an image and shrinks it to fit the available horizontal space,
 * preserving the aspect ratio.
 */
jQuery.fn.fitimage = function(url, options) {
	this.each(function() {
		var img = new Image();
		var elem = $(this);
		var onload = options['onload'];
		var aspectRatio = options['aspectRatio'];
		img.onload = function() {
			width = parseInt(img.width);
			height = parseInt(img.height);
			// Jam it in the available space
			var jqImage = $(img);
			var originalWidth = elem.width();
			console.log("fitimage: " + width + "x" + height + ", fit to " + originalWidth + ' with AR ' + aspectRatio);
			var height;
			if (typeof aspectRatio == 'number') {
				height = originalWidth / aspectRatio;
			} else if (typeof aspectRatio == 'function') {
				height = aspectRatio(originalWidth, width, height);
			} else {
				height = height * (originalWidth / width);
			}
			jqImage.css({
				width: originalWidth + 'px',
				height: height + 'px'
			});
			if (typeof onload == 'function')
				onload.call(img);
		};
		if ('onerror' in options) {
			var onerror = options['onerror'];
			img.onerror = function() {
				if (onerror == 'remove') {
					$(img).remove();
				} else if (typeof onerror == 'function') {
					onerror.call(img);
				}
			};
		}
		img.src = url;
		if (!('append' in options) || options['append']) {
			elem.append(img);
			if (options['hide']) {
				$(img).hide();
			}
		}
	});
};

DICOMViewer.prototype = {
	scanSpeed: 100,
	/**
	 * Currently visible slice.
	 */
	_slice: 0,
	/**
	 * Current brightness.
	 */
	_adjustBrightness: 0,
	_adjustConstrast: 0,
	_showProcessedUI: false,
	/**
	 * When set to true, slider updates are ignored (in other words,
	 * {@link #_showSlice()} and {@link #_fundusShowSlice()} do nothing).
	 */
	_ignoreSliderUpdates: false,
	/**
	 * Attempts to load scan information for the fragment. This is rarely used.
	 */
	loadFromFragment: function() {
		var qs = location.hash;
		if (qs.charAt(0) == '#')
			qs = qs.substring(1);
		return this._loadFromQS(qs);
	},
	/**
	 * Attempts to load scan information from the query string. This is the most
	 * common usecase.
	 */
	loadFromQuery: function() {
		var qs = location.search;
		if (qs.charAt(0) == '?')
			qs = qs.substring(1);
		return this._loadFromQS(qs);
	},
	/**
	 * First attempts to load a scan based on the scan information provided in
	 * the query string, then by the fragment.
	 */
	loadFromLocation: function() {
		// Try the query string first
		if (this.loadFromQuery()) {
			// Yay.
			return;
		}
		if (this.loadFromFragment()) {
			return;
		}
		// Otherwise, no info, display an error
		this._error("No object was requested.");
	},
	_loadFromQS: function(qs) {
		var studyId = null, seriesId = null, objectId = null, processed = false, colormap = null;
		var re = /([^;&=]+)(?:=([^;&]*))?/g;
		var m;
		while (m = re.exec(qs)) {
			if (m[2]) {
				if (m[1] == 'studyUID') {
					studyId = decodeURIComponent(m[2]);
				} else if (m[1] == 'seriesUID') {
					seriesId = decodeURIComponent(m[2]);
				} else if (m[1] == 'objectUID') {
					objectId = decodeURIComponent(m[2]);
				} else if (m[1] == 'colormap') {
					colormap = decodeURIComponent(m[2]);
				}
			}
			if (m[1] == 'processed') {
				processed = true;
			}
		}
		if (studyId != null && seriesId != null && objectId != null) {
			this._colormap = colormap;
			this.load(studyId, seriesId, objectId, processed);
			return true;
		}
		return false;
	},
	/**
	 * Display a given DICOM object.
	 * 
	 * @param studyId
	 *            the study UID of the object to show
	 * @param seriesId
	 *            the series UID of the object to show
	 * @param objectId
	 *            the object UID of the object to show
	 * @param processed
	 *            which UI to show, processed or unprocessed (if left blank, the
	 *            value at construction time is used)
	 */
	load: function(studyId, seriesId, objectId, processed) {
		this._destroyUI();
		this._studyId = studyId;
		this._seriesId = seriesId;
		this._objectId = objectId;
		if (arguments.length < 4)
			processed = this._showProcessedUI;
		this._url = 'slices/' + encodeURIComponent(studyId) + '/' + encodeURIComponent(seriesId) + '/' + encodeURIComponent(objectId);
		this._dicomURL = 'studyUID=' + encodeURIComponent(studyId) + '&seriesUID=' + encodeURIComponent(seriesId) + '&objectUID=' + encodeURIComponent(objectId);
		this._loading();
		this._annotationsManager = new AnnotationsManager(studyId, seriesId, objectId);
		var me = this;
		// Grab the metainfo
		console.log("Loading metadata...");
		// Disable the sliders during the load, they will be reenabled when the
		// slice manager is ready.
		this._ignoreSliderUpdates = true;
		$.ajax({
			url: this._url + '/info',
			dataType: 'json',
			error: function(jqXHR, textStatus, errorThrown) {
				if (jqXHR.status == 404) {
					me._error("Unable to load imagery: the requested object was not found.");
				} else {
					me._error("Unable to load imagery. Server said: " + jqXHR.status + " " + jqXHR.statusText);
				}
			},
			success: function(data, textStatus, jqXHR) {
				// Build the display now that we have everything.
				me._createUI(processed, data.efid);
				me._applyMetaData(data.metadata);
			},
			//timeout: 60, // timeout
			type: 'GET'
		});
	},
	/**
	 * Shows the slice at the given index.
	 * @param slice the slice to show
	 */
	showSlice: function(slice) {
		// Update the sliders
		this._ignoreSliderUpdates = true;
		this._dicomSlider.slider('option', 'value', slice);
		this._fundus.setValue(slice);
		this._ignoreSliderUpdates = false;
		// And then do the same thing the slider does
		this.__showSlice(slice, false);
	},
	animateSlices: function(animate) {
		if (arguments.length == 0) {
			// When called with no arguments, toggle
			animate = typeof this._slidingInterval != 'number';
		}
		if (animate) {
			var slice = 0, me = this;
			this.showSlice(0);
			this._slidingInterval = setInterval(function() {
				slice++;
				if (slice >= me._layerCount) {
					me.animateSlices(false);
				} else {
					me.showSlice(slice);
				}
			}, this.scanSpeed);
			this._animateSlicesButton.removeClass("ui-icon-play").addClass("ui-icon-stop");
		} else {
			if (typeof this._slidingInterval == 'number')
				clearInterval(this._slidingInterval);
			this._slidingInterval = null;
			this._animateSlicesButton.removeClass("ui-icon-stop").addClass("ui-icon-play");
		}
	},
	/**
	 * Callback from our slider, indicating that the slice has changed.
	 * @param slice
	 * @param sliding
	 */
	_showSlice: function(slice, sliding) {
		if (this._ignoreSliderUpdates)
			return;
		// Kill the animation if it's running
		this.animateSlices(false);
		if (slice == this._slice) {
			return;
		}
		this.__showSlice(slice, sliding);
		this._fundus.setValue(slice);
	},
	/**
	 * Callback from the fundus slider after sliding.
	 * @param slice the slice to change to
	 * @param sliding whether or not the fundus slider is still sliding
	 */
	_fundusShowSlice: function(slice, xMarker, sliding) {
		if (this._ignoreSliderUpdates)
			return;
		// Again, kill the animation if it's running
		this.animateSlices(false);
		this._sliceManager.setVerticalMarker(xMarker);
		if (slice == this._slice) {
			return;
		}
		this._dicomSlider.slider('option', 'value', slice);
		this.__showSlice(slice, sliding);
	},
	/**
	 * Actual implementation of changing the slice.
	 * @param slice
	 * @param sliding
	 */
	__showSlice: function(slice, sliding) {
		this._slice = slice;
		this._dicomLayerLabel.text('Layer ' + (slice+1) + ' of ' + this._layerCount);
		this._sliceManager.showSlice(slice, sliding);
	},
	_loading: function(message) {
		if (!message)
			message = "Loading...";
		this._container.empty();
		var loading = $('<div class="loading-indicator"><div class="throbber"></div></div>');
		loading.append($('<span/>').text(message));
		this._container.append(loading);
	},
	_error: function(message) {
		this._container.empty();
		errorBox(this._container, message);
	},
	/**
	 * Destroy (delete) all arguments related to the current UI.
	 */
	_destroyUI: function() {
		this._container.empty();
		delete this._imageryColumn;
		delete this._fundusDiv;
		delete this._thicknessDiv;
		delete this._thicknessImage;
		delete this._dicom;
		delete this._fundus;
		delete this._dicomSlider;
		this._diagnoses = {};
	},
	/**
	 * Creates the UI components.
	 * @param processed true to display the processed UI, false not to
	 */
	_createUI: function(processed, efid) {
		this._container.empty();
		this._container.attr('class', 'dicom dicom-' + (processed ? 'processed' : 'unprocessed'));
		var title = processed ? "Processed Scan" : "Unprocessed Scan";
		if (efid != null) {
			title = title + " - " + efid;
		}
		this._title = $('<h1/>').addClass('title').text(title);
		document.title = "OCT Scan Viewer - " + title;
		this._container.append(this._title);
		this._imageryColumn = $('<div/>').addClass('fundus-column');
		this._fundusDiv = $('<div/>').addClass('fundus-image');
		var me = this;
		// Create a hider for this column.
		this._imageryColumnDisclosure = $('<div>&lt;&lt;</div>').addClass('fundus-column-disclosure').click(function() {
			if (me._container.hasClass('fundus-closed')) {
				me._container.removeClass('fundus-closed');
				$(this).text('<<');
			} else {
				me._container.addClass('fundus-closed');
				$(this).text('>>');
			}
			if (me._sliceManager) {
				me._sliceManager.resized();
			}
		});
		this._container.append(this._imageryColumnDisclosure);
		if (processed) {
			// ONLY the processed scan gets the change to create a fundus slider
			this._fundusDiv.fitimage(DICOMViewer.PROCESSED_FUNDUS_URI + encodeURIComponent(this._dicomURL), {
				'onload': function() {
					$(this).before('<h1>Synthesized Fundus Photo</h1>');
					$(this).show();
					me._fundus = new FundusSlider(this, me);
				},
				// Make it square:
				'aspectRatio': 1,
				'hide': true
			});
		}
		// Both views get the chance at the original fundus photo
		this._fundusDiv.fitimage(DICOMViewer.FUNDUS_URI + '?' +
				(processed ? DICOMViewer.PROCESSED_KEY : DICOMViewer.UNPROCESSED_KEY) +
				'=' + encodeURIComponent(this._dicomURL), {
			'onload': function(img) {
				$(this).before('<h1>Fundus Photo</h1>');
				$(this).css('cursor', 'pointer');
				var fundusDialog = null;
				var sizeToFit, fitWindow;
				$(this).click(function() {
					if (fundusDialog) {
						fundusDialog.dialog('open');
						var size = fitWindow();
						fundusDialog.dialog('option', 'width', size.width);
						fundusDialog.dialog('option', 'height', size.height);
						sizeToFit();
					} else {
						var container = $('<div/>');
						container.css({
							"width": "100%",
							"height": "100%",
							"overflow": "auto"
						});
						var img = $('<img/>').attr('src', this.src);
						container.append(img);
						// FIXME: The padding added to the width and height
						// below are "magic" numbers and really should be
						// based on the style sheet.
						// Well, really, there should be a way to tell jQuery UI
						// to size the dialog to fit, but in the absence of that...
						var originalWidth = img.get(0).width;
						var originalHeight = img.get(0).height;
						var aspectRatio = originalWidth / originalHeight;
						var maxSize = false;
						fitWindow = function() {
							var width = originalWidth + 15*2;
							var height = originalHeight + 57;
							//console.log("Width: " + width + " Height: " + height + " Window: " + $(window).width() + "x" + $(window).height());
							if (width > $(window).width())
								width = $(window).width();
							if (height > $(window).height())
								height = $(window).height();
							return { "width": width, "height": height };
						}
						sizeToFit = function() {
							if (maxSize) {
								img.css({"width": originalWidth, "height": originalHeight});
								return;
							}
							// The - 1 is to basically deal with browsers that are
							// a bit ... touchy with overflow: auto and scrollbars
							var w = container.innerWidth() - 1;
							var h = container.innerHeight() - 1;
							//console.log("Sizing to fit " + originalWidth + "x" + originalHeight + " in " + w + "x" + h);
							// See which one fits better, scaling horizontally
							// or vertically.
							var aW = h * aspectRatio;
							var aH = w/ aspectRatio;
							if (aW > w) {
								console.log("Using " + w + "x" + aH);
								// Doesn't fit that way, use the height.
								img.css({ "width": w, "height": aH });
							} else {
								console.log("Using " + aW + "x" + h);
								// Otherwise, go with the other way
								img.css({ "width": aW, "height": h });
							}
						}
						var size = fitWindow();
						fundusDialog = $('<div title="Fundus Photo"></div>')
								.append(container)
								.dialog({
									width: size.width,
									height: size.height,
									resize: function() {
										if (!maxSize)
											sizeToFit();
									}
								});
						sizeToFit();
						container.bind("click", function() {
							maxSize = !maxSize;
							sizeToFit();
						});
					}
				});
				$(this).show();
			},
			'hide': true
		});
		// Create a dummy fundus before it loads (or for the unprocessed view)
		me._fundus = new FundusSlider(null, this);
		this._imageryColumn.append(this._fundusDiv);
		if (processed) {
			this._thicknessDiv = $('<div/>').addClass('thickness-map');
			var thicknessImage = new Image();
			thicknessImage.onload = function() {
				width = parseInt(thicknessImage.width);
				height = parseInt(thicknessImage.height);
				// Jam it in there
				me._thicknessDiv.append($('<h1>Thickness Map</h1>'));
				var jqImage = $(thicknessImage);
				me._thicknessDiv.append(jqImage);
				var originalWidth = jqImage.width();
				console.log("Map: " + width + "x" + height + ", fit to " + originalWidth);
				jqImage.css({
					width: originalWidth + 'px',
					height: (height * (originalWidth / width)) + 'px'
				});
			};
			thicknessImage.src =
					DICOMViewer.PROCESSED_THICKNESS_MAP + encodeURIComponent(this._dicomURL);
			this._imageryColumn.append(this._thicknessDiv);
		}
		this._container.append(this._imageryColumn);
		this._dicom = $('<div/>').addClass('dicom-imagery-container');
		this._container.append(this._dicom);
		if (processed) {
			new ProcessedFeedbackUI(this._container, this._dicomURL, this);
		} else {
			this._createUnprocessedUI();
		}
		var controls = $('<div/>').addClass('dicom-controls');
		this._dicom.append(controls);
		var me = this;
		this._dicomSlider = $('<div/>').slider({
			disabled: true, min: 0, max: 64, value: 32,
			slide: function(event, ui) {
				me._showSlice(ui.value, true);
			},
			change: function(event, ui) {
				me._showSlice(ui.value, false);
			}
		});
		controls.append(this._animateSlicesButton = $('<div>&nbsp;</div>').css("float", "left").addClass("ui-icon ui-icon-play").button().click(function() {
			me.animateSlices();
		}));
		controls.append($('<div/>').addClass("dicom-slice-slider-container").append(this._dicomSlider));
		this._dicom.append(this._dicomView = $('<div/>').addClass('dicom-imagery'));
		this._dicom.append(this._dicomLayerLabel = $('<div/>'));
		this._dicom.append(this._dicomMetadataButton = $('<div class="dicom-show-metadata">Show Metadata</div>'));
		this._dicom.append(this._annotationsButton = $('<div class="dicom-add-annotations">Loading annotations...</div>'));
		this._dicomMetadataButton.button();
		this._dicomMetadataButton.click(function () {
			me._showMetadata();
		});
	},
	_createUnprocessedUI: function() {
		new ClinicianReviewUI(this._container, 'unprocessed-diagnoses',
				'Clinical Interpretation:',
				DICOMViewer.UNPROCESSED_DIAGNOSES_URI,
				'rawQueryString', this._dicomURL,
				DICOMViewer.UNPROCESSED_DIAGNOSES);
	},
	/**
	 * Apply the metadata returned from the server.
	 * @param metadata
	 */
	_applyMetaData: function(metadata) {
		if ('elements' in metadata) {
			var elements = metadata.elements;
			// Sort tags by ???
			elements.sort(function (a,b) {
				// For now, go with the tag name
				return a.tagName.localeCompare(b.tagName);
			});
			this._metadata = elements;
		}
		if ('rows' in metadata && 'columns' in metadata && 'frames' in metadata) {
			var ar = 1;
			if ('aspectRatio' in metadata) {
				ar = metadata.aspectRatio;
			}
			this._dicom.show();
			this._layerCount = metadata.frames;
			var me = this;
			// Have the information we need to create the slice manager
			this._sliceManager = SliceManager.create(this, this._dicomView, metadata.columns, metadata.rows, metadata.frames, ar, this._colormap, function() {
				// Now that the manager is read, activate everything
				// (max is inclusive)
				me._dicomSlider.slider('option', 'max', metadata.frames - 1);
				me._fundus.setMaxSlice(metadata.frames - 1);
				me._fundus.setImageWidth(metadata.columns);
				me._dicomSlider.slider('option', 'disabled', false);
				me.showSlice(Math.floor(metadata.frames / 2) + 1);
				me._sliceManager.setVerticalMarker(me._fundus.getMarker());
				this._ignoreSliderUpdates = false;
			});
			if (this._sliceManager.canColorize()) {
				// Add the sliders to enable contrast/brightness adjustment
				this._createImageControls();
			}
			// And once we have the slice manager, we can actually create the
			// annotations manager.
			this._annotationsUI = new AnnotationsUI(this._annotationsButton, this._annotationsManager, this._sliceManager);
		} else {
			console.log("Got bad response from server, not loading frame data.");
		}
	},
	_createImageControls: function() {
		var container = $('<div/>');
		var me = this;
		container.append(this._createImageSlider('contrast', function(e, ui) {
			me._setContrast(ui.value);
		}));
		container.append(this._createImageSlider('lightbulb', function(e, ui) {
			me._setBrightness(ui.value);
		}));
		this._dicom.append(container);
	},
	_createImageSlider: function(icon, onslide) {
		var div = $('<div/>').addClass('image-slider');
		// FIXME: This probably shouldn't be hardcoded quite like this.
		div.append($('<img/>').addClass('icon').attr('src', DICOMViewer.APP_ROOT + '/plugins/famfamfam-1.0.1/images/icons/' + icon + '.png'));
		var slider = $('<div/>').slider({'min': -1.0, 'max': 1.0, 'step': 0.01, 'value': 0.0,
			'change': onslide, 'slide': onslide});
		div.append(slider);
		return div;
	},
	_setContrast: function(c) {
		this._adjustConstrast = c;
		this._sliceManager.setColorizer(Colorizer.Contrast(this._adjustConstrast, this._adjustBrightness));
	},
	_setBrightness: function(b) {
		this._adjustBrightness = b;
		this._sliceManager.setColorizer(Colorizer.Contrast(this._adjustConstrast, this._adjustBrightness));
	},
	/**
	 * If a request has succeeded, this returns the HTML required to generate
	 * an HTML table.
	 */
	createMetaDataTable: function() {
		var table = ['<table><tr><th>Tag</th><th>Value</th></tr>'];
		this._createMetaDataTable(this._metadata, table, '');
		table.push('</table>');
		return table.join('');
	},
	_createMetaDataTable: function(elements, table, depth) {
		var length = elements.length;
		for (var i = 0; i < length; i++) {
			var e = elements[i];
			// Hack to remove private elements (those elements that don't have
			// a known name):
			if (e.tag != e.tagName) {
				table.push('<tr><td title="');
				table.push(EyesFirst.escapeHTML(e.tag + ' (' + e.vr + ')'));
				table.push('">');
				if (depth) {
					table.push(depth);
					table.push(' ');
				}
				table.push(EyesFirst.escapeHTML(e.tagName));
				table.push('</td><td>');
				if (e.value != null && typeof e.value == 'object') {
					table.push('</td></tr>');
					this._createMetaDataTable(e.value, table, depth + '\u00a0\u00a0\u00a0\u00a0');
				} else {
					table.push(EyesFirst.escapeHTML(e.value));
					table.push('</td></tr>');
				}
			}
		}
	},
	_showMetadata: function() {
		if (this._metadataWindow && (!this._metadataWindow.closed)) {
			this._metadataWindow.focus();
		} else {
			this._metadataWindow = window.open('', 'DICOMViewer_Metadata', 'resizable=yes,scrollbars=yes');
			var doc = this._metadataWindow.document;
			doc.writeln('<!DOCTYPE html>');
			doc.writeln('<html><head><title>DICOM Metadata</title>');
			EyesFirst.copyCSSLinks(doc);
			doc.writeln('</head><body><h1>DICOM Metadata</h1>');
			doc.writeln(this.createMetaDataTable());
			doc.writeln('</body></html>');
			doc.close();
		}
	}
};

DICOMViewer.BASE_URL = (function(url) {
	var i = url.lastIndexOf('/');
	if (i > 0)
		url = url.substring(0, i+1);
	return url;
})(location.href);

function FundusSlider(image, viewer) {
	this._viewer = viewer;
	if (image) {
		this._back = $(image);
		// Create the slider. The slider is a custom jQuery UI widget.
		this._slider = $('<div/>').addClass('fundus-slider');
		// Position the div above the image
		this._slider.css({
			position: 'absolute',
			width: this._back.width() + 'px',
			height: this._back.height() + 'px'
		});
		this._slider.offset(this._back.offset());
		this._back.parent().append(this._slider);
		var me = this;
		this._slider.xyslider({
			slide: function(event, ui) {
				viewer._fundusShowSlice(me._maxSlice - ui.yvalue, ui.xvalue, true);
			},
			change: function(event, ui) {
				viewer._fundusShowSlice(me._maxSlice - ui.yvalue, ui.xvalue, false);
			}
		});
	}
	// Image can be null, in which case this simply does nothing.
}

FundusSlider.prototype = {
	_maxSlice: 100,
	getMarker: function() {
		return this._slider ? this._slider.xyslider('option', 'xvalue') : -1;
	},
	setValue: function(value) {
		if (this._slider)
			this._slider.xyslider('option', 'yvalue', this._maxSlice - value);
	},
	setImageWidth: function(value) {
		if (this._slider) {
			this._slider.xyslider('option', 'xmax', value);
			// And recenter the marker
			this._slider.xyslider('option', 'xvalue', value >>> 1);
		}
	},
	setMaxSlice: function(value) {
		if (this._slider)
			this._slider.xyslider('option', 'ymax', value);
		this._maxSlice = value;
	}
};