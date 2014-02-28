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
 * Library for dealing with popups
 */

function Popup(msg, left, top) {
	"use strict";
	this.$popup = $('<div/>').addClass("popup").html(msg).css({
		'position': 'absolute', 'top': top, 'left': left
	});
	$("body").append(this.$popup);
	// Really what we should do is check for the mouse to leave the area where
	// the popup is and hide then. But instead, let's just set a five second
	// timer.
	var me = this;
	setTimeout(function() {
		me.hide();
	}, 5000);
	this.visible = true;
}

Popup.prototype = {
	isVisible: function() {
		return this.visible;
	},
	hide: function() {
		// See if the browser support animation
		if (Animation.supported) {
			Animation.addAnimationEndEvent(this.$popup.get(0), (function(me){
				return function(event) {
					me.remove();
				}
			})(this));
			this.$popup.addClass("fadeout");
		} else {
			this.remove();
		}
	},
	remove: function() {
		this.$popup.remove();
		this.visible = false;
	}
};