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
 * Slice managers
 */

/**
 * Base class for managing slices.
 * @constructor
 */
function SliceManager(viewer, container, width, height, depth, aspectRatio, callback) {
	// Must work when created as the prototype
	if (arguments.length == 0)
		return;
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

SliceManager.MAX_BATCH_SIZE = 4;
// Time between frames for the zoom 
SliceManager.ZOOM_TIMEOUT = 50;
// How large to zoom in on images
SliceManager.MAX_ZOOM = 5;
// The number of pixels around a hard exudate to include in the zoomed view
SliceManager.ZOOM_CONTEXT = 8;

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
	 * The currently selected hard exudate, if any.
	 * @protected
	 */
	selectedHardExudate: null,
	/**
	 * The aspect ratio to display the image as. Defaults to 1 (a square).
	 */
	aspectRatio: 1,
	/**
	 * The aspect ratio of the source. This is simply width/height.
	 */
	sourceAspectRatio: 1,
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
				me._container.append(me._sliceImage);
				me.createClickListener(me._sliceImage);
				// Pretend we were resized to fix the aspect ratio
				me.resized();
				callback();
			};
		})(this);
		this.loadSlices(cb);
	},
	/**
	 * Create a click listener that will select hard exudates for the given
	 * object.
	 * @protected
	 */
	createClickListener: function(over) {
		$(over).click((function(me){
			return function(event) {
				var p = $(over).offset();
				var x = event.pageX - p.left;
				var y = event.pageY - p.top;
				me.selectHardExudate(me.findHardExudateUnder(x, y));
			};
		})(this));
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
				me._loading.hide();
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
	 * @param type
	 *            the type of slice to fetch
	 * @param {number}
	 *            slice the 0-based index of the slice to fetch
	 * @param sliding
	 *            true if the slider is being slid, false if the user has let
	 *            go
	 * @returns
	 */
	showSlice: function(type, slice, sliding) {
		slice = Math.floor(slice);
		this._slice = slice;
		if (this.slices[slice]) {
			this._sliceImage.attr('src', this.slices[slice].src);
		}
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
	 * Invoked whenever the slice manager has been resized. The default slice
	 * manager uses an <code>&lt;img&gt;</code> to display the
	 */
	resized: function() {
		// First, hide the image
		this._sliceImage.css('display', 'none');
		var size = this.getSizeForAspectRatio();
		// Use it
		this._sliceImage.css({
			width: size.width + 'px',
			height: size.height + 'px',
			display: 'inline'
		});
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
	selectHardExudate: function(hardExudate) {
		this.selectedHardExudate = hardExudate;
	},
	/**
	 * Given an x/y coordinate in the given slice, finds the hard exudate
	 * displayed under it, if any.
	 */
	findHardExudateUnder: function(x, y) {
		var p = this.sliceCoordinates(x, y);
		// Go through the hard exudate list backwards...
		for (var i = this.hardExudates.length - 1; i >= 0; i--) {
			// FIXME: Make this fuzzier, some of the smallest ones are impossible
			// to click on.
			if (this.hardExudates[i].isInLayer(this._slice)) {
				var bb = this.hardExudates[i].getBoundingBox();
				if (bb.containsX(p.x) && bb.containsY(p.y))
					return this.hardExudates[i];
			}
		}
		return null;
	},
	/**
	 * Convert the given display coordinates to slice coordinates. The default
	 * implementation uses the display image.
	 */
	sliceCoordinates: function(dispX, dispY) {
		return {
			x: dispX / this._sliceImage.width() * this._width,
			y: dispY / this._sliceImage.height() * this._height
		};
	}
};

SliceManager.ERROR_IMAGE = new Image();
SliceManager.ERROR_IMAGE.src = DICOMViewer.APP_ROOT + "/images/viewer-missing-image.png";

SliceManager.Canvas = function(viewer, container, width, height, depth, aspectRatio, callback, colorizer) {
	SliceManager.apply(this, arguments);
	this._colorizer = colorizer;
	this._zooming = false;
	this._zoomTick = (function(me){
		return function() {
			me._zoom += 0.25;
			me._nextFrame += SliceManager.ZOOM_TIMEOUT;
			if (me._zoom > SliceManager.MAX_ZOOM)
				me._zoom = SliceManager.MAX_ZOOM;
			me._redrawSlice();
			if (me._zoom < SliceManager.MAX_ZOOM) {
				setTimeout(me._zoomTick, me.nextFrame - new Date().getTime());
			} else {
				me._zooming = false;
			}
		};
	})(this);
};

SliceManager.Canvas.prototype = new SliceManager();

SliceManager.Canvas.prototype.init = function(callback) {
	console.log("Using canvas viewer...");
	// Create our canvas
	this._canvas = document.createElement('canvas');
	this.createClickListener(this._canvas);
	this._canvasJQ = $(this._canvas);
	this._container.append(this._canvasJQ);
	// Also create an offscreen canvas for synthesizing other stuff
	this._offscreenCanvas = document.createElement('canvas');
	this.loadSlices(callback);
	this.resized();
};

SliceManager.Canvas.prototype.showSlice = function(type, slice, sliding) {
	if (type == 'x') {
		// Need the offscreen canvas for this - synthesize the slice, then
		// display it. X slices show z,y slices.
		var ctx = this._offscreenCanvas.getContext('2d');
		this._offscreenCanvas.width = this._depth;
		this._offscreenCanvas.height = this._height;
		for (var x = 0; x < this._depth; x++) {
			ctx.drawImage(this.slices[x], slice, 0, 1, this._height, x, 0, 1, this._height);
		}
		this._canvas.width = this._depth;
		this._canvas.height = this._height;
		var context = this._canvas.getContext('2d');
		context.drawImage(this._offscreenCanvas, 0, 0);
	} else if (type == 'y') {
		// Need the offscreen canvas for this - synthesize the slice, then
		// display it. Y slices show z,x slices.
		var ctx = this._offscreenCanvas.getContext('2d');
		this._offscreenCanvas.width = this._width;
		this._offscreenCanvas.height = this._depth;
		for (var y = 0; y < this._depth; y++) {
			ctx.drawImage(this.slices[y], 0, slice, this._width, 1, 0, y, this._width, 1);
		}
		this._drawSlice(this._offscreenCanvas, this._width, this._depth);
	} else if (type == 'z') {
		this._slice = slice;
		this._drawSlice(this.slices[slice], this._width, this._height);
	}
};

SliceManager.Canvas.prototype.resized = function() {
	// First, hide the canvas
	this._canvasJQ.css('display', 'none');
	var size = this.getSizeForAspectRatio();
	console.log("Wanted aspect ratio: " + this.aspectRatio + "; calculated aspect ratio: " + (size.width / size.height));
	// Use them
	this._canvas.width = size.width;
	this._canvas.height = size.height;
	// Reshow the canvas
	this._canvasJQ.css('display', 'block');
	// And draw
	this._redrawSlice();
};

SliceManager.Canvas.prototype._drawSlice = function(image, width, height) {
	this._currentSlice = image;
	this._redrawSlice();
};

SliceManager.Canvas.prototype._redrawSlice = function() {
	if (this._currentSlice) {
		var context;
		if (this._colorizer) {
			// colorize the current slice, whatever it is
			// This may require the offscreen canvas
			this._offscreenCanvas.width = this._currentSlice.width;
			this._offscreenCanvas.height = this._currentSlice.height;
			var ctx = this._offscreenCanvas.getContext('2d');
			ctx.drawImage(this._currentSlice, 0, 0);
			var imageData = ctx.getImageData(0, 0, this._offscreenCanvas.width, this._offscreenCanvas.height);
			var o, w = imageData.width, h = imageData.height, data = imageData.data, colorizer = this._colorizer;
			var max = w * h * 4;
			var start = new Date();
			for (o = 0; o < max; o += 4) {
				colorizer(data, o);
			}
			console.log("Colorize took " + (((new Date()).getTime()) - start.getTime()) + "ms");
			ctx.putImageData(imageData, 0, 0);
			context = this._canvas.getContext('2d');
			context.drawImage(this._offscreenCanvas, 0, 0, this._canvas.width, this._canvas.height);
		} else {
			context = this._canvas.getContext('2d');
			context.drawImage(this._currentSlice, 0, 0, this._canvas.width, this._canvas.height);
		}
		// Now that it's drawn, drawn any hard exudates on top of it
		context.lineWidth = 2;
		var r, x, y, width, height;
		for (var i = 0; i < this.hardExudates.length; i++) {
			if (this.hardExudates[i].isInLayer(this._slice)) {
				context.strokeStyle = this.hardExudates[i] == this.selectedHardExudate ? 'rgb(255,255,0)' : 'rgb(255,0,0)';
				r = this.hardExudates[i].getBoundingBox();
				x =      (r.x      / this._width ) * this._canvas.width;
				width =  (r.width  / this._width ) * this._canvas.width;
				y =      (r.y      / this._height) * this._canvas.height;
				height = (r.height / this._height) * this._canvas.height;
				/* Debugging crap verifying that the calculated aspect ratio
				 * is what it's supposed to be (within a margin of error due to
				 * the rounding of the final canvas size)
				console.log("Drawing hard exudate " + r + " at (" + x + ", " +
						y + "), [" + width + " x " + height + "]");
				var ar = r.width / r.height;
				console.log("Original AR: " + ar + ", when corrected: " + (r.width/r.height)*(this._height/this._width)*this.aspectRatio);
				console.log("Calculated: " + width/height);*/
				context.strokeRect(x, y, width, height);
			}
		}
		if (this.selectedHardExudate != null && this.selectedHardExudate.isInLayer(this._slice)) {
			this._zoomHardExudate(context, this.selectedHardExudate);
		}
	}
};

/**
 * Renders the zoomed in portion.
 * FIXME: This should really be rendering into a new canvas on top of the old
 * one so that we're not forced to clip to the original.
 */
SliceManager.Canvas.prototype._zoomHardExudate = function(context, exudate) {
	// Render a zoomed-in view
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
	bb.x = bb.x / this._width * this._canvas.width;
	bb.y = bb.y / this._height * this._canvas.height;
	bb.width = bb.width / this._width * this._canvas.width;
	bb.height = bb.height / this._height * this._canvas.height;
	// Initially position this off above and to the left of the source
	zoomX = bb.x + (bb.width/2) - (zoomWidth/2) - 2;// + bb.width - zoomWidth - 2;
	zoomY = bb.y + (bb.height/2) - (zoomHeight/2) - 2;// - zoomHeight - 2;
	if (zoomX < 0) {
		zoomX = 0;
	}
	if (zoomX + zoomWidth > this._canvas.width) {
		// Try and place it left
		zoomX = this._canvas.width - zoomWidth;
		if (zoomX < 0) {
			// Fine, place the x such that we're centered as best as possible
			zoomX = (zoomWidth - this._canvas.width/2);
		}
	}
	if (zoomY < 0) {
		// Drop it below
		zoomY = 0;
	}
	if (zoomY + zoomHeight > this._canvas.height) {
		zoomX = this._canvas.height - zoomHeight;
		if (zoomY < 0) {
			// Fine, place the x such that we're centered as best as possible
			zoomY = (zoomHeight - this._canvas.height/2);
		}
	}
	// And floor the x/y coordinates
	zoomX = Math.floor(zoomX);
	zoomY = Math.floor(zoomY);
	//console.log("Drawing source " + imageBB + " to (" + zoomX + ", " + zoomY + ") [" + zoomWidth + " x " + zoomHeight + "]");
	context.drawImage(this._currentSlice, imageBB.x, imageBB.y, imageBB.width, imageBB.height, zoomX, zoomY, zoomWidth, zoomHeight);
	context.strokeStyle = 'rgb(255,255,0)';
	context.lineWidth = 2;
	context.strokeRect(zoomX - 1, zoomY - 1, zoomWidth+2, zoomHeight+2);
	if (SliceManager.ZOOM_CONTEXT > 0) {
		// If we're showing context, highlight the correct area
		context.strokeStyle = 'rgb(255,0,0)';
		context.lineWidth = 1;
		context.strokeRect(zoomX + contextX, zoomY + contextY, zoomWidth - (contextX*2), zoomHeight - (contextY*2));
	}
};

SliceManager.Canvas.prototype.selectHardExudate = function(hardExudate) {
	//SliceManager.prototype.selectHardExudate.call(this, arguments);
	this.selectedHardExudate = hardExudate;
	this._zoom = 1;
	if (!this._zooming) {
		this._zooming = true;
		this._nextFrame = new Date().getTime() + SliceManager.ZOOM_TIMEOUT;
		setTimeout(this._zoomTick, 50);
	}
	this._redrawSlice();
}

SliceManager.Canvas.prototype.sliceCoordinates = function(dispX, dispY) {
	return {
		x: dispX / this._canvas.width * this._width,
		y: dispY / this._canvas.height * this._height
	};
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
	var canvas = document.createElement('canvas');
	if (canvas.getContext && canvas.getContext('2d')) {
		return new SliceManager.Canvas(viewer, container, width, height, depth, aspectRatio, callback, colorizer);
	} else {
		return new SliceManager(viewer, container, width, height, depth, aspectRatio, callback);
	}
};

function Colorizer(colors) {
	if (colors.length < 2)
		throw Error("Must have at least two colors");
	// Take the colors array, and "normalize" it
	for (var i = 0; i < colors.length; i++) {
		if ('length' in colors[i]) {
			colors[i] = { color: colors[i] };
		}
		// Used to enforce a stable sort
		colors[i]._index = i;
	}
	colors.sort(function (a, b) {
		if ('value' in a && 'value' in b) {
			return a.value - b.value;
		} else {
			return a._index - b._index;
		}
	});
	var c = colors[0];
	if ('value' in c && c.value > 0) {
		// Prepend a new color value for 0...
		colors.unshift({value: 0, color: c.color});
	} else {
		c.value = 0;
	}
	c = colors[colors.length-1];
	if ('value' in c && c.value < 255) {
		// Add a new color for 255...
		colors.push({value: 255, color: c.color});
	} else {
		c.value = 255;
	}
	// Now go through and add in any missing values using linear interpolation
	var start = 0;
	for (var i = 0; i < colors.length; i++) {
		if ('value' in colors[i]) {
			if (start + 1 < i) {
				// Set all values between start and this
				var value = colors[start].value;
				var interval = (colors[i].value - value) / (i - start + 1);
				start++;
				for (; start < i; start++) {
					value += interval;
					colors[start].value = value;
				}
			}
			start = i;
		}
	}
	// And, finally, create our palette:
	var palette = new Array(256);
	for (var i = 1; i < colors.length; i++) {
		var r1 = colors[i-1].color[0],
			g1 = colors[i-1].color[1],
			b1 = colors[i-1].color[2],
			r2 = colors[i].color[0],
			g2 = colors[i].color[1],
			b2 = colors[i].color[2];
		var a = 1;
		var deltaA = 1 / (colors[i].value - colors[i-1].value);
		for (var p = Math.floor(colors[i-1].value); p <= colors[i].value; p++) {
			palette[p] = [
				Math.floor(r1 * a + r2 * (1-a)),
				Math.floor(g1 * a + g2 * (1-a)),
				Math.floor(b1 * a + b2 * (1-a))
			];
			a -= deltaA;
		}
	}
	return Colorizer.Palette(palette);
}

Colorizer.Palette = function(palette) {
	if (palette.length != 256)
		throw Error("Palette must contain 256 entries");
	return function(data, o) {
		var gray = Math.floor((data[o]+data[o+1]+data[o+2])/3);
		data[o] = palette[gray][0];
		data[o+1] = palette[gray][1];
		data[o+2] = palette[gray][2];
	};
};

Colorizer['JET'] = Colorizer([
	[ 0, 0, 128 ],
	[ 0, 0, 255 ],
	[ 0, 255, 255 ],
	[ 0, 255, 0 ],
	[ 255, 255, 0 ],
	[ 255, 0, 0 ],
	[ 128, 0, 0 ]
]);

Colorizer['LAYERS'] = Colorizer([
	[ 0, 0, 0 ],
	{ value: 209, color: [ 209, 209, 209 ]},
	{ value: 210, color: [ 0, 0, 255 ]},
	[0, 0, 255]
]);