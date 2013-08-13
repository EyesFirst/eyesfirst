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
 * The generic slice manager API.
 * <p>
 * Warning: the base slice manager is mostly untested at present.
 */

/**
 * Base class for managing slices.
 * @constructor
 */
function SliceManager(viewer, container, width, height, depth, aspectRatio, callback) {
	// Must work when created as the prototype
	if (arguments.length == 0)
		return;
	// If the missing image wasn't loaded yet, throw it in
	if (!SliceManager.ERROR_IMAGE) {
		SliceManager.ERROR_IMAGE = new Image();
		SliceManager.ERROR_IMAGE.src = DICOMViewer.APP_ROOT + "/images/viewer-missing-image.png";
	}
	this._viewer = viewer;
	this._width = width;
	this._height = height;
	this._depth = depth;
	this.aspectRatio = (aspectRatio == null) ? 1 : aspectRatio;
	this.sourceAspectRatio = width / height;
	this._container = container;
	this.slices = new Array(this._depth);
	// Create the loading indicator.
	this._loading = $('<div class="loading-indicator"><div class="throbber"></div> Loading...</div>');
	this._loading.append(this._loadingProgress = $('<div/>'));
	this._loadingProgress.progressbar();
	this._container.append(this._loading);
	// Bind a resize listener
	$(window).resize((function(me) {
		var width=0, height=0;
		return function() {
			// IE likes to fire spurious resize events, so ignore resize events
			// that haven't actually resized the window.
			var w = $(window).width();
			var h = $(window).height();
			if (w != width || h != height) {
				width = w;
				height = h;
				me.resized();
			}
		};
	})(this));
	this.init(callback);
}

// Maximum number of images to load at once.
SliceManager.MAX_BATCH_SIZE = 4;
// Time between frames for the zoom 
SliceManager.ZOOM_TIMEOUT = 17;
// How large to zoom in on images
SliceManager.MAX_ZOOM = 5;
// Time in milliseconds to do the animation.
SliceManager.ZOOM_TIME = 200;
// The number of pixels around a hard exudate to include in the zoomed view
SliceManager.ZOOM_CONTEXT = 8;
// Maximum distance squared a exudate is before we consider it "clicked".
SliceManager.MAX_FUZZ = 8*8;

// Colors to use.
SliceManager.THEME = {
	MACHINE_TAG: 'rgb(204,51,51)',
	MACHINE_TAG_HIGHLIGHT: 'rgb(255,153,153)',
	MACHINE_TAG_CONTEXT: 'rgb(204,204,51)',
	ANNOTATION: 'rgb(51,102,204)',
	ANNOTATION_HIGHLIGHT: 'rgb(102,153,255)',
	RUBBER_BAND: 'rgb(0,0,255)'
};

SliceManager.distance = function(given, start, end) {
	if (given < start) {
		return start - given;
	} else if (given > end) {
		return given - end;
	} else {
		return 0;
	}
};

