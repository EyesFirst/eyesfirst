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
 * The main uploader structure: the basic Uploader class that handles moving
 * through pages, and the page structure itself.
 */

(function($){
	/**
	 * The main class that deals with the uploader workflow.
	 * @constructor
	 */
	function Uploader(container) {
		// Pages
		this._pages = {};
		this._history = [];
		var steps = {
			"choose-efid": "Choose EFID",
			"choose-files-dicom": "Choose DICOM ZIPs",
			"choose-files-fundus": "Choose Fundus ZIP",
			"deid-run": "Anonymize Files",
			"deid-verify": "Verify Anonymized Information",
			"upload": "Upload Files"
		};
		this._steps = {};
		this._stepsList = [];

		// Create our UI.
		this._topDiv = $(container).addClass('uploader');
		this._statusDiv = $('<div/>').addClass('uploader-status');
		for (var step in steps) {
			var s = steps[step];
			var div = $('<div/>').addClass('step');
			div.append($('<span/>').text(s));
			this._statusDiv.append(div);
			var o = {
				id: step,
				name: s,
				div: div,
				index: this._stepsList.length
			};
			this._steps[step] = o;
			this._stepsList.push(o);
		}
		var width = (100 / this._stepsList.length) + '%';
		for (var i = 0; i < this._stepsList.length; i++) {
			this._stepsList[i].div.css('width', width);
		}
		this._statusDiv.append($('<div/>').css({"clear":"both"}));
		this._topDiv.append(this._statusDiv);
		this._pageContainer = $('<div/>').addClass('page');
		this._topDiv.append(this._pageContainer);
		this._buttons = $('<div/>').addClass('page-buttons');
		this._buttons.append(this._form = $('<form method="GET" action=""></form>').bind('submit', function() { return false; }));
		this._backButton = $('<button/>').text("< Back");
		this._backButton.click((function(me){
			return function() {
				me.back();
			}
		})(this));
		this._form.append(this._backButton);
		this._nextButton = $('<button/>').text("Next >");
		this._nextButton.click((function(me) {
			return function() {
				me.next();
			}
		})(this));
		this._form.append(this._nextButton);
		this._topDiv.append(this._buttons);
		// And start
		this.showPage(new HIPAAPage());
	}

	Uploader.prototype = {
		/**
		 * Shows the given page. 
		 * @protected
		 */
		showPage: function(page, backwards) {
			if (arguments.length == 1)
				backwards = false;
			if (!backwards) {
				this._history.push(page)
			}
			this._showPage(page, !backwards);
		},
		/**
		 * Entirely replaces the current page with the given page. This removes
		 * the current page from the "history" so that the "back" button can't
		 * be used to go to it, and instead makes the new page take its place.
		 * <p>
		 * If there is a current page, it's {@link Page#onNext()} method will
		 * be queried.
		 */
		replacePage: function(page) {
			if (this._currentPage) {
				if (!this._currentPage.onNext())
					return;
			}
			if (this._history.length > 0)
				this._history[this._history.length-1] = page;
			this._showPage(page, true);
		},
		/**
		 * Internal method that handles removing the currently visible page and
		 * displaying the new page. Does not adjust history at all.
		 */
		_showPage: function(page, forwards) {
			if (this._currentPage) {
				this._currentPage.hidePage();
			}
			this._currentPage = page;
			// Reset button text
			this.setNextText("Next >");
			this._pageContainer.empty();
			this._currentPage.showPage(this, this._pageContainer, forwards);
			this.updateWizardButtons();
		},
		/**
		 * Go backwards a page, if possible.
		 */
		back: function() {
			if (this._history.length > 0) {
				// Pop off the last page...
				this._history.pop();
				// ...and move backwards
				this.showPage(this._history[this._history.length-1], true);
			}
		},
		/**
		 * Go forwards a page, if possible.
		 */
		next: function() {
			if (this._currentPage) {
				if (this._currentPage.onNext()) {
					var page = this._currentPage.getNext();
					if (page != null) {
						this.showPage(page);
					}
				}
			}
		},
		/**
		 * Changes the text of the next button.
		 */
		setNextText: function(text) {
			this._nextButton.text(text);
		},
		/**
		 * Updates the state of the back and next buttons (used primarily when
		 * a page is now ready to enable next).
		 */
		updateWizardButtons: function() {
			this._backButton.prop('disabled', this._history.length <= 1);
			this._nextButton.prop('disabled', !(this._currentPage && this._currentPage.isNextAvailable()));
		},
		/**
		 * Sets which wizard buttons are available. If called with only one
		 * argument, sets both back and next buttons.
		 * 
		 * @param {Boolean} back
		 *            whether the back button is visible
		 * @param {Boolean} [next]
		 *            whether the next button is visible - if not given,
		 *            uses the same value as {@code back}
		 */
		setWizardButtonsVisible: function(back, next) {
			if (arguments.length == 1)
				next = back;
			this._backButton[back ? 'show' : 'hide']();
			this._nextButton[next ? 'show' : 'hide']();
		},
		/**
		 * Sets the ID of the current step. The step IDs may be "nested" - for
		 * example, if there is a step called {@code "foo-bar"}, but not
		 * {@code "foo-bar-baz"}, and {@code setStep("foo-bar-baz")} is invoked,
		 * the {@code "foo-bar"} step will be set.
		 */
		setStep: function(step) {
			while (true) {
				if (step in this._steps) {
					// Use this step
					var currentStep = this._steps[step];
					currentStep.div.attr('class', 'step current');
					for (var i = currentStep.index+1; i < this._stepsList.length; i++) {
						this._stepsList[i].div.attr('class', 'step');
					}
					for (var i = currentStep.index-1; i >= 0; i--) {
						this._stepsList[i].div.attr('class', 'step completed');
					}
					return;
				}
				var i = step.lastIndexOf('-');
				if (i < 0)
					break;
				step = step.substring(0, i);
			}
		}
	};

	/**
	 * Base class for pages. The default constructor doesn't do anything, but
	 * invoking <code>Page.apply(this, arguments)</code> won't hurt.
	 * @constructor
	 */
	function Page() {
	}

	Page.prototype = {
		/**
		 * Determine whether or not the user is allowed to move off this page.
		 * The default just returns <code>{@link #getNext()} == null</code>.
		 */
		isNextAvailable: function() {
			return this.getNext() != null;
		},
		/**
		 * Gets the next page, if there is one.
		 */
		getNext: function() {
			return this._next;
		},
		/**
		 * Sets the next page, returned by the default {@link #getNext()}.
		 * @protected
		 * @param {Page} next the next page
		 */
		setNext: function(next) {
			this._next = next;
		},
		getStep: function() {
			return this._step;
		},
		/**
		 * Sets the step of this page. If set, the default {@link #showPage()}
		 * method will invoke {@link Uploader#setStep()} with this value.
		 */
		setStep: function(step) {
			this._step = step;
		},
		/**
		 * Internal method to render the page. Return a jQuery object (or an
		 * HTML element, or a string) of the actual page. This is invoked by
		 * {@link #showPage()} to actually show the page. By default the
		 * return result will be cached and reused.
		 * @param {Uploader} uploader the uploader from {@code showPage()}
		 */
		render: function(uploader) {
			return "<strong>Please override <code>render()</code>!</strong>";
		},
		/**
		 * Invoked when the page is being hidden. The default model detaches
		 * the existing content from the container. This is generally what you
		 * want, so that it can be re-added when the page is reshown.
		 */
		hidePage: function() {
			this._contents.detach();
		},
		/**
		 * Show the current page in the given container. The container will
		 * already be empty, and any page contents should be appended into the
		 * container.
		 * <p>
		 * The default implementation appends any contents set by
		 * {@link #setContents()} (if any), and if there aren't any, invokes
		 * {@link #render()} to create contents.
		 * 
		 * @param {Uploader} uploader
		 *            the uploader showing this page
		 * @param {jQuery} container
		 *            the jQuery object to render page contents within
		 * @param {Boolean} forwards
		 *            {@code true} if the page is being visited as normal,
		 *            {@code false} if the page is being re-displayed because
		 *            the user went backwards through the workflow
		 */
		showPage: function(uploader, container, forwards) {
			if (!this._contents) {
				this.setContents(this.render(uploader));
			}
			if (this._step) {
				uploader.setStep(this._step);
			}
			container.append(this._contents);
		},
		/**
		 * Sets the page contents.
		 * @protected
		 * @param contents the page contents. If a jQuery object, it's used
		 * directly. If a String, it's used as HTML in a new {@code <DIV>}.
		 * Otherwise, the toString() value is used as text in a new
		 * {@code <DIV>}.
		 */
		setContents: function(contents) {
			if (typeof contents == 'object') {
				if ('length' in contents && 'join' in contents) {
					// Assume it's an array, and:
					contents = contents.join('');
					contents = $(contents);
				}
				if (!('jquery' in contents)) {
					contents = $(contents);
				}
			} else if (typeof contents == 'string') {
				contents = $('<div/>').html(contents);
			} else {
				contents = $('<div/>').text(contents.toString());
			}
			this._contents = contents;
		},
		/**
		 * Invoked just before this page is hidden because the previous page is
		 * being displayed.
		 * 
		 * @return {Boolean} {@code true} to move back a page, {@code false} to
		 *         force this page to remain. The default always returns
		 *         {@code true}.
		 */
		onBack: function() {
			return true;
		},
		/**
		 * Invoked just before this page is hidden because the next page is
		 * being displayed. This is also invoked when
		 * {@link Uploader#replacePage()} is being used to replace this page.
		 * 
		 * @return {Boolean} {@code true} to move to the next page,
		 *         {@code false} to force this page to remain. The default
		 *         always returns {@code true}.
		 */
		onNext: function() {
			return true;
		}
	};

	// Exports:

	window['Uploader'] = Uploader;
	Uploader['setEFIDIssuerURL'] = function(url) { Uploader.EFID_ISSUER_URL = url; };
	Uploader['setEFIDVerifyierURL'] = function(url) { Uploader.EFID_VERIFIER_URL = url; };
	Uploader['setSessionId'] = function(sessionId) { Uploader.SESSION_ID = sessionId; };
	window['Page'] = Page;
})(jQuery);