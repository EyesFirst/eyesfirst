/*
 * Just the canvas slice manager.
 */

SliceManager.Canvas = function(viewer, container, width, height, depth, aspectRatio, callback, canvas, colorizer) {
	this._canvas = canvas;
	SliceManager.apply(this, arguments);
	this._colorizer = colorizer;
	this.verticalMarkerX = -1;
};

SliceManager.Canvas.prototype = new SliceManager();

SliceManager.Canvas.prototype.init = function(callback) {
	console.log("Using canvas viewer...");
	this.createMouseListeners(this._canvas);
	this._canvasJQ = $(this._canvas);
	this._container.append(this._canvasJQ);
	// Also create an offscreen canvas for synthesizing other stuff
	this._offscreenCanvas = document.createElement('canvas');
	this.loadSlices(callback);
	this.resized();
};

SliceManager.Canvas.prototype.showSlice = function(slice, sliding) {
	this._slice = slice;
	this._drawSlice(this.slices[slice], this._width, this._height);
};

SliceManager.Canvas.prototype.resized = function() {
	// First, hide the canvas
	this._canvasJQ.css('display', 'none');
	var size = this.getCorrectedSize();
	//console.log(size);
	//console.log("Wanted aspect ratio: " + this.aspectRatio + "; calculated aspect ratio: " + (size.width / size.height));
	// Use them
	this._canvas.width = size.width;
	this._canvas.height = size.height;
	// Reshow the canvas
	this._canvasJQ.css('display', 'block');
	// And draw
	this.redraw();
};

SliceManager.Canvas.prototype.getDisplaySize = function() {
	return {
		'width': this._canvas.width,
		'height': this._canvas.height
	};
};

/**
 * Draws the given slice, applying the current colorizer to it and saving it in
 * the offscreen canvas, if necessary.
 */
SliceManager.Canvas.prototype._drawSlice = function(image) {
	this._currentSlice = image;
	this.redraw();
};

SliceManager.Canvas.prototype.redraw = function() {
	if (this._currentSlice) {
		var context;
		if (this._colorizer) {
			// colorize the current slice, whatever it is
			// This may require the offscreen canvas
			this._offscreenCanvas.width = this._currentSlice.width;
			this._offscreenCanvas.height = this._currentSlice.height;
			var ctx = this._offscreenCanvas.getContext('2d');
			ctx.clearRect(0, 0, this._offscreenCanvas.width, this._offscreenCanvas.height);
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
		}
		this._redraw();
	}
};

SliceManager.Canvas.prototype._redraw = function() {
	context = this._canvas.getContext('2d');
	context.clearRect(0, 0, this._canvas.width, this._canvas.height);
	context.mozImageSmoothingEnabled = true;
	context.drawImage(this._colorizer ? this._offscreenCanvas : this._currentSlice, 0, 0, this._canvas.width, this._canvas.height);
	// Now that it's drawn, drawn any hard exudates on top of it
	context.lineWidth = 2;
	this._context = context;
	this.drawOverlays();
	if (this.verticalMarkerX >= 0) {
		var x = (this.verticalMarkerX / this._width) * this._canvas.width;
		context.strokeStyle = 'rgb(255,0,0)';
		context.lineWidth = 1;
		context.strokeRect(x, 0, 0, this._canvas.height);
	}
	this._context = null;
};

SliceManager.Canvas.prototype.drawBox = function(color, borderWidth, x, y, width, height) {
	this._context.strokeStyle = color;
	this._context.lineWidth = borderWidth;
	this._context.strokeRect(x, y, width, height);
},

SliceManager.Canvas.prototype.initHardExudatePopup = function(popup) {
	popup.append(this._hardExudatePopupCanvas = document.createElement('canvas'));
};

SliceManager.Canvas.prototype.drawHardExudatePopup = function(exudate,
		sliceX, sliceY, sliceWidth, sliceHeight,
		contextHorizontal, contextVertical,
		displayWidth, displayHeight) {
	var zoomX = displayWidth / sliceWidth;
	var zoomY = displayHeight / sliceHeight;
	this._hardExudatePopupCanvas.width = displayWidth;
	this._hardExudatePopupCanvas.height = displayHeight;
	var context = this._hardExudatePopupCanvas.getContext('2d');
	context.mozImageSmoothingEnabled = false;
	var slice = this._colorizer ? this._offscreenCanvas : this._currentSlice;
	context.drawImage(slice, sliceX, sliceY, sliceWidth, sliceHeight, 0, 0, displayWidth, displayHeight);
	context.strokeStyle = this.theme.MACHINE_TAG_CONTEXT;
	context.lineWidth = 1;
	context.strokeRect(Math.floor(contextHorizontal * zoomX)-0.5, Math.floor(contextVertical * zoomY)-0.5,
			Math.floor((sliceWidth - contextHorizontal*2) * zoomX), Math.floor((sliceHeight - contextVertical*2) * zoomY));
}

SliceManager.Canvas.prototype.setColorizer = function(colorizer) {
	this._colorizer = colorizer;
	this.redraw();
};

SliceManager.Canvas.prototype.canColorize = function() {
	return true;
};

SliceManager.Canvas.prototype.setVerticalMarker = function(x) {
	this.verticalMarkerX = x;
	this._redraw();
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

/**
 * Creates a colorizer based on adjusting the image contrast and brightness.
 * Contrast and brightness adjustment algorithm taken from the Wikipedia,
 * which is based on the GIMP algorithm.
 */
Colorizer.Contrast = function(contrast, brightness) {
	var palette = new Array(256);
	if (brightness < 0.0) {
		for (var i = 0; i < 256; i++) {
			palette[i] = (i/255) * (1.0 + brightness);
		}
	} else {
		for (var i = 0; i < 256; i++) {
			var v = (i/255);
			palette[i] = v + ((1.0 - v) * brightness);
		}
	}
	for (var i = 0; i < 256; i++) {
		palette[i] = Math.floor(((palette[i] - 0.5) * Math.tan((contrast + 1) * Math.PI/4) + 0.5) * 255 + 0.5);
		//palette[i] = Math.floor(palette[i] * 255 + 0.5);
		if (palette[i] < 0)
			palette[i] = 0;
		if (palette[i] > 255)
			palette[i] = 255;
		var v = palette[i];
		palette[i] = [ v, v, v ];
	}
	return Colorizer.Palette(palette);
}

Colorizer.Palette = function(palette) {
	if (palette.length != 256)
		throw Error("Palette must contain 256 entries");
	return function(data, o) {
		// | 0 is ever so slightly faster than Math.floor(), at least in
		// Firefox where it actually matters.
		var gray = ((data[o]+data[o+1]+data[o+2])/3) | 0;
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