SliceManager.prototype = {
	/**
	 * The array of slice imagery. Stores the "fast-time" slices.
	 * @protected
	 */
	slices: [],
	/**
	 * The array of hard exudates, if any.
	 * @protected
	 */
	hardExudates: [],
	/**
	 * The array of annotations, if any.
	 */
	annotations: [],
	/**
	 * The currently selected hard exudate, if any.
	 * @protected
	 */
	selectedHardExudate: null,
	/**
	 * The currently selected annotaiton, if any.
	 */
	selectedAnnotation: null,
	/**
	 * The currently selected object, if any.
	 */
	selectedObject: null,
	/**
	 * The aspect ratio to display the image as. Defaults to 1 (a square).
	 */
	aspectRatio: 1,
	/**
	 * The aspect ratio of the source. This is simply width/height.
	 */
	sourceAspectRatio: 1,
	/**
	 * Whether or not to "maximize" the image, making it fill such that the
	 * the pixel count for one side of the image is 1. (So if this.aspectRatio
	 * is greater than 1 (wider than tall), the height is set to the source
	 * height. Otherwise, it's set to the source width.)
	 */
	maximized: false,
	/**
	 * The currently active slice.
	 * @protected
	 */
	_slice: 0,
	_zoom: SliceManager.MAX_ZOOM,
	theme: SliceManager.THEME,
	/**
	 * Whether or not an annotation can be added right now. When true, clicking
	 * and dragging on the slice manager creates an annotation.
	 * @protected
	 */
	addingAnnotation: false,
	/**
	 * The annotation manager.
	 * @protected
	 */
	annotationManager: null,
	/**
	 * Initialize the slice manager. Called by the constructor. The callback
	 * should be notified once the manager is ready. The default method sets
	 * up the area used to display the slice and then invokes
	 * {@link #loadSlices(function)} with the callback to load all slices.
	 */
	init: function(callback) {
		var cb = (function(me) {
			return function() {
				me._sliceImage = $('<img/>');
				me._overlay = $('<div/>');
				me._container.append(me._sliceImage);
				me._container.append(me._overlay);
				me.createMouseListeners(me._overlay);
				callback();
				// Pretend we were resized to fix the aspect ratio
				me.resized();
			};
		})(this);
		this.loadSlices(cb);
	},
	/**
	 * Creates the mouse listeners that will handle various default functionality
	 * (selecting hard exudates, creating annotations).
	 * @protected
	 */
	createMouseListeners: function(over) {
		var me = this;
		$(over).click(function(event) {
			if (me.addingAnnotation) {
				// Ignore this while adding an annotation.
				return;
			}
			var p = $(over).offset();
			var x = Math.floor(event.pageX - p.left + 0.5 - 1);
			var y = Math.floor(event.pageY - p.top + 0.5 - 1);
			var exudate = me.findHardExudateUnder(x, y);
			if (exudate != null) {
				me.selectHardExudate(exudate);
				return;
			}
			if (me.annotationManager) {
				var annotation = me.findAnnotationUnder(x, y);
				if (annotation != null) {
					var size = me.getDisplaySize();
					var x = annotation.x * size.width / me._width;
					var y = annotation.y * size.height / me._height;
					var width = annotation.width * size.width / me._width;
					var height = annotation.height * size.height / me._height;
					x += p.left;
					y += p.top;
					me.annotationManager.showAnnotation(annotation, x, y, width, height);
					return;
				}
			}
			me.selectHardExudate(null);
		});
		$(over).mousedown(function(event) {
			if (me.addingAnnotation) {
				// Our temporary variables:
				var offset = $(over).offset();
				var x = event.pageX - offset.left;
				var y = event.pageY - offset.top;
				var x2 = x, y2 = y;
				me.drawRubberBand(me.theme.RUBBER_BAND,
						x, y, 0, 0);
				console.log("Starting drag at (" + x + "," + y + ")");
				function constrainToImage() {
					// Fun with closures!
					if (x2 < 0)
						x2 = 0;
					if (y2 < 0)
						y2 = 0;
					var size = me.getDisplaySize();
					if (x2 >= size.width)
						x2 = size.width - 1;
					if (y2 >= size.height)
						y2 = size.height - 1;
				}
				function mouseup(event) {
					me.hideRubberBand();
					$(document).unbind('mouseup', mouseup);
					$(document).unbind('mousemove', mousedrag);
					// TODO: (maybe): Require a "long" drag.
					offset = $(over).offset();
					x2 = event.pageX - offset.left;
					y2 = event.pageY - offset.top;
					constrainToImage();
					if (x2 < x) {
						var t = x;
						x = x2;
						x2 = t;
					}
					if (y2 < y) {
						var t = y;
						y = y2;
						y2 = t;
					}
					var pos = {
						'left': x+offset.left, 'top': y+offset.top, 'width': x2-x, 'height': y2-y
					};
					// Convert coordinates to image coordinates
					var p1 = me.sliceCoordinates(x, y);
					var p2 = me.sliceCoordinates(x2, y2);
					// And send it off to the annotation UI.
					console.log("Creating annotation at (" + p1.x + "," + p1.y + ")-(" + p2.x + "," + p2.y + ")");
					me.annotationManager.addAnnotation(p1.x, p1.y, me._slice, p2.x-p1.x, p2.y-p1.y, 1, pos);
				}
				function mousedrag(event) {
					offset = $(over).offset();
					x2 = event.pageX - offset.left;
					y2 = event.pageY - offset.top;
					constrainToImage();
					me.drawRubberBand(me.theme.RUBBER_BAND,
							Math.min(x, x2), Math.min(y, y2),
							Math.abs(x - x2), Math.abs(y - y2));
					event.preventDefault();
				}
				$(document).bind('mouseup', mouseup);
				$(document).bind('mousemove', mousedrag);
				//event.preventDefault();
			}
		});
	},
	/**
	 * Sets whether or not an annotation is being added. When in adding
	 * annotation mode, clicking and dragging on the slice manager creates
	 * a new annotation.
	 */
	setAddingAnnotation: function(adding) {
		this.addingAnnotation = this.annotationManager != null && adding;
	},
	/**
	 * Set the annotation manager. (Actually, this is the annotation manager
	 * UI.)
	 */
	setAnnotationManager: function(annotationManager) {
		this.annotationManager = annotationManager;
	},
	/**
	 * Loads a slice from the server. This always generates a new image object
	 * and does not use the slices array.
	 * @protected
	 * @param type
	 *            the type of slice to fetch
	 * @param {number}
	 *            slice the 0-based index of the slice to fetch
	 * @param onload
	 *            callback to call once the image loads successfully
	 * @param onerror
	 *            callback to call if the image fails to load/is aborted while
	 *            loading
	 * @returns {Image} the image object (which will not be loaded yet)
	 */
	loadSlice: function(type, slice, onload, onerror) {
		var image = new Image();
		image.onload = onload;
		image.onabort = onerror;
		image.onerror = onerror;
		// For debugging positioning bugs:
		//image.src = '../images/test_slice.jpg';
		image.src = this._viewer._url + '/slices/' + type + '/' + (slice+1);
		return image;
	},
	/**
	 * Load all slices, invoking the given callback when done. This method is
	 * called by default via {@link #init()}.
	 * @protected
	 * @param callback the callback to invoke
	 */
	loadSlices: function(callback) {
		var me = this;
		var loaded = 0;
		var i = 0;
		var maxToLoad = 0;
		var onLoad = function() {
			loaded++;
			var percent = (loaded / me._depth) * 100;
			me._loadingProgress.progressbar('value', percent);
			if (loaded >= me._depth) {
				$.favicon('reset');
				me._loading.remove();
				delete me._loading;
				delete me._loadingProgress;
				callback();
			} else {
				$.favicon('percent', percent);
				if (loaded >= i) {
					// Load the next batch
					loadBatch();
				}
			}
		};
		// In order to allow ... certain browsers to properly update their
		// favicons as we load the page, make sure we only load in batches of
		// SliceManager.MAX_BATCH_SIZE.
		var loadBatch = function() {
			maxToLoad = Math.min(me._depth, maxToLoad + SliceManager.MAX_BATCH_SIZE);
			for (; i < maxToLoad; i++) {
				me._loadSlice(i, onLoad);
			}
		}
		loadBatch();
	},
	/**
	 * Internal method to load a single slice.
	 * @private
	 */
	_loadSlice: function(i, onLoad) {
		this.slices[i] = this.loadSlice('z', i, onLoad, (function(me, i, onLoad) {
			return function() {
				me.slices[i] = SliceManager.ERROR_IMAGE;
				// Otherwise, pretend it was a success
				onLoad();
			};
		})(this, i, onLoad));
	},
	/**
	 * Display the given slice.
	 * @param {number}
	 *            slice the 0-based index of the slice to fetch
	 * @param sliding
	 *            true if the slider is being slid, false if the user has let
	 *            go
	 * @returns
	 */
	showSlice: function(slice, sliding) {
		slice = Math.floor(slice);
		this._slice = slice;
		if (this.slices[slice]) {
			this._sliceImage.attr('src', this.slices[slice].src);
		}
		this.redraw();
	},
	/**
	 * Gets the number of slices of a given type. If a given type cannot be
	 * displayed by this manager, returns 0.
	 * @param type the slice type
	 * @returns the number of slices of that type
	 */
	getSliceCount: function(type) {
		if (type == 'x') {
			return this._width;
		}
		return 0;
	},
	/**
	 * Destroy the scan viewer - removes references to the various components.
	 */
	destroy: function() {
		this.slices = [];
		this.hardExudates = [];
		this.selectedHardExudate = null;
	},
	/**
	 * Gets the aspect-ratio corrected height for the current width of the
	 * container.
	 * 
	 * @return an object with two properties, width and height - the width is
	 *         the width of the container, the height is the aspect-ratio
	 *         corrected height for that container
	 */
	getSizeForAspectRatio: function() {
		var width = this._container.innerWidth();
		// Calculate a height based on the aspect ratio
		var height = Math.floor(width / this.aspectRatio + 0.5);
		return {
			width: width, height: height
		};
	},
	/**
	 * Gets the display size. The default implementation returns the size of
	 * the slice image.
	 * @return an object with a width and height field
	 */
	getDisplaySize: function() {
		return {
			'width': this._sliceImage.innerWidth(),
			'height': this._sliceImage.innerHeight()
		};
	},
	/**
	 * If the image is maximized, returns the aspect-ratio corrected maximized
	 * size. Otherwise, returns the same value for {@link #getSizeForAspectRatio}.
	 */
	getCorrectedSize: function() {
		if (this.maximized) {
			if (this.aspectRatio >= 1.0) {
				return {
					width: this._height * this.aspectRatio,
					height: this._height
				};
			} else {
				return {
					width: this._width,
					height: this._width * this.aspectRatio
				};
			}
		} else {
			return this.getSizeForAspectRatio();
		}
	},
	/**
	 * Invoked whenever the slice manager has been resized. The default slice
	 * manager uses an <code>&lt;img&gt;</code> to display the
	 */
	resized: function() {
		// First, hide the image
		this._sliceImage.css('display', 'none');
		var size = this.getCorrectedSize();
		// Use it
		this._sliceImage.css({
			width: size.width + 'px',
			height: size.height + 'px',
			display: 'inline'
		});
		var p = this._sliceImage.offset();
		this._overlay.css({
			'position': 'absolute',
			'left': p.left, 'top': p.top,
			'width': size.width, 'height': size.height
		});
		this.redraw();
	},
	/**
	 * Forces the image to redraw. Use after changing the hard exudates or the
	 * images to make sure that the image is updated.
	 */
	redraw: function() {
		this._overlay.empty();
		this.drawOverlays();
	},
	/**
	 * Adds the hard exudates information to the scan viewer, so that they'll be
	 * highlighted on the scans.
	 * @param hardExudates the hard exudates to show, as an array of HardExudate objects
	 */
	setHardExudates: function(hardExudates) {
		// TODO (probably not): Keep a list of hard exudates per slice to speed
		// look-up times. Currently this sounds pointless, as the number of
		// hard exudates per scan is expected to be extremely low. (On the order
		// of no more than 20.)
		this.hardExudates = hardExudates;
	},
	/**
	 * Sets the array of annotations to use. Currently this is NOT copied, it
	 * is instead referenced. This means that as annotations are added and
	 * removed, you can just
	 */
	setAnnotations: function(annotations) {
		this.annotations = annotations;
		// The annotations can load before we've finished loading.
		if (!this._loading)
			this.redraw();
		console.log("Annotations");
		console.log(annotations);
	},
	/**
	 * @protected
	 * Given the current width and height, invokes the drawHardExudate and
	 * drawAnnotation functions for all annotations and hard exudates on the
	 * current slice.
	 * <p>
	 * If the hard exudate popup is currently visible, this will also invoke the
	 * drawHardExudatePopup() function with any updated information.
	 */
	drawOverlays: function() {
		var displaySize = this.getDisplaySize();
		var displayWidth = displaySize['width'];
		var displayHeight = displaySize['height'];
		var rect;
		for (var i = 0; i < this.hardExudates.length; i++) {
			if (this.hardExudates[i].isInLayer(this._slice)) {
				rect = this._boundingBoxToDisplayBox(this.hardExudates[i].getBoundingBox(), displayWidth, displayHeight);
				this.drawHardExudate(this.hardExudates[i],
						this.hardExudates[i] == this.selectedHardExudate,
						this.hardExudates[i] == this.hovered,
						rect.x, rect.y, rect.width, rect.height);
			}
		}
		for (var i = 0; i < this.annotations.length; i++) {
			if (this.annotations[i].isInLayer(this._slice)) {
				rect = this._boundingBoxToDisplayBox(this.annotations[i].getBoundingBox(), displayWidth, displayHeight);
				this.drawAnnotation(this.annotations[i],
						false,
						this.annotations[i] == this.hovered,
						rect.x, rect.y, rect.width, rect.height);
			}
		}
		if (this.selectedHardExudate) {
			this._drawHardExudatePopup(this.selectedHardExudate);
		}
	},
	_boundingBoxToDisplayBox: function(r, displayWidth, displayHeight) {
		var x =      (r.x      / this._width ) * displayWidth;
		var width =  (r.width  / this._width ) * displayWidth;
		var y =      (r.y      / this._height) * displayHeight;
		var height = (r.height / this._height) * displayHeight;
		/* Debugging crap verifying that the calculated aspect ratio
		 * is what it's supposed to be (within a margin of error due to
		 * the rounding of the final canvas size)
		console.log("Drawing hard exudate " + r + " at (" + x + ", " +
				y + "), [" + width + " x " + height + "]");
		var ar = r.width / r.height;
		console.log("Original AR: " + ar + ", when corrected: " + (r.width/r.height)*(this._height/this._width)*this.aspectRatio);
		console.log("Calculated: " + width/height);/**/
		return { x: x, y: y, width: width, height: height};
	},
	/**
	 * @protected
	 * Draw a single annotation. Annotations are very similar to hard exudates,
	 * except that they exist
	 */
	drawAnnotation: function(annotation, selected, hovered, x, y, width, height) {
		this.drawBox(selected ? this.theme.ANNOTATION_HIGHLIGHT :
			this.theme.ANNOTATION, 2, x, y, width, height);
	},
	/**
	 * @protected
	 * Called to draw a box around a single hard exudate. The default method
	 * creates a DIV around it with a single red border.
	 */
	drawHardExudate: function(exudate, selected, hovered, x, y, width, height) {
		this.drawBox(selected ? this.theme.MACHINE_TAG_HIGHLIGHT :
					this.theme.MACHINE_TAG, 2, x, y, width, height);
	},
	/**
	 * @protected
	 * Draw a box on top of the current image. The default drawAnnotation and
	 * drawHardExudate functions use this to actually draw the final box.
	 * It is expected that the box will remain until the next call to
	 * {@link #drawOverlays()}.
	 */
	drawBox: function(color, borderWidth, x, y, width, height) {
		var div = $('<div/>').css({
			'position': 'absolute',
			'top': y-borderWidth, 'left': x-borderWidth,
			'width': width, 'height': height,
			'border': 'solid ' + borderWidth + 'px ' + color
		});
		this._overlay.append(div);
	},
	/**
	 * Draw a "rubber band" on top of the image. The "rubber band" is temporary
	 * and will likely be changed later by a future call to this function or
	 * by removing it completely with hideRubberBand().
	 * <p>
	 * The default method places a {@code <div>} into the container and
	 * absolutely positions it relative to the container to draw the band.
	 */
	drawRubberBand: function(color, x, y, width, height) {
		if (!this._rubberBand) {
			this._rubberBand = $('<div/>');
			this._container.append(this._rubberBand);
		}
		var p = this._container.offset();
		this._rubberBand.css({
			'position': 'absolute',
			'top': p.top + y-2, 'left': p.left + x-2,
			'width': width, 'height': height,
			'border': 'solid 2px ' + color,
			'opacity': 0.5,
			'display': 'block'
		});
	},
	hideRubberBand: function() {
		if (this._rubberBand)
			this._rubberBand.css('display', 'none');
	},
	selectHardExudate: function(hardExudate) {
		this.selectedHardExudate = hardExudate;
		if (hardExudate == null) {
			this.hideHardExudatePopup();
		} else {
			this.showHardExudatePopup(hardExudate);
		}
	},
	/**
	 * Given an x/y coordinate in the given slice, finds the hard exudate
	 * displayed under it, if any. This implements a slight "fuzz" factor to
	 * find the closest hard exudate.
	 */
	findHardExudateUnder: function(x, y) {
		var displaySize = this.getDisplaySize();
		var displayWidth = displaySize['width'];
		var displayHeight = displaySize['height'];
		var r, ex, ey, width, height;
		var bestD = Infinity, best = null;
		// Go through the hard exudate list backwards...
		for (var i = this.hardExudates.length - 1; i >= 0; i--) {
			if (this.hardExudates[i].isInLayer(this._slice)) {
				var r = this.hardExudates[i].getBoundingBox();
				// Create a version that's in our coordinates
				ex =     (r.x      / this._width ) * displayWidth;
				width =  (r.width  / this._width ) * displayWidth;
				ey =     (r.y      / this._height) * displayHeight;
				height = (r.height / this._height) * displayHeight;
				var dx = SliceManager.distance(x, ex, ex+width);
				var dy = SliceManager.distance(y, ey, ey+height);
				if (dx == 0 && dy == 0) {
					// Return immediately
					return this.hardExudates[i];
				}
				var d = dx * dx + dy * dy;
				//console.log("Fuzz is " + d);
				if (d < bestD) {
					best = this.hardExudates[i];
					bestD = d;
				}
			}
		}
		return bestD < SliceManager.MAX_FUZZ ? best : null;
	},
	/**
	 * Given an x/y coordinate in the given slice, finds the annotation
	 * displayed under it, if any. This implements a slight "fuzz" factor to
	 * find the closest annotation.
	 */
	findAnnotationUnder: function(x, y) {
		var displaySize = this.getDisplaySize();
		var displayWidth = displaySize['width'];
		var displayHeight = displaySize['height'];
		var r, ex, ey, width, height;
		var bestD = Infinity, best = null;
		// Go through the annotation list backwards...
		for (var i = this.annotations.length - 1; i >= 0; i--) {
			if (this.annotations[i].isInLayer(this._slice)) {
				var r = this.annotations[i].getBoundingBox();
				// Create a version that's in our coordinates
				ex =     (r.x      / this._width ) * displayWidth;
				width =  (r.width  / this._width ) * displayWidth;
				ey =     (r.y      / this._height) * displayHeight;
				height = (r.height / this._height) * displayHeight;
				var dx = SliceManager.distance(x, ex, ex+width);
				var dy = SliceManager.distance(y, ey, ey+height);
				if (dx == 0 && dy == 0) {
					// Return immediately
					return this.annotations[i];
				}
				var d = dx * dx + dy * dy;
				//console.log("Fuzz is " + d);
				if (d < bestD) {
					best = this.annotations[i];
					bestD = d;
				}
			}
		}
		return bestD < SliceManager.MAX_FUZZ ? best : null;
	},
	/**
	 * Show a popup showing detail for the given hard exudate.
	 */
	showHardExudatePopup: function(exudate) {
		if (!this._hardExudatePopup) {
			this._hardExudatePopup = $('<div/>').css({
				'position': 'absolute',
				'border': 'solid 2px ' +
					this.theme.MACHINE_TAG_HIGHLIGHT
			});
			this._container.append(this._hardExudatePopup);
			this.initHardExudatePopup(this._hardExudatePopup);
		}
		// Start the zooming!
		var size = this.getDisplaySize();
		this._zoom = Math.min(size.width/this._width, size.height/this._height);
		// Set this here to account for startup time
		var zoomNext = new Date().getTime() + SliceManager.ZOOM_TIMEOUT;
		this._hardExudatePopup.show();
		this._drawHardExudatePopup(exudate);
		if (this._zoomAnim) {
			// Stop it.
			clearTimeout(this._zoomAnim);
		}
		var zoomStep = (SliceManager.MAX_ZOOM - this._zoom) / (SliceManager.ZOOM_TIME / SliceManager.ZOOM_TIMEOUT);
		var me = this;
		var zoomAnim = function() {
			me._zoom += zoomStep;
			if (me._zoom >= SliceManager.MAX_ZOOM) {
				me._zoom = SliceManager.MAX_ZOOM;
				me._zoomAnim = null;
			} else {
				me._drawHardExudatePopup(exudate);
				var now = new Date().getTime();
				// See if we missed any, and skip past them if we did
				var missed = Math.floor((now - zoomNext) / SliceManager.ZOOM_TIMEOUT);
				me._zoom += zoomStep * missed;
				zoomNext += SliceManager.ZOOM_TIMEOUT * (missed + 1);
				me._zoomAnim = setTimeout(zoomAnim, zoomNext - now);
			}
		}
		setTimeout(zoomAnim, Math.max(1, zoomNext - new Date().getTime()));
	},
	/**
	 * @protected
	 * Initialize the contents of the hard exudate popup.
	 */
	initHardExudatePopup: function(popup) {
		// Note that position: absolute in this case is relative to our parent,
		// so this works.
		this._hardExudatePopupContents = $('<img/>').css('position', 'absolute').addClass('nearest-neighbor');
		popup.append(this._hardExudatePopupContents);
		this._hardExudatePopupContext = $('<div/>').css({
			'position': 'absolute',
			'border': 'solid 1px ' + this.theme.MACHINE_TAG_CONTEXT
		});
		popup.append(this._hardExudatePopupContext);
		popup.css('overflow', 'hidden');
	},
	_drawHardExudatePopup: function(exudate) {
		if (!exudate.isInLayer(this._slice)) {
			this._hardExudatePopup.hide();
		} else {
			this._hardExudatePopup.show();
		}
		// Calculate the portion of the slice to render...
		// Clone the bounding box (we're going to be messing with it)
		var bb = exudate.getBoundingBox();
		// The zoom context - eventually this will be configurable somewhere?
		var imageBB = new Rectangle3D(bb);
		//console.log("Initial bounding box: " + imageBB);
		imageBB.x -= SliceManager.ZOOM_CONTEXT;
		imageBB.y -= SliceManager.ZOOM_CONTEXT;
		imageBB.width += SliceManager.ZOOM_CONTEXT*2;
		imageBB.height += SliceManager.ZOOM_CONTEXT*2;
		//console.log("Including context pixels: " + imageBB);
		// And now, constraint it.
		if (imageBB.x < 0) {
			imageBB.x = 0;
		}
		if (imageBB.y < 0) {
			imageBB.y = 0;
		}
		if (imageBB.x + imageBB.width > this._width) {
			imageBB.width = this._width - imageBB.x;
		}
		if (imageBB.y + imageBB.height > this._height) {
			imageBB.height = this._height - imageBB.y;
		}
		var zoomWidth = imageBB.width * this._zoom, zoomHeight = imageBB.height * this._zoom;
		var contextX, contextY;
		contextX = contextY = SliceManager.ZOOM_CONTEXT * this._zoom;
		// First, correct for the aspect ratio of the source, to make the zoom have
		// "square" pixels.
		if (this.sourceAspectRatio > 1) {
			// Source is wider than it is tall, bump out height to make square pixels
			zoomHeight *= this.sourceAspectRatio;
			contextY *= this.sourceAspectRatio;
		} else {
			// Source is taller than it is wide, bump out width to make square pixels
			zoomWidth /= this.sourceAspectRatio;
			contextX /= this.sourceAspectRatio;
		}
		// Now that the zoom has "square" pixels, we can apply the final aspect ratio
		if (this.aspectRatio > 1) {
			// Wider than it is tall, keep height as-is
			zoomWidth *= this.aspectRatio;
			contextX *= this.aspectRatio;
		} else {
			// Taller than it is wide, keep width as-is
			zoomHeight /= this.aspectRatio;
			contextY /= this.aspectRatio;
		}
		bb = new Rectangle3D(bb);
		// Correct the bounding box for our aspect ratio
		var displaySize = this.getDisplaySize();
		var displayWidth = displaySize['width'];
		var displayHeight = displaySize['height'];
		bb.x = bb.x / this._width * displayWidth;
		bb.y = bb.y / this._height * displayHeight;
		bb.width = bb.width / this._width * displayWidth;
		bb.height = bb.height / this._height * displayHeight;
		// Initially position this off above and to the left of the source
		zoomX = bb.x + (bb.width/2) - (zoomWidth/2) - 2;// + bb.width - zoomWidth - 2;
		zoomY = bb.y + (bb.height/2) - (zoomHeight/2) - 2;// - zoomHeight - 2;
		if (zoomX < 0) {
			zoomX = 0;
		}
		if (zoomX + zoomWidth > displayWidth) {
			// Try and place it left
			zoomX = displayWidth - zoomWidth;
			if (zoomX < 0) {
				// Fine, place the x such that we're centered as best as possible
				zoomX = (zoomWidth - displayWidth/2);
			}
		}
		if (zoomY < 0) {
			// Drop it below
			zoomY = 0;
		}
		if (zoomY + zoomHeight > displayHeight) {
			zoomX = displayHeight - zoomHeight;
			if (zoomY < 0) {
				// Fine, place the x such that we're centered as best as possible
				zoomY = (zoomHeight - displayHeight/2);
			}
		}
		// Round everything off
		zoomX = Math.floor(zoomX + 0.5);
		zoomY = Math.floor(zoomY + 0.5);
		zoomWidth = Math.floor(zoomWidth + 0.5);
		zoomHeight = Math.floor(zoomHeight + 0.5);
		// And position our popup
		var offset = this._container.offset();
		this._hardExudatePopup.css({
			'left': zoomX + offset.left,
			'top': zoomY + offset.top,
			'width': zoomWidth,
			'height': zoomHeight
		});
		this.drawHardExudatePopup(exudate,
				imageBB.x, imageBB.y, imageBB.width, imageBB.height,
				SliceManager.ZOOM_CONTEXT, SliceManager.ZOOM_CONTEXT,
				zoomWidth, zoomHeight);
	},
	/**
	 * @protected
	 * Draw the contents of the hard exudate popup.
	 *
	 * @param exudate
	 *            the specific hard exudate object being drawn
	 * @param sliceX
	 *            the X coordinate within the slice
	 * @param sliceY
	 *            the Y coordinate within the slice
	 * @param sliceWidth
	 *            the width in slice pixels
	 * @param sliceHeight
	 *            the height in slice pixels
	 * @param contextHoriztonal
	 *            the number of slice pixels left/right that are context
	 *            and NOT the actual tagged exudate
	 * @param contextVertical
	 *            the number of slice pixels top/bottom that are context
	 *            and NOT the actual tagged exudate
	 * @param displayWidth
	 *            the width to display the portion of the slice
	 * @param displayHeight
	 *            the height to display the portion of the slice
	 */
	drawHardExudatePopup: function(exudate, sliceX, sliceY,
			sliceWidth, sliceHeight,
			contextHorizontal, contextVertical,
			displayWidth, displayHeight) {
		var slice = this.slices[this._slice];
		var zoomX = displayWidth / sliceWidth;
		var zoomY = displayHeight / sliceHeight;
		this._hardExudatePopupContents.css({
			'left': -(sliceX * zoomX),
			'top': -(sliceY * zoomY),
			'width': this._width * zoomX,
			'height': this._height * zoomY
		});
		this._hardExudatePopupContents.attr('src', slice.src);
		this._hardExudatePopupContext.css({
			'left': contextHorizontal * zoomX,
			'top': contextVertical * zoomY,
			'width': (sliceWidth - contextHorizontal*2) * zoomX,
			'height': (sliceHeight - contextVertical*2) * zoomY
		});
	},
	/**
	 * Hide the existing hard exudate popup if it's currently visible.
	 */
	hideHardExudatePopup: function() {
		if (this._hardExudatePopup) {
			this._hardExudatePopup.hide();
		}
	},
	/**
	 * Convert the given display coordinates to slice coordinates. The default
	 * implementation uses the display image.
	 */
	sliceCoordinates: function(dispX, dispY) {
		var size = this.getDisplaySize();
		return {
			x: dispX / size.width * this._width,
			y: dispY / size.height * this._height
		};
	},
	/**
	 * Does nothing.
	 */
	setColorizer: function(colorizer) {
	},
	/**
	 * Determine whether this slice manager can colorize. The image
	 * implementation can't, the canvas one can.
	 */
	canColorize: function() {
		return false;
	},
	/**
	 * Display a vertical marker on the image. This is just a vertical line
	 * across the given slice of pixels.
	 * @param {number} x the x coordinate to place the vertical line, or a
	 * negative number to hide it
	 */
	setVerticalMarker: function(x) {
		if (!this.verticalMarker) {
			this.verticalMarker = $('<div/>').css({
				"background-color": "#F00",
				"width": "1px",
				"height": "100%"
			});
		}
		this.verticalMarkerX = x;
	}
};

