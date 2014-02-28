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
 * The artifact viewer is the entire UI for displaying artifacts. It can display
 * either a single artifact in single pane view, or two in split pane view. In
 * split pane view, either the same artifact is shown twice or two artifacts are
 * compared with each other.
 * <p>
 * The artifact viewer controls the currently active tool and deals with passing
 * tool events from the imagery pane back up to the tool itself.
 * @constructor Create a new artifact viewer
 */
function ArtifactViewer(eyesfirst) {
	// Generate our UI
	this.ui = $('<div/>').attr('id', 'artifact-viewer');
	this.toolbar = $('<header/>').addClass("imagery-toolbar");
	this.ui.append(this.toolbar);
	// Create icons:
	this.icons = {};
	for (var id in ArtifactViewer.TOOLBAR_ICONS) {
		var icon = ArtifactViewer.TOOLBAR_ICONS[id];
		this._createIcon(id, icon);
	}
	this.imageryContainer = $('<div/>').addClass("imagery-view");
	this.ui.append(this.imageryContainer);
	this.sidebar = $('<div/>').addClass("imagery-sidebar");
	this.ui.append(this.sidebar);
	this.timeline = new ArtifactTimeline(this.ui, this);
	// Create the metadata pane
	this.metadata = $('<div/>').addClass("imagery-metadata");
	this.sidebar.append(this.metadata);
	this.thicknessMap = $('<div class="oct-thickness-map imagery-sidebox"><h2>ETDRS Thickness Map</h2><img alt=""></div>');
	this.thicknessMap.hide();
	this.sidebar.append(this.thicknessMap);
	// Create the tools
	this.handTool = new ArtifactViewer.HandTool(this);
	this.rulerTool = new ArtifactViewer.RulerTool(this);
	// And activate the hand tool first
	this.tool = this.handTool;
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
	this._canShow = false;
}

ArtifactViewer.TOOLBAR_ICONS = {
	/*"single": { tooltip: "Single Pane" },
	"split-horiz": { tooltip: "Split Horizontally" },
	"split-vert": { tooltip: "Split Vertically" },*/
	"zoom-in": { tooltip: "Zoom In", onclick: function(viewer) { viewer.zoomIn() }, disabled: ["oct"] },
	"zoom-out": { tooltip: "Zoom Out", onclick: function(viewer) { viewer.zoomOut() }, disabled: ["oct"] },
	"play": { tooltip: "Play", onclick: function(viewer) { viewer.playSlices() } },
	"ruler": { tooltip: "Ruler", onclick: function(viewer) {
		if (viewer.tool == viewer.rulerTool) {
			viewer.setTool(viewer.handTool);
		} else {
			viewer.setTool(viewer.rulerTool);
		}
	}, disabled: ["fundus"] }
	/*"box-select": { tooltip: "Box Select" }*/
};

