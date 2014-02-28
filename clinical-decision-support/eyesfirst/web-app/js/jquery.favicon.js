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
 *
 * Quick little plugin to show loading status in the favicon area.
 *
 * Requires canvas.
 */

(function($) {
	var animate_interval = false;
	var canvas = null;
	var percent = 0;
	var ignorePercentChange = false;
	var resetURL = null;
	var resetMime = null;
	var spinner = 0;

	function setURL(url, mimeType) {
		// Kill any existing favicons
		var toRemove = [];
		$('link').each(function() {
			if ($(this).attr('rel').toUpperCase() == 'SHORTCUT ICON') {
				toRemove.push(this);
			}
		});
		// Create the new one:
		var favicon = $('<link rel="shortcut icon"/>');
		if (mimeType) {
			favicon.attr('type', mimeType);
		}
		favicon.attr('href', url);
		// Remove all the old ones
		for (var i = 0; i < toRemove.length; i++) {
			$(toRemove[i]).remove();
		}
		// Append the new one
		$('head').append(favicon);
	}

	function repaintFavicon() {
		if (canvas) {
			setURL(canvas.toDataURL());
		}
	}

	function getCanvas() {
		canvas = document.createElement('canvas');
		if (!('getContext' in canvas)) {
			canvas = null;
			// No canvas support
			getCanvas = function() { return null; };
		} else {
			canvas.width = 16;
			canvas.height = 16;
			getCanvas = function() { return canvas; };
		}
		return canvas;
	}

	var percentStartingAngle = -Math.PI / 2;

	var percentPaintInterval = 100;
	var lastPercentChange = 0;

	/**
	 * Internal function that paints the current percent as set by favicon('percent', value);
	 */
	function paintPercent() {
		// In order to avoid spamming paint events, enforce a minimum timeout
		// between actually painting.
		if (ignorePercentChange) {
			// well within the interval, abort immediately
			return;
		}
		var now = new Date().getTime();
		// Check to see if this request is too soon
		if (now - lastPercentChange < percentPaintInterval) {
			// It is - ignore this request, and set a timeout after which we'll
			// repaint to update to whatever this is.
			ignorePercentChange = setTimeout(function() {
				ignorePercentChange = false;
				paintPercent();
			}, percentPaintInterval);
			return;
		}
		lastPercentChange = now;
		if (!getCanvas()) {
			return;
		}
		var context = canvas.getContext('2d');
		context.clearRect(0, 0, canvas.width, canvas.height);
		context.fillStyle = 'rgb(128, 128, 128)';
		context.beginPath();
		context.moveTo(8, 8);
		context.lineTo(8, 1);
		context.arc(8, 8, 7, percentStartingAngle, percentStartingAngle + Math.PI * 2 * percent / 100);
		context.lineTo(8, 8);
		context.fill();
		context.beginPath();
		context.strokeStyle = 'rgb(128, 128, 128)';
		context.lineWidth = 2;
		context.arc(8, 8, 7, 0, Math.PI*2);
		context.stroke();
		repaintFavicon();
	}

	function startSpinner() {
		if (!animate_interval) {
			spinner = 0;
			if (getCanvas()) {
				drawSpinner();
				animate_interval = setInterval(drawSpinner, 100);
			}
		}
	}

	function stopSpinner() {
		if (animate_interval) {
			clearInterval(animate_interval);
			animate_interval = false;
		}
	}

	var spinnerRotateIncrement = Math.PI / 6;
	var spinnerColor = 'rgba(0,0,0,';

	function drawSpinner() {
		if (!getCanvas())
			return;
		var context = canvas.getContext('2d');
		context.clearRect(0, 0, canvas.width, canvas.height);
		context.save();
		// This is fairly simple: draw little lines around in a circle
		context.lineCap = 'round';
		context.lineWidth = 2;
		context.translate(8, 8);
		context.rotate(spinnerRotateIncrement * spinner);
		var alpha = 0.2;
		for (var i = 0; i < 12; i++) {
			context.strokeStyle = spinnerColor + alpha + ')';
			context.beginPath();
			context.moveTo(0, 5);
			context.lineTo(0, 7);
			context.stroke();
			context.rotate(spinnerRotateIncrement);
			if (i >= 6) {
				alpha += 0.1;
			}
		}
		spinner++;
		if (spinner >= 12)
			spinner = 0;
		context.restore();
		repaintFavicon();
	}

	function findFavIcon() {
		var result = null;
		$('link').each(function() {
			if ($(this).attr('rel').toUpperCase() == 'SHORTCUT ICON') {
				result = this;
				return false;
			}
		});
		// Allow null, since that will create effectively an empty result set,
		// which is basically what we want.
		return $(result);
	}

	function removeAllFavicons() {
		var toRemove = [];
		$('link').each(function() {
			if ($(this).attr('rel').toUpperCase() == 'SHORTCUT ICON') {
				toRemove.push(this);
			}
		});
		for (var i = 0; i < toRemove.length; i++) {
			$(toRemove[i]).remove();
		}
	}

	function findFavIconURL() {
		var fi = findFavIcon();
		var res = null;
		if (fi != null) {
			res = fi.attr('href');
		}
		return res == null ? 'favicon.ico' : res;
	}

	function clearTimeouts() {
		stopSpinner();
		if (ignorePercentChange)
			clearTimeout(ignorePercentChange);
	}

	$['favicon'] = function(option, value, mimeType) {
		if (resetURL == null) {
			var fi = findFavIcon();
			resetURL = fi.attr('href');
			if (resetURL == null)
				resetURL = 'favicon.ico';
			resetMime = fi.attr('type');
		}
		if (arguments.length == 0) {
			// No options, return the favicon
			return findFavIcon();
		} else if (option == 'url') {
			if (argument.length == 1) {
				return findFavIconURL();
			} else {
				clearTimeouts();
				setURL(value, mimeType);
			}
		} else if (option == 'percent') {
			if (arguments.length == 1) {
				return percent;
			} else {
				if (typeof value != 'number') {
					// coerce as best as possible
					value = Number(value);
				}
				var lastPercent = percent;
				if (value >= 0 && value <= 100) {
					percent = value;
				} else if (value > 100) {
					percent = 100;
				} else {
					percent = 0;
				}
				stopSpinner();
				paintPercent();
			}
		} else if (option == 'spinner') {
			if (arguments.length == 1)
				return animate_interval != false;
			if (value) {
				startSpinner();
			} else {
				clearTimeouts();
			}
		} else if (option == 'remove') {
			clearTimeouts();
			// Remove all favicons
			removeAllFavicons();
		} else if (option == 'reset') {
			clearTimeouts();
			setURL(resetURL, resetMime);
		}
		// If we've fallen through, we have no special return value, so return
		// the jQuery object itself.
		return $;
	};
})(jQuery);