/**
 * Create an appropriate slice manager (either the canvas viewer if canvas is
 * available, or the fall-back image manager if it is not).
 * @param viewer
 * @param container
 * @param width
 * @param height
 * @param depth
 * @param aspectRatio
 * @param callback
 * @returns
 */
SliceManager.create = function(viewer, container, width, height, depth, aspectRatio, colormap, callback) {
	var colorizer = null;
	if (colormap) {
		colormap = colormap.toUpperCase();
		if (colormap.charAt(0) != '_' && colormap in Colorizer) {
			colorizer = Colorizer[colormap];
		}
	}
	if (SliceManager.Canvas || SliceManager.WebGL) {
		// See if we can use those
		var canvas = document.createElement('canvas');
		if (canvas.getContext) {
			if (SliceManager.WebGL && (canvas.getContext('webgl') || canvas.getContext('experimental-webgl'))) {
				return new SliceManager.WebGL(viewer, container, width, height, depth, aspectRatio, callback, canvas, colorizer);
			} else if (SliceManager.Canvas && canvas.getContext('2d')) {
				return new SliceManager.Canvas(viewer, container, width, height, depth, aspectRatio, callback, canvas, colorizer);
			}
		}
	}
	return new SliceManager(viewer, container, width, height, depth, aspectRatio, callback);
};