ArtifactViewer.prototype = {
	_createIcon: function(id, iconData) {
		var icon = $('<div/>').addClass("icon imagery-" + id).attr('title', iconData.tooltip).text(iconData.tooltip);
		this.icons[id] = {
			div: icon, data: iconData
		};
		this.toolbar.append(icon);
		var onclick = iconData.onclick;
		if (onclick) {
			(function(me, icon) {
				icon.click(function() {
					if (!icon.is(".disabled")) {
						onclick(me);
					}
				});
			})(this, icon);
		}
	},
	_updateToolbar: function(mode) {
		for (var id in this.icons) {
			var data = this.icons[id].data;
			var disabled = data.disabled;
			if (disabled) {
				var disable = false;
				for (var i = 0; i < disabled.length; i++) {
					if (disabled[i] == mode) {
						disable = true;
						break;
					}
				}
				if (disable) {
					this.icons[id].div.addClass("disabled");
				} else {
					this.icons[id].div.removeClass("disabled");
				}
			}
		}
	},
	getUI: function() {
		return this.ui;
	},
	getHash: function() {
		return "patient=" + this.patientId;
	},
	onshow: function() {
		// Some stuff gets calculated based on visible size, so fake a resize
		this.resized();
	},
	onhide: function() {
		this.ui.detach();
	},
	canShow: function() {
		return this._canShow;
	},
	_displayError: function(message) {
		// TODO: Something better than this
		if (this.loading) {
			this.loading.remove();
			this.loading = null;
		}
		this.imageryContainer.append($('<pre/>').text(message));
	},
	showPatient: function(patientId) {
		// We have something to show
		this._canShow = true;
		this._showLoading();
		this.patientId = patientId;
		var me = this;
		$.ajax({
			url: "patient/info/" + encodeURIComponent(patientId),
			dataType: 'json',
			error: function(jqXHR, textStatus, errorThrown) {
				me._displayError("Error loading artifact list: " + textStatus + "\n" + errorThrown);
			},
			success: function(data, textStatus, jqXHR) {
				// TODO: Pick the most recent artifact rather than the first
				// artifact.
				me.showPatientMetadata(data);
				var artifacts = data["artifacts"];
				if (artifacts.length == 0) {
					me._displayError("No artifacts to display.");
				} else {
					me.timeline.showArtifacts(artifacts);
					me.showArtifact(me.timeline.getMostRecent());
				}
			},
			//timeout: 60000, // timeout
			type: 'GET'
		});
	},
	showPatientMetadata: function(metadata) {
		this.metadata.empty();
		this.metadata.append($('<h2/>').text(metadata["lastName"] + ", " + metadata["firstName"]));
		var dl = $('<dl/>');
		this.metadata.append(dl);
		var birthday = "--/--/--";
		if (metadata["birthday"]) {
			birthday = DateUtil.parseDate(metadata["birthday"]);
			var month = birthday.getMonth() + 1;
			var day = birthday.getDate();
			birthday = birthday.getFullYear() + "/" +
				(month < 10 ? "0" + month : month) + "/" +
				(day < 10 ? "0" + day : day);
		}
		var gender = "?";
		if (metadata["gender"]) {
			gender = metadata["gender"];
		}
		dl.append("<dt>DOB:</dt>", $("<dd/>").text(birthday + " " + gender));
		if (metadata["mrn"]) {
			dl.append("<dt>MRN:</dt>", $("<dd/>").text(metadata["mrn"]));
		}
	},
	showArtifact: function(metadata, onload, hideLoading) {
		if (typeof metadata != 'object')
			throw Error('Loading by ID is not supported yet');
		if (arguments.length < 2)
			onload = function() { };
		if (arguments.length < 3 && !hideLoading) {
			this._showLoading();
		} else {
			// Otherwise, hide the thickness map immediately
			this.thicknessMap.hide();
		}
		this.x = 0;
		this.y = 0;
		this.zoom = 1;
		var type = metadata['type'];
		this.type = type;
		this.laterality = metadata['laterality'];
		this.timeline.setActiveArtifact(metadata);
		switch (type) {
		case "fundus":
			this._showFundusImage('artifact/fetch?id=' + encodeURIComponent(metadata['id']), metadata, onload);
			break;
		case "oct":
			this._showOCTImage(metadata, onload);
			break;
		default:
			this._displayError("Cannot display artifacts of type \"" + type + "\".");
			onload();
		}
	},
	_showLoading: function() {
		this.imageryContainer.empty();
		this.image = null;
		if (this.thumbnail) {
			this.thumbnail.remove();
			this.thumbnail = null;
		}
		this.loading = $('<div class="loading">Loading...</div>');
		this.imageryContainer.append(this.loading);
		this.thicknessMap.hide();
	},
	_showFundusImage: function(url, metadata, onload) {
		// Reset the active tool
		this.setTool(this.handTool);
		// Keep the loading indicator up for now and load the image in the
		// background, since things need to know the width/height and apparently
		// there is no way to track load progress.
		var img = new Image();
		var me = this;
		img.onload = function() {
			// Now that we have the imagery size, store it
			me.x = 0;
			me.y = 0;
			me.imageWidth = img.width;
			me.imageHeight = img.height;
			me.imageryContainer.empty();
			me.thumbnail = new ArtifactViewer.Thumbnail(me);
			me.sidebar.append(me.thumbnail.getUI());
			me.thumbnail.setImage(img.src);
			me.imageryView = new ArtifactViewer.ImagePane(me, me.imageryContainer, img, 'fundus');
			if (metadata['laterality']) {
				me.imageryView.setLaterality(metadata['laterality']);
			}
			me.zoomFit();
			me.updateThumbnailPosition();
			me._updateToolbar("fundus");
			// And inform our onload handler
			onload();
		};
		img.onerror = function() {
			me._displayError("Unable to load imagery");
		};
		img.src = url;
	},
	_showOCTImage: function(metadata) {
		// Reset the active tool
		this.setTool(this.handTool);
		var me = this;
		var sliceManager;
		function oncomplete() {
			// FIXME: Merge the copy-pasted code from showFundusImage some how
			// Create the imagery view
			me.imageryContainer.empty();
			me.x = 0;
			me.y = 0;
			me.imageWidth = sliceManager.getSliceWidth();
			me.imageHeight = sliceManager.getSliceHeight();
			me.thicknessMap.find("img").attr("src", sliceManager.getThicknessMapURL());
			me.thicknessMap.show();
			me.thumbnail = new ArtifactViewer.OCTFundusThumbnail(me);
			me.sidebar.append(me.thumbnail.getUI());
			me.thumbnail.setImage(sliceManager.getThumbnailURL(), 1);
			me.imageryView = new ArtifactViewer.OCTPane(me, me.imageryContainer, sliceManager);
			if (metadata['laterality']) {
				me.imageryView.setLaterality(metadata['laterality']);
			}
			me.zoomFit();
			me.updateThumbnailPosition();
			me._updateToolbar("oct");
			// Load classifier results
			sliceManager.loadClassifierResults(function(data) {
				me.imageryView.sliceSelector.setHardExudates(data["hardExudates"]);
			}, function() { });
		}
		var pb = $('<div class="progressbar"><div class="bar"></div></div>');
		this.loading.append(pb);
		pb = pb.find('.bar');
		function onprogress(progress) {
			pb.css('width', (progress*100) + '%');
		}
		sliceManager = new OCT.SliceManager(metadata['id'], oncomplete, function(error) { console.log(error); }, onprogress );
	},
	/**
	 * Scrolls the imagery to the given position. The x and y values are the
	 * top left corner of the image when fully zoomed in.
	 */
	scrollImageryTo: function(x, y) {
		// Constrain to the far edge first, because at zoom to fit, one of the
		// far edges may push the scroll negative
		var visibleWidth = this.imageryContainer.width() / this.zoom;
		var visibleHeight = this.imageryContainer.height() / this.zoom;
		var maxX = this.imageWidth - visibleWidth;
		if (x > maxX)
			x = maxX;
		var maxY = this.imageHeight - visibleHeight;
		if (y > maxY)
			y = maxY;
		// Then reset the scroll so that it can't go below 0,0
		if (x < 0)
			x = 0;
		if (y < 0)
			y = 0;
		this.x = x;
		this.y = y;
		this.imageryView.scrollTo(x, y);
		this.updateThumbnailPosition();
	},
	zoomIn: function() {
		var zoom = this.zoom * 2;
		if (zoom > 1) {
			zoom = 1;
		}
		this.zoomTo(zoom);
	},
	zoomOut: function() {
		var zoom = this.zoom / 2;
		var min = this.imageryView.getShowAllZoom()
		if (zoom < min) {
			zoom = min;
		}
		this.zoomTo(zoom);
	},
	/**
	 * Set the imagery to "zoom to fit".
	 */
	zoomFit: function() {
		this.zoomTo(this.imageryView.getShowAllZoom());
	},
	zoomTo: function(scale) {
		// Figure out the center of the image right now
		var visibleWidth = this.getVisibleWidth();
		var visibleHeight = this.getVisibleHeight();
		var cx = this.x + visibleWidth / 2;
		var cy = this.y + visibleHeight / 2;
		// Change the zoom...
		this.zoom = scale;
		this.width = Math.round(this.imageWidth * this.zoom);
		this.height = Math.round(this.imageHeight * this.zoom);
		this.imageryView.setZoom(this.zoom);
		visibleWidth = this.getVisibleWidth();
		visibleHeight = this.getVisibleHeight();
		// And attempt to reset the X/Y
		this.scrollImageryTo(Math.round(cx - visibleWidth / 2), Math.round(cy - visibleHeight / 2));
	},
	resized: function() {
		if (this.imageryView)
			this.imageryView.resized();
		if (this.thumbnail)
			this.updateThumbnailPosition();
		this.timeline.resized();
	},
	getVisibleWidth: function() {
		return Math.min(this.imageryContainer.width() / this.zoom, this.imageWidth);
	},
	getVisibleHeight: function() {
		return Math.min(this.imageryContainer.height() / this.zoom, this.imageHeight);
	},
	updateThumbnailPosition: function() {
		// Figure out how much of the image is visible.
		var visibleWidth = this.imageryContainer.width() / this.zoom;
		var visibleHeight = this.imageryContainer.height() / this.zoom;
		if (this.thumbnail != null)
			this.thumbnail.setPosition(this.x, this.y, visibleWidth, visibleHeight, this.imageWidth, this.imageHeight);
	},
	/**
	 * Assuming an OCT image is being displayed (or any artifact with slices,
	 * really), jump to the given slice.
	 */
	setSlice: function(slice) {
		if (this.imageryView.setSlice)
			this.imageryView.setSlice(slice);
	},
	/**
	 * Assuming an image with a vertical bar marker is being displayed, adjust
	 * the vertical bar position.
	 */
	setVerticalBarPosition: function(x) {
		if (this.imageryView.setVerticalBarPosition)
			this.imageryView.setVerticalBarPosition(x);
	},
	playSlices: function() {
		if (this.imageryView.playSlices) {
			this.imageryView.playSlices();
		} else {
			var images = this.timeline.getAllOfType(this.type, this.laterality);
			var me = this;
			var img;
			var i = 0;
			function nextFrame() {
				me.showArtifact(images[i], function() {
					if (i < images.length)
						setTimeout(nextFrame, 1500);
				}, true);
				i++;
			}
			nextFrame();
		}
	},
	/**
	 * Convert a given number of pixels to a length in millimeters, if possible.
	 * This is based on the current zoom level and the dot pitch, if available.
	 * If not available, this returns {@code false}.
	 */
	convertPixelsToMMs: function(pixels) {
		var pixelPitch = this.imageryView.getPixelPitch();
		if (pixelPitch > 0) {
			return pixels / this.zoom * pixelPitch;
		} else {
			return false;
		}
	},
	setTool: function(tool) {
		this.tool = tool;
		this.imageryContainer.css('cursor', tool && tool.getCursor ? tool.getCursor() : 'default');
	}
};

