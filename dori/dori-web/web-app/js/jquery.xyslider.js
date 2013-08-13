/*
 * Custom plugin to implement the slider over the fundus photo in the scan
 * viewer.
 * 
 * The "default" styles for this widget would be:
 * 
 * .ui-eyesfirst-slider {
 *     position: relative;
 * }
 * .ui-eyesfirst-slider-handle-x {
 *     left: 0%;
 *     width: 1px;
 *     height: 100%;
 * }
 * .ui-eyesfirst-slider-handle-y {
 *     top: 0%;
 *     width: 100%;
 *     height: 1px;
 * }
 * .ui-eyesfirst-slider-handle {
 *     position: absolute;
 *     background-color: #F00;
 * }
 * .ui-eyesfirst-slider-handle.ui-state-active {
 *     background-color: #FF0;
 * }
 */
(function($) {
$.widget("eyesfirst.xyslider", $.ui.mouse, {
	widgetEventPrefix: "xyslide",
	options: {
		xmin: 0,
		xmax: 100,
		xvalue: 50,
		xstep: 1,
		ymin: 0,
		ymax: 100,
		yvalue: 50,
		ystep: 1,
		/**
		 * The number of pixels away from a handle to "snap" to that handle and
		 * that handle alone.
		 */
		handlesnap: 5,
		/**
		 * Maximum distance for the mouse to travel before the mouse plugin
		 * sends drag and stop events - make it 0 to ensure single clicks are
		 * handled correctly.
		 */
		distance: 0
	},
	_create: function() {
		this._mouseInit();
		// The slider consists of two child DIVs place above the parent DIV
		this._xslider = $('<div/>').addClass('ui-eyesfirst-slider-handle ui-eyesfirst-slider-handle-x');
		this._yslider = $('<div/>').addClass('ui-eyesfirst-slider-handle ui-eyesfirst-slider-handle-y');
		this.element.addClass('ui-eyesfirst-slider');
		this.element.append(this._xslider, this._yslider);
		this._updatePositions();
	},

	_mouseCapture: function(event) {
		this.elementOffset = this.element.offset();
		this.elementSize = {
			width: this.element.outerWidth(),
			height: this.element.outerHeight()
		};
		// Determine which slider/sliders we're moving
		var x = event.pageX - this.elementOffset.left;
		var y = event.pageY - this.elementOffset.top;
		var sliders = this._sliderPixelPositions();
		this._slidingX = Math.abs(sliders.x - x) < this.options.handlesnap;
		this._slidingY = Math.abs(sliders.y - y) < this.options.handlesnap;
		if (!(this._slidingX || this._slidingY)) {
			// If we're not "snapped" to either slider, jump both of them
			this._slidingX = this._slidingY = true;
		}
		if (this._slidingX)
			this._xslider.addClass("ui-state-active");
		if (this._slidingY)
			this._yslider.addClass("ui-state-active");
		// Do the slide
		this._slide(event);
		return true;
	},

	_mouseStart: function(event) {
		return true;
	},

	_mouseDrag: function(event) {
		this._slide(event);
		return false;
	},

	_mouseStop: function(event) {
		if (this._slidingX)
			this._xslider.removeClass("ui-state-active");
		if (this._slidingY)
			this._yslider.removeClass("ui-state-active");
		this._slidingX = this.slidingY = false;
		this._change(event);
		return false;
	},

	_slide: function(event, fire) {
		if (this._slidingX) {
			var x = Math.max(0, Math.min(event.pageX - this.elementOffset.left, this.elementSize.width));
			this.options.xvalue = this._normalizeX((x / this.elementSize.width) * (this.options.xmax - this.options.xmin) + this.options.xmin);
		}
		if (this._slidingY) {
			var y = Math.max(0, Math.min(event.pageY - this.elementOffset.top, this.elementSize.height));
			this.options.yvalue = this._normalizeY(this.options.ymax - ((y / this.elementSize.height) * (this.options.ymax - this.options.ymin)));
		}
		this._updatePositions();
		this._trigger("slide", event, {
			xvalue: this.options.xvalue,
			yvalue: this.options.yvalue,
			slidingX: this._slidingX,
			slidinyY: this._slidingY
		});
	},

	_change: function(event) {
		this._trigger("change", event, {
			xvalue: this.options.xvalue,
			yvalue: this.options.yvalue
		});
	},

	_normalize: function(x, min, max, step) {
		if (x < min)
			return min;
		if (x > max)
			return max;
		var modStep = (x - min) % step;
		var aligned = x - modStep;
		if (Math.abs(modStep) * 2 >= step) {
			// Round off
			aligned += (modStep < 0 ? (-step) : step);
		}
		// This is based on jQuery UI's slider
		aligned = parseFloat(aligned.toFixed(5));
		return aligned;
	},
	_normalizeX: function(x) {
		return this._normalize(x, this.options.xmin, this.options.xmax, this.options.xstep);
	},
	_normalizeY: function(y) {
		return this._normalize(y, this.options.ymin, this.options.ymax, this.options.ystep);
	},

	_setOption: function(key, value) {
		switch (key) {
		case 'xvalue':
			this.options.xvalue = this._normalizeX(value);
			this._updatePositions();
			this._change(null);
			break;
		case 'yvalue':
			this.options.yvalue = this._normalizeY(value);
			this._updatePositions();
			this._change(null);
			break;
		}
		$.Widget.prototype._setOption.apply( this, arguments );
	},

	_updatePositions: function() {
		this._xslider.css('left', ((this.options.xvalue - this.options.xmin) / (this.options.xmax - this.options.xmin)) * 100 + '%');
		this._yslider.css('bottom', ((this.options.yvalue - this.options.ymin) / (this.options.ymax - this.options.ymin)) * 100 + '%');
	},
	_sliderPixelPositions: function() {
		return {
			x: this.elementSize.width * ((this.options.xvalue - this.options.xmin) / (this.options.xmax - this.options.xmin)),
			y: this.elementSize.height * (1 - ((this.options.yvalue - this.options.ymin) / (this.options.ymax - this.options.ymin)))
		};
	},
	destroy: function() {
		this._xslider.remove();
		this._yslider.remove();
		this._mouseDestroy();
		return $.Widget.prototype.destroy.call( this );
	}
});

})(jQuery);