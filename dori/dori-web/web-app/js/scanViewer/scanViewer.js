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

// Configuration URLs.
{
	var root = location.pathname;
	var i = root.indexOf('/', 1);
	if (i > 0) {
		root = root.substring(0, i);
	}
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
	// UI precache stuff
	// FIXME: Need to calculate this.
	new Image().src = root + "/plugins/jquery-ui-1.8.11/jquery-ui/themes/ui-lightness/images/ui-bg_gloss-wave_35_f6a828_500x100.png";
	new Image().src = root + "/images/large-spinner.gif";
}

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
	/**
	 * Currently visible slice.
	 */
	_slice: 0,
	_showProcessedUI: false,
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
		var me = this;
		// Grab the metainfo
		console.log("Loading metadata...");
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
				me._createUI(processed);
				me._applyMetaData(data);
			},
			//timeout: 60, // timeout
			type: 'GET'
		});
	},
	/**
	 * Shows the slice at the given index.
	 * @param slice
	 */
	showSlice: function(type, slice) {
		// Update the slider
		this._dicomSlider.slider('option', 'value', slice);
		this._fundus.setValue(slice);
		// And then do the same thing the slider does
		this.__showSlice(slice, false);
	},
	/**
	 * Callback from our slider, indicating that the slice has changed.
	 * @param type
	 * @param slice
	 * @param sliding
	 */
	_showSlice: function(type, slice, sliding) {
		if (slice == this._slice) {
			return;
		}
		this.__showSlice(slice, sliding);
		this._fundus.setValue(slice);
	},
	/**
	 * Callback from the FUNDUS slider after sliding.
	 * @param slice the slice to change to
	 * @param sliding whether or not the FUNDUS slider is still sliding
	 */
	_fundusShowSlice: function(slice, sliding) {
		if (slice == this._slice) {
			return;
		}
		this.__showSlice(slice, sliding);
		this._dicomSlider.slider('option', 'value', slice);
	},
	/**
	 * Actual implementation of changing the slice.
	 * @param slice
	 * @param sliding
	 */
	__showSlice: function(slice, sliding) {
		this._slice = slice;
		this._dicomLayerLabel.text('Layer ' + (slice+1) + ' of ' + this._layerCount);
		this._sliceManager.showSlice(this._sliceType, slice, sliding);
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
	_createUI: function(processed) {
		this._container.empty();
		this._container.attr('class', 'dicom dicom-' + (processed ? 'processed' : 'unprocessed'));
		this._title = $('<h1/>').addClass('title').text(processed ? 'Processed Scan' : 'Unprocessed Scan');
		this._container.append(this._title);
		this._imageryColumn = $('<div/>').addClass('fundus-column');
		this._fundusDiv = $('<div/>').addClass('fundus-image');
		var me = this;
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
		// Both views get the chance at the original FUNDUS photo
		this._fundusDiv.fitimage(DICOMViewer.FUNDUS_URI + '?' +
				(processed ? DICOMViewer.PROCESSED_KEY : DICOMViewer.UNPROCESSED_KEY) +
				'=' + encodeURIComponent(this._dicomURL), {
			'onload': function() {
				$(this).before('<h1>Fundus Photo</h1>');
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
		//this._dicomSliceDropdown = $('<select><option value="x">X</option><option value="y">Y</option><option value="z" selected="selected">Z</option></select>');
		//controls.append(this._dicomSliceDropdown);
		this._sliceType = 'z';
		/*this._dicomSliceDropdown.bind('change', function(event) {
			me._setSliceType(this.value);
		});*/
		this._dicomSlider = $('<div/>').slider({
			disabled: true, min: 0, max: 64, value: 32,
			slide: function(event, ui) {
				me._showSlice(me._sliceType, ui.value, true);
			},
			change: function(event, ui) {
				me._showSlice(me._sliceType, ui.value, false);
			}
		});
		controls.append(this._dicomSlider);
		this._dicom.append(this._dicomView = $('<div/>').addClass('dicom-imagery'));
		this._dicom.append(this._dicomLayerLabel = $('<div/>'));
		this._dicom.append(this._dicomMetadataButton = $('<div class="dicom-show-metadata">Show Metadata</div>'));
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
	_setSliceType: function(type) {
		this._sliceType = type;
		// For now, reset the slice to 0
		this._dicomSlider.slider('option', 'max', this._sliceManager.getSliceCount(type)-1);
		this.showSlice(type, 0);
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
				me._dicomSlider.slider('option', 'disabled', false);
				me.showSlice(me._sliceType, Math.floor(metadata.frames / 2) + 1);
			});
		} else {
			console.log("Got bad response from server, not loading frame data.");
		}
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
		}
	},
	_showMetadata: function() {
		if (this._metadataWindow && (!this._metadataWindow.closed)) {
			this._metadataWindow.focus();
		} else {
			this._metadataWindow = window.open('', 'DICOMViewer_Metadata', 'resizable=yes,scrollbars=yes');
			var doc = this._metadataWindow.document;
			doc.writeln('<html><head><title>DICOM Metadata</title><link rel="stylesheet" type="text/css" href="' + DICOMViewer.BASE_URL + '/styles/dicom.css"></head><body><h1>DICOM Metadata</h1>');
			doc.writeln(this.createMetaDataTable());
			doc.writeln('</body></html>');
			doc.close();
		}
	}
};

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
		// Create the slider div. Basically, we use a mis-themed slider to
		// create the slider over the fundus image.
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
		this._slider.slider({
			orientation: 'vertical',
			slide: function(event, ui) {
				viewer._fundusShowSlice(me._maxSlice - ui.value, true);
			},
			change: function(event, ui) {
				viewer._fundusShowSlice(me._maxSlice - ui.value, false);
			}
		});
	}
	// Image can be null, in which case this simply does nothing.
}

FundusSlider.prototype = {
	_maxSlice: 100,
	setValue: function(value) {
		if (this._slider)
			this._slider.slider('option', 'value', this._maxSlice - value);
	},
	setMaxSlice: function(value) {
		if (this._slider)
			this._slider.slider('option', 'max', value);
		this._maxSlice = value;
	}
};