/**
 * The hand tool - allows imagery to be dragged around, moving the display.
 * @constructor Create a new hand tool
 */
ArtifactViewer.HandTool = function(viewer) {
	this.viewer = viewer;
}

ArtifactViewer.HandTool.prototype = {
	ondragstart: function(event) {
		this.startPageX = event.pageX;
		this.startPageY = event.pageY;
		this.startImageX = this.viewer.x;
		this.startImageY = this.viewer.y;
		//console.log("Drag start at " + this.startPageX + ", " + this.startPageY);
		return true;
	},
	ondrag: function(event) {
		var dx = event.pageX - this.startPageX;
		var dy = event.pageY - this.startPageY;
		this.viewer.scrollImageryTo(this.startImageX - (dx / this.viewer.zoom), this.startImageY - (dy / this.viewer.zoom));
		//console.log("Dragged to " + event.pageX + ", " + event.pageY + ", difference is " + dx + ", " + dy);
	},
	ondragstop: function(event) {
		// Pretend this is a drag to move the image to its final location
		this.ondrag(event);
		// And then do whatever stopping the event would have done, which at
		// present, is nothing.
	}
};

/**
 * The ruler tool - displays an overlay that allows the user to measure
 * distances on the image.
 * @constructor Create a new ruler tool
 */
