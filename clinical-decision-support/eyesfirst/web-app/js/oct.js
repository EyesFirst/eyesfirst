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

/**
 * Deal with loading OCT slices.
 */

/**
 * @namespace
 */
function OCT() { }

/**
 * The slice manager is responsible for generating images that can be displayed
 * via the slice viewer.
 * @constructor Create a new slice manager.
 */
OCT.SliceManager = function(id, oncomplete, onerror, onprogress) {
	var me = this;
	if (arguments.length < 4) {
		onprogress = function() { };
	}
	if (arguments.length < 3) {
		onerror = function() { };
	}
	this.id = id;
	$.ajax({
		url: 'oct/' + encodeURIComponent(id) + '/info',
		dataType: 'json',
		error: function(jqXHR, textStatus, errorThrown) {
			if (jqXHR.status == 404) {
				onerror("Unable to load imagery: the requested object was not found.");
			} else {
				onerror("Unable to load imagery. Server said: " + jqXHR.status + " " + jqXHR.statusText);
			}
		},
		success: function(data, textStatus, jqXHR) {
			// Now that we have the metadata, we can start loading the slices
			me._load(data, oncomplete, onerror, onprogress);
		},
		//timeout: 60, // timeout
		type: 'GET'
	});
}

OCT.SliceManager.MAX_IMAGES_AT_ONCE = 5;

OCT.SliceManager.prototype = {
	getSliceCount: function() {
		return this.sliceCount;
	},
	getSlice: function(slice) {
		return this.slices[slice];
	},
	/**
	 * Gets the slice width, taking the aspect ratio into account, if the image
	 * is zoomed in fully.
	 */
	getSliceWidth: function() {
		return Math.round(this.actualWidth * this.scaleWidth);
	},
	getSliceHeight: function() {
		return Math.round(this.actualHeight * this.scaleHeight);
	},
	getThumbnailURL: function() {
		return 'oct/' + encodeURIComponent(this.id) + '/synthesizedFundus';
	},
	getThicknessMapURL: function() {
		return 'oct/' + encodeURIComponent(this.id) + '/thicknessMap';
	},
	getPixelPitch: function() {
		return this.pixelSpacing;
	},
	_load: function(metadata, oncomplete, onerror, onprogress) {
		if ('elements' in metadata) {
			var elements = metadata.elements;
			/* Don't bother sorting, we never display the metadata anyway.
			 * The sort dates back to when we did.
			// Sort tags by ???
			elements.sort(function (a,b) {
				// For now, go with the tag name
				return a.tagName.localeCompare(b.tagName);
			});
			*/
			this._metadata = elements;
			var me = this;
			// Grab the pixel pitch information out of the metadata, if it exists.
			elements.forEach(function(e) {
				if (e['tag'] == "5200:9229" /* SharedFunctionalGroupsSequence */) {
					// Within this, locate pixel measures sequence
					e['value'].forEach(function(e2) {
						if (e2['tag'] == "0028:9110" /*PixelMeasuresSequence*/) {
							// Go through the values and find PixelSpacing
							e2['value'].forEach(function(e3) {
								if (e3['tag'] == "0028:0030" /* PixelSpacing */) {
									// Pick whichever value is smaller, since we'll be stretching the other side
									me.pixelSpacing = Math.min.apply(null, e3['value']);
								}
							});
						}
					});
				}
			});
		}
		if ('rows' in metadata && 'columns' in metadata && 'frames' in metadata) {
			var ar = 1;
			if ('aspectRatio' in metadata) {
				ar = metadata.aspectRatio;
			}
			this.aspectRatio = ar;
			this.actualWidth = metadata.columns;
			this.actualHeight = metadata.rows;
			// We need to calculate the adjustment for the actual size
			var baseAR = this.actualWidth / this.actualHeight;
			// First, compute the scale to convert the image to a square...
			var sw, sh;
			if (this.actualWidth > this.actualHeight) {
				// The height would need to be increased, use that
				sw = 1;
				sh = baseAR;
			} else {
				// Otherwise, we'd increase the width
				sw = 1/baseAR;
				sh = 1;
			}
			// Then apply the actual aspect ratio to calculate the scales
			if (ar > 1) {
				// Wider than it is tall, so keep the height at 1 and increase
				// the width
				this.scaleWidth = sw * ar;
				this.scaleHeight = sh;
			} else {
				// Otherwise, do the opposite.
				this.scaleWidth = sw;
				this.scaleHeight = sh * (1 / ar);
			}
			// And use that to calculate a "zoomed in" width/height
			this.width = this.actualWidth * this.scaleWidth;
			this.height = this.actualHeight * this.scaleHeight;
			this.sliceCount = metadata.frames;
			// Start loading the slices
			(function(me) {
				me.slices = new Array(me.sliceCount);
				var prefix = 'oct/' + encodeURIComponent(me.id) + '/slice/';
				var i = 0;
				var totalLoaded = 0;
				function loadNext() {
					if (i < me.sliceCount) {
						var img = new Image();
						me.slices[i] = img;
						img.onload = onload;
						i++;
						img.src = prefix + i;
					}
				}
				function onload() {
					totalLoaded++;
					onprogress(totalLoaded / me.sliceCount);
					loadNext();
					// If we've loaded everything now, send the message
					if (i >= me.sliceCount && oncomplete) {
						// Make sure we always blank oncomplete so it doesn't
						// get sent multiple times
						var f = oncomplete;
						oncomplete = null;
						f();
					}
				}
				var l = Math.min(OCT.SliceManager.MAX_IMAGES_AT_ONCE, me.sliceCount);
				for (i; i < l;) {
					loadNext();
				}
			})(this);
		} else {
			console.log("Got bad response from server, not loading frame data.");
		}
	},
	loadClassifierResults: function(oncomplete, onerror) {
		$.ajax({
			url: 'oct/' + encodeURIComponent(this.id) + '/classifierResults',
			dataType: 'json',
			error: function(jqXHR, textStatus, errorThrown) {
				if (jqXHR.status == 404) {
					onerror("Unable to load imagery: the requested object was not found.");
				} else {
					onerror("Unable to load imagery. Server said: " + jqXHR.status + " " + jqXHR.statusText);
				}
			},
			success: function(data, textStatus, jqXHR) {
				oncomplete(data);
			},
			//timeout: 60, // timeout
			type: 'GET'
		});
	}
};

