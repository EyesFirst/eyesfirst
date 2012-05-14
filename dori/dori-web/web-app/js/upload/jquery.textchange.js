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
 * Little jQuery plugin for dealing with text changing in a text field.
 * This was written explicitly for the uploader.
 */

(function($){
	jQuery.fn.textchange = function(callback, timeout, immediate) {
		if (arguments.length < 2)
			timeout = 500;
		this.each(function() {
			var tid = false;
			var me = this;
			var last = $(this).prop('value');
			function handler() {
				var value = $(me).prop('value');
				if (value == last)
					return;
				if (immediate)
					immediate(me);
				if (tid) {
					clearTimeout(tid);
				}
				last = value;
				tid = setTimeout(function() {
					tid = false;
					callback(me);
				}, timeout);
			}
			// Keydown should be obvious: text entry.
			$(this).keydown(handler);
			var focusInterval = false;
			// Bind to focus to start an interval where we just poll to see if
			// it's changed (for dealing with pasting from the context menu
			// or things like that)
			$(this).focus(function() {
				focusInterval = setInterval(handler, 500);
			});
			// And undo on blur
			$(this).blur(function() {
				if (focusInterval)
					clearInterval(focusInterval);
				focusInterval = false;
			});
			// Also bind to change, for instances where the change happens that
			// we just flat-out miss. At least it will happen eventually.
			$(this).change(handler);
		});
	};
})(jQuery);