ArtifactViewer.RulerTool = function(viewer) {
	this.viewer = viewer;
}

ArtifactViewer.RulerTool.prototype = {
	getCursor: function() {
		return 'crosshair';
	},
	formatNumber: function(n) {
		return n.toFixed(Math.max(0,4-n.toFixed(0).length));
	},
	drawLine: function(sx, sy, dx, dy) {
		this.viewer.imageryView.clearAnnotationLayer();
		var ctx = this.viewer.imageryView.getAnnotationContext();
		ctx.strokeStyle = 'rgb(235,98,44)';
		ctx.lineWidth = 2;
		ctx.shadowOffsetX = 0;
		ctx.shadowOffsetY = 1;
		ctx.shadowBlur = 2;
		ctx.shadowColor = 'rgba(0,0,0,1)';
		ctx.beginPath();
		ctx.moveTo(sx, sy);
		ctx.lineTo(dx, dy);
		ctx.stroke();
		// Calculate the length of the line
		var w = dx - sx, h = dy - sy;
		var length = Math.sqrt(w * w + h * h);
		// Attempt to convert this to mms
		var mm = this.viewer.convertPixelsToMMs(length);
		var t = mm === false ? length.toFixed(3) + " px" : this.formatNumber(mm*1000) + " \u00b5m";
		ctx.font = '14px sans-serif';
		ctx.textAlign = w >= 0 ? 'right' : 'left';
		ctx.textBaseline = h >= 0 ? 'top' : 'bottom';
		ctx.shadowBlur = 2;
		ctx.fillStyle = 'rgb(235,98,44)';
		ctx.fillText(t, (sx + dx) / 2, (sy + dy) / 2);
	},
	ondragstart: function(event) {
		var p = this.viewer.imageryContainer.offset();
		this.startX = event.pageX - p.left;
		this.startY = event.pageY - p.top;
		return true;
	},
	ondrag: function(event) {
		var p = this.viewer.imageryContainer.offset();
		var dx = event.pageX - p.left;
		var dy = event.pageY - p.top;
		this.drawLine(this.startX, this.startY, dx, dy);
	},
	ondragstop: function(event) {
		this.ondrag(event);
	}
};

