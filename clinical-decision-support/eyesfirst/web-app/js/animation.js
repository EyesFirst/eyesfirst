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
 * Utilities for using CSS3 animation and working around browser prefixes.
 */

// Check for animation support based on the script at
// https://developer.mozilla.org/en-US/docs/CSS/Tutorials/Using_CSS_animations/Detecting_CSS_animation_support

function Animation() { }

(function() {
	var supported = false,
		jsname = 'animation',
		cssprefix = '',
		domPrefixes = ['Webkit','Moz','O','ms','Khtml'],
		eventPrefixes = ['webkit',null,'o','MS','khtml'],
		eventCapitalizes = [true,false,false,true,true],
		pfx = '',
		eventPrefix = '', eventCapitalize = false;

	var style = [ "animation-name: test" ];
	for (var i = 0; i < domPrefixes.length; i++) {
		style.push("-" + domPrefixes[i].toLowerCase() + "-animation-name: test");
	}

	var elm = document.createElement('div');
	elm.setAttribute("style", style.join(';'));

	if (elm.style.animationName) { supported = true; }

	if (supported === false) {
		for (var i = 0; i < domPrefixes.length; i++) {
			if( elm.style[domPrefixes[i] + 'AnimationName'] !== undefined) {
				pfx = domPrefixes[i];
				eventPrefix = eventPrefixes[i];
				eventCapitalize = eventCapitalizes[i];
				jsname = pfx + 'Animation';
				cssprefix = '-' + pfx.toLowerCase() + '-';
				supported = true;
				break;
			}
		}
	}

	Animation.supported = supported;
	Animation.prefix = cssprefix;
	Animation.propertyName = jsname;
	Animation.eventPrefix = eventPrefix;
	Animation.eventCapitalize = eventCapitalize;
})();

Animation.addAnimationStartEvent = function(element, handler) {
	Animation.addAnimationEvent(element, "Start", handler);
};

Animation.addAnimationIterationEvent = function(element, handler) {
	Animation.addAnimationEvent(element, "Iteration", handler);
};

Animation.addAnimationEndEvent = function(element, handler) {
	Animation.addAnimationEvent(element, "End", handler);
};

Animation.addAnimationEvent = function(element, event, handler) {
	var n = Animation.eventPrefix + "Animation" + event;
	if (!Animation.eventCapitalize)
		n = n.toLowerCase();
	element.addEventListener(n, handler, false);
};