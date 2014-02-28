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
 * Utility library for dealing with dragging an element.
 */

(function($) {
	$.fn.mousedrag = function(draghandler, starthandler, stophandler) {
		if (arguments.length == 1) {
			if (typeof draghandler == 'object') {
				// Grab handlers out of it.
				var o = draghandler;
				draghandler = o['drag'];
				starthandler = 'dragstart' in o ? o['dragstart'] : function() { return true; };
				stophandler = 'dragstop' in o ? o['dragstop'] : function() { };
			} else if (typeof draghandler == 'function') {
				// Generate handlers
				starthandler = (function(handler) {
					return function(event) {
						handler('dragstart', event);
					}
				})(draghandler);
				stophandler = (function(handler) {
					return function(event) {
						handler('dragstop', event);
					}
				})(draghandler);
				draghandler = (function(handler) {
					return function(event) {
						handler('drag', event);
					}
				})(draghandler);
			} else {
				throw Error("Bad handler object");
			}
		}
		return this.each(function() {
			(function(me) {
				// We need to keep the "me" so that the "this" given to the
				// event handler makes sense as the actual target, rather than
				// the document which will become the target for the drag handler.
				$(me).mousedown(function(event) {
					// Maybe start a drag.
					//console.log("jqd: start drag");
					if (starthandler.call(me, event)) {
						// Start the drag!
						function mouseup(event) {
							$(document).unbind('mouseup', mouseup);
							$(document).unbind('mousemove', mousedrag);
							//console.log("jqd: stop drag");
							stophandler.call(me, event);
						}
						function mousedrag(event) {
							// It's possible that we missed a mouseup event (due to
							// various browser stupidity)
							if ('buttons' in event && event.buttons == 0) {
								// No button is currently down, so treat this like a
								// mouse-up event.
								return mouseup(event);
							}
							draghandler.call(me, event);
							event.preventDefault();
							//console.log("jqd: drag");
							return false;
						}
						//console.log("jqd: starting drag (startdrag returned true)");
						$(document).bind('mouseup', mouseup);
						$(document).bind('mousemove', mousedrag);
						event.preventDefault();
					} else {
						//console.log("jqd: doing nothing (startdrag returned false)");
					}
				});
			})(this);
		});
	};
})(jQuery);