/**
 * @constructor create a thumbnail view
 */
ArtifactViewer.Thumbnail = function(viewer) {
	this.ui = $('<div class="imagery-thumbnail imagery-sidebox">' +
			'<h2></h2>' +
			'<div class="thumbnail">' +
				'<div class="bounding-box"></div><img src="" alt="" style="display: none;">' +
			'</div>' +
		'</div>');
	this.viewer = viewer;
	this.box = this.ui.find('.bounding-box');
	this.image = this.ui.find('img');
	// Bind event handlers
	(function(me, ui, box) {
		ui.mousedrag({
			drag: function(event) { return me.ondrag(event); },
			dragstart: function(event) { return me.ondragstart(event, true); },
			dragstop: function(event) { return me.ondragstop(event); }
		});
		box.mousedrag({
			drag: function(event) { return me.ondrag(event); },
			dragstart: function(event) { return me.ondragstart(event, false); },
			dragstop: function(event) { return me.ondragstop(event); }
		});
	})(this, this.image, this.box);
}

ArtifactViewer.Thumbnail.prototype = {
	getUI: function() { return this.ui; },
	remove: function() {
		this.ui.remove();
	},
	empty: function() {
		this.image.hide();
		this.image.attr('src', '');
	},
	setImage: function(url, aspectRatio) {
		var img = new Image(), me = this;
		if (arguments.length == 1) {
			aspectRatio = -1;
		}
		img.onload = function(event) {
			me.actualWidth = img.width;
			me.actualHeight = img.height;
			if (aspectRatio <= 0) {
				// Use aspect ratio
				aspectRatio = img.width / img.height;
			}
			var height = 199;
			if (aspectRatio <= 1) {
				// width < height, so adjust height to 199px and width to match
				me.image.css({
					'width': (199 * aspectRatio) + "px",
					'height': "199px"
				});
			} else {
				height = (199 / aspectRatio);
				me.image.css({
					'width': "199px",
					'height': height + "px"
				});
			}
			var header = me.ui.find("h2").height();
			me.ui.css("height", (header + height) + "px");
			me.image.attr('src', url);
			me.image.show();
		};
		img.src = url;
	},
	setPosition: function(x, y, width, height, imageWidth, imageHeight) {
		// Constrain the visible area to the image
		if (width > imageWidth)
			width = imageWidth;
		if (height > imageHeight)
			height = imageHeight;
		// Save position information (needed for the drag handlers).
		this.x = x;
		this.y = y;
		this.visibleWidth = width;
		this.visibleHeight = height;
		this.imageWidth = imageWidth;
		this.imageHeight = imageHeight;
		var scale = this.ui.width() / imageWidth;
		//console.log('Viewed area: ' + x + ', ' + y + ' [' + width + 'x' + height + '], scale is ' + scale);
		// If the entire thing is visible, hide the box entirely
		if (width >= imageWidth && height >= imageHeight) {
			this.box.css('display', 'none');
		} else {
			this.box.css({
				display: 'block',
				left: (x * scale) + 'px',
				top: (y * scale) + 'px',
				// FIXME: Border widths are hardcoded here
				width: ((width * scale) - 4) + 'px',
				height: ((height * scale) - 4) + 'px'
			});
		}
	},
	ondragstart: function(event, jump) {
		if (this.imageWidth) {
			if (jump) {
				// Jump the center to wherever the mouse is
				var scale = this.imageWidth / this.ui.width();
				var p = this.image.offset();
				var x = (event.pageX - p.left) * scale - this.visibleWidth / 2;
				var y = (event.pageY - p.top) * scale - this.visibleHeight / 2;
				this.viewer.scrollImageryTo(x, y);
			}
			this.dragStartX = event.pageX;
			this.dragStartY = event.pageY;
			this.imageStartX = this.x;
			this.imageStartY = this.y;
			return true;
		} else {
			return false;
		}
	},
	ondrag: function(event) {
		var dx = event.pageX - this.dragStartX;
		var dy = event.pageY - this.dragStartY;
		var scale = this.imageWidth / this.ui.width();
		this.viewer.scrollImageryTo(this.imageStartX + (dx * scale), this.imageStartY + (dy * scale));
		//console.log("Dragged to " + event.pageX + ", " + event.pageY + ", difference is " + dx + ", " + dy);
	},
	ondragstop: function(event) {
		// Pretend this is a drag to move the image to its final location
		this.ondrag(event);
		// And then do whatever stopping the event would have done, which at
		// present, is nothing.
	}
}

/**
 * @constructor create the OCT fundus thumbnail, which is basically identical
 * to the regular thumbnail but shows an overview of the OCT image
 */
ArtifactViewer.OCTFundusThumbnail = function(viewer) {
	this.ui = $('<div class="imagery-thumbnail imagery-sidebox">' +
			'<h2>Enhanced Blood Vessels</h2>' +
			'<div class="thumbnail">' +
				'<div class="horizontal-line"></div><div class="vertical-line"></div><img src="" alt="" style="display: none;">' +
			'</div>' +
		'</div>');
	this.viewer = viewer;
	this.horizLine = this.ui.find('.horizontal-line');
	this.vertLine = this.ui.find('.vertical-line');
	this.image = this.ui.find('img');
	// Bind event handlers
	(function(me) {
		for (var i = 1; i < arguments.length; i++) {
			arguments[i].mousedrag({
				drag: function(event) { return me.ondrag(event); },
				dragstart: function(event) { return me.ondragstart(event); },
				dragstop: function(event) { return me.ondragstop(event); }
			});
		}
	})(this, this.image, this.horizLine, this.vertLine);
}

ArtifactViewer.OCTFundusThumbnail.PROXIMITY = 8;

ArtifactViewer.OCTFundusThumbnail.prototype = {
	getUI: function() { return this.ui; },
	remove: function() { this.ui.remove(); },
	empty: function() {
		this.image.hide();
		this.image.attr('src', '');
	},
	// One of the nice things about JavaScript is we can steal implementations
	// from other classes directly. Like so:
	setImage: ArtifactViewer.Thumbnail.prototype.setImage,
	setPosition: function(x, y, width, height, imageWidth, imageHeight) {
		// This doesn't make sense for this type of thumbnail, so just flat-out
		// ignore it.
	},
	setOCTPosition: function(slice, sliceCount, vertX, width) {
		this.slice = slice;
		this.sliceCount = sliceCount;
		this.vertX = vertX;
		this.vertWidth = width;
		this.horizLine.css('top', ((slice / sliceCount) * 100) + "%");
		this.vertLine.css('left', ((vertX / width) * 100) + "%");
	},
	ondragstart: function(event) {
		if (this.slice) {
			// TODO: Determine what we're closest to, and possibly only drag
			// one bar at a time.
			// But for now, always jump:
			this.ondrag(event);
			return true;
		} else {
			return false;
		}
	},
	ondrag: function(event) {
		var p = this.image.offset();
		var x = (event.pageX - p.left);
		var y = (event.pageY - p.top);
		// Convert to slice:
		var slice = Math.round((y / this.image.height()) * this.sliceCount);
		if (slice < 0)
			slice = 0;
		if (slice >= this.sliceCount)
			slice = this.sliceCount - 1;
		this.viewer.setSlice(slice);
		var bx = Math.round((x / this.image.width()) * this.vertWidth);
		this.viewer.setVerticalBarPosition(bx);
	},
	ondragstop: function(event) {
		// Pretend this is a drag to move the image to its final location
		this.ondrag(event);
		// And then do whatever stopping the event would have done, which at
		// present, is nothing.
	}
}