OCT.SliceSelector = function(container, slice, sliceCount) {
	this.canvas = document.createElement("canvas");
	this.context = this.canvas.getContext('2d');
	this.$canvas = $(this.canvas);
	this.$canvas.addClass("oct-slice-selector");
	(function($canvas, me) {
		$canvas.mousedrag({
			dragstart: function(event) {
				var x = event.pageX - $(this).offset().left;
				me.clicked(x);
				return true;
			},
			drag: function(event) {
				var x = event.pageX - $(this).offset().left;
				me.clicked(x);
			}
		});
	})(this.$canvas, this);
	container.append(this.$canvas);
	this.slice = slice;
	this.sliceCount = sliceCount;
	this.sliceHistogram = null;
	this.resized();
}

OCT.SliceSelector.prototype = {
	draw: function() {
		var ctx = this.context, w = this.canvas.width, h = this.canvas.height;
		// Clear the thing
		ctx.clearRect(0, 0, w, h);
		// Draw a shadow on the top
		var g = ctx.createLinearGradient(0, 0, 0, 8);
		g.addColorStop(0, "rgba(0,0,0,0.5)");
		g.addColorStop(1, "rgba(0,0,0,0)");
		ctx.fillStyle = g;//"rgb(255,0,255)";
		ctx.fillRect(0, 0, w, 8);
		var sliceWidth = w / this.sliceCount;
		if (this.sliceHistogram) {
			var barWidth = sliceWidth / 2;
			var sx = barWidth / 2;
			ctx.fillStyle = "rgb(246, 97, 48)";
			for (var i = 0; i < this.sliceCount; i++) {
				if (this.sliceHistogram[i] > 0) {
					var sh = h * this.sliceHistogram[i];
					ctx.fillRect(sx + i*sliceWidth, h - sh, barWidth, sh);
				}
			}
		}
		// Draw the current slice
		ctx.fillStyle = "rgba(255, 255, 255, 0.5)";
		ctx.fillRect(sliceWidth * this.slice, 0, sliceWidth, h);
	},
	clicked: function(x) {
		var newSlice = Math.floor(x * this.sliceCount / this.$canvas.width());
		// Bound the slice to the allowable range
		if (newSlice < 0)
			newSlice = 0;
		if (newSlice >= this.sliceCount)
			newSlice = this.sliceCount - 1;
		if (newSlice != this.slice) {
			this.slice = newSlice;
			this.draw();
			this.onslicechange(newSlice);
		}
	},
	setSlice: function(slice) {
		if (slice != this.slice) {
			this.slice = slice;
			this.draw();
		}
	},
	resized: function() {
		// Make the canvas width/height match the actual width/height
		this.canvas.width = this.$canvas.width();
		this.canvas.height = this.$canvas.height();
		this.draw();
	},
	setHardExudates: function(hardExudates) {
		this.sliceHistogram = new Array(this.sliceCount);
		for (var i = 0; i < this.sliceCount; i++) {
			this.sliceHistogram[i] = 0;
		}
		// Go through the hard exudates and mark any slices
		for (var i = 0; i < hardExudates.length; i++) {
			var box = hardExudates[i]["boundingBox"];
			var z = box["z"];
			var mz = Math.min(z + box["depth"], this.sliceCount);
			for (; z < mz; z++) {
				this.sliceHistogram[z]++;
			}
		}
		var max = Math.max.apply(null, this.sliceHistogram);
		for (var i = 0; i < this.sliceCount; i++) {
			this.sliceHistogram[i] /= max;
		}
		this.draw();
	}
};