/**
 * @constructor Create a new image pane
 */
ArtifactViewer.ImagePane = function(viewer, container, image) {
	if (arguments.length == 0) {
		// To allow extending, do nothing when called with no arguments
		return;
	}
	this.viewer = viewer;
	this.container = container;
	// Create a container within our container to hold our imagery view (which
	// is needed to work around some CSS stuff, basically to allow things to
	// be positioned on the edge of the imagery view and not the page)
	container.append(this.imageryContainer = $('<div/>').addClass("imagery-container"));
	this.x = 0;
	this.y = 0;
	this.width = image.width;
	this.height = image.height;
	this.image = this._createUI(this.imageryContainer, image);
	this.canvas = this._createAnnotationCanvas(this.imageryContainer);
	this.$canvas = $(this.canvas);
	this.annotationContext = this.canvas.getContext('2d');
	// Bind mouse handlers
	(function(me, image) {
		image.mousedrag({
			drag: function(event) { return me.ondrag(event); },
			dragstart: function(event) { return me.ondragstart(event); },
			dragstop: function(event) { return me.ondragstop(event); }
		});
	})(this, this.image);
}

ArtifactViewer.ImagePane.prototype = {
	/**
	 * Override to create the UI. Return the image used.
	 */
	_createUI: function(container, image) {
		var rv = $('<img/>').attr('src', image.src).attr('alt', '');
		container.append(rv);
		return rv;
	},
	_createAnnotationCanvas: function(container) {
		var rv = document.createElement("canvas");
		rv.className = "annotation-layer";
		container.append(rv);
		return rv;
	},
	clearAnnotationLayer: function() {
		this.annotationContext.clearRect(0, 0, this.canvas.width, this.canvas.height);
	},
	getAnnotationContext: function() {
		return this.annotationContext;
	},
	getPixelPitch: function() {
		return null;
	},
	setLaterality: function(laterality) {
		if (!this.laterality) {
			this.laterality = $('<div class="laterality"></div>');
			this.image.after(this.laterality);
		}
		this.laterality.text(laterality);
	},
	// TODO: Check to see if the tool implements any of the following events
	ondragstart: function(event) {
		return this.viewer.tool.ondragstart(event);
	},
	ondrag: function(event) {
		return this.viewer.tool.ondrag(event);
	},
	ondragstop: function(event) {
		return this.viewer.tool.ondragstop(event);
	},
	/**
	 * Calculate the zoom that will display all of the image.
	 */
	getShowAllZoom: function() {
		var zoomWidth = this.container.width() / this.width;
		var zoomHeight = this.container.height() / this.height;
		return Math.min(zoomWidth, zoomHeight);
	},
	setZoom: function(zoom) {
		this.zoom = zoom;
		this.viewWidth = Math.round(this.width * zoom);
		this.viewHeight = Math.round(this.height * zoom);
		this.image.css({
			"width": this.viewWidth + 'px',
			"height": this.viewHeight + 'px'
		});
		// And fake a scrollTo to make the image location correct
		this.scrollTo(this.x, this.y);
	},
	/**
	 * Move the image so the given x,y coordinates are the top left corner.
	 * Should ONLY be called by the master artifact viewer.
	 */
	scrollTo: function(x, y) {
		//console.log("Scroll to " + x + "," + y + " (at " + this.zoom + ": " + (-(x * this.zoom)) + ", " + (-(y * this.zoom)) + ")");
		this.image.css({
			'margin-left': -(x * this.zoom), 'margin-top': -(y * this.zoom)
		});
		this.x = x;
		this.y = y;
		this.resized();
	},
	/**
	 * Called whenever the window has resized.
	 */
	resized: function() {
		// If the image is now smaller than the container, center it.
		if (this.viewWidth < this.container.width()) {
			this.image.css('margin-left', (this.container.width() - this.viewWidth) / 2);
		}
		if (this.viewHeight < this.container.height()) {
			this.image.css('margin-top', (this.container.height() - this.viewHeight) / 2);
		}
		// Resize the annotation layer to fit
		this.canvas.width = this.$canvas.width();
		this.canvas.height = this.$canvas.height();
	}
};

/**
 * Create an artifact viewer that shows an OCT image. This is almost the same
 * as the image pane but with a few changes.
 * @constructor Create a new OCT pane
 */
ArtifactViewer.OCTPane = function(viewer, container, sliceManager) {
	this.sliceManager = sliceManager;
	ArtifactViewer.ImagePane.call(this, viewer, container, sliceManager);
	// Now that the master container has been created, throw in the slice selector
	this.sliceSelector = new OCT.SliceSelector(this.imageryContainer, this.slice, sliceManager.getSliceCount());
	this.sliceSelector.onslicechange = (function(me) {
		return function(slice) {
			me.setSlice(slice);
		}
	})(this);
	this.verticalBarX = this.viewWidth / 2;
	viewer.thumbnail.setOCTPosition(this.slice, this.sliceManager.getSliceCount(), this.verticalBarX, this.viewWidth);
}

ArtifactViewer.OCTPane.prototype = new ArtifactViewer.ImagePane;

ArtifactViewer.OCTPane.prototype._createUI = function(container, sliceManager) {
	// At some point, the image returned might in fact be a canvas, but for now:
	var img = $('<img alt=""></img>');
	this.slice = Math.floor(sliceManager.getSliceCount()/2);
	img.attr('src', sliceManager.getSlice(this.slice).src);
	container.append(img);
	container.append(this.verticalLine = $('<div class="vertical-line"></div>'));
	return img;
};

ArtifactViewer.OCTPane.prototype.setSlice = function(slice) {
	if (slice == this.slice)
		return;
	this.slice = slice;
	this.viewer.thumbnail.setOCTPosition(slice, this.sliceManager.getSliceCount(), this.verticalBarX, this.viewWidth);
	this.sliceSelector.setSlice(this.slice);
	var s = this.sliceManager.getSlice(this.slice);
	this.image.attr('src', s.src);
};

ArtifactViewer.OCTPane.prototype.setVerticalBarPosition = function(x) {
	if (x == this.verticalBarX)
		return;
	if (x < 0)
		x = 0;
	if (x >= this.viewWidth)
		x = this.viewWidth - 1;
	this.verticalBarX = x;
	this.viewer.thumbnail.setOCTPosition(this.slice, this.sliceManager.getSliceCount(), this.verticalBarX, this.viewWidth);
	this.verticalLine.css('left', x + 'px');
};

ArtifactViewer.OCTPane.prototype.playSlices = function() {
	var curSlice = -1;
	var totalSlices = this.sliceManager.getSliceCount();
	var me = this;
	function nextSlice() {
		curSlice++;
		if (curSlice < totalSlices) {
			me.setSlice(curSlice);
			setTimeout(nextSlice, 100);
		}
	}
	nextSlice();
};

ArtifactViewer.OCTPane.prototype.getPixelPitch = function() {
	return this.sliceManager.getPixelPitch();
};

// FIXME: Temporary: disable scrolling, always zoom to fit
ArtifactViewer.OCTPane.prototype.setZoom = function(zoom) {
	if (zoom == this.zoom)
		return;
	ArtifactViewer.ImagePane.prototype.setZoom.apply(this, arguments);
	ArtifactViewer.ImagePane.prototype.resized.apply(this, arguments);
};
ArtifactViewer.OCTPane.prototype.scrollTo = function() { };
ArtifactViewer.OCTPane.prototype.resized = function() {
	this.viewer.zoomFit();
	this.sliceSelector.resized();
};