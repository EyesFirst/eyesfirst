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
 * The timeline portion of the artifact viewer.
 */
function ArtifactTimeline(container, viewer) {
	this.ui = $('<div class="imagery-timeline"><div class="imagery-timeline-graph"></div><div class="imagery-artifact-list"><ul></ul></div><div class="imagery-timeline-key"></div><div class="imagery-graph-tabs"></div></div>');
	this.timeline = new Timeline(this.ui.find(".imagery-timeline-graph"));
	this.list = this.ui.find(".imagery-artifact-list ul");
	// Create the key
	this.key = this.ui.find(".imagery-timeline-key");
	this.tabs = this.ui.find(".imagery-graph-tabs");
	var ul = $("<ul/>");
	this.tabs.append(ul);
	ul.append(this.visitTab = $("<li>Visits</li>").click((function(me) {
		return function() {
			me.showVisitsOnTimeline();
		};
	})(this)).addClass("active"));
	ul.append(this.graphTab = $("<li>Graphs</li>").click((function(me) {
		return function() {
			me.showGraphsOnTimeline();
		};
	})(this)));
	this.createKey(ArtifactTimeline.TYPE_KEY);
	container.append(this.ui);
	this.viewer = viewer;
}

ArtifactTimeline.TYPE_KEY = {
	'oct': 'OCT',
	'fundus': 'FP',
	'fluoro': 'FA'//,
	//'scanning': 'SL'
};

ArtifactTimeline.GRAPH_KEY = {
	'hrc': 'HRC',
	'csf': 'CSF'
};

ArtifactTimeline.prototype = {
	createKey: function(key) {
		this.key.empty();
		var ul = $("<ul/>");
		this.key.append(ul);
		for (var t in key) {
			ul.append($("<li/>").addClass(t).text(key[t]));
		}
	},
	getArtifactIcon: function(type) {
		if (type in ArtifactTimeline.ICONS)
			return ArtifactTimeline.ICONS[type];
		else
			return ArtifactTimeline.UNKNOWN_ICON;
	},
	showArtifacts: function(artifacts) {
		this.timeline.empty();
		for (var i = 0; i < artifacts.length; i++) {
			// Convert the dates to timestamps...
			artifacts[i]["timestamp"] = DateUtil.parseDateTime(artifacts[i]["timestamp"]).getTime();
		}
		artifacts.sort(function(a, b) {
			a = a["timestamp"];
			b = b["timestamp"];
			return a < b ? -1 : (a == b ? 0 : 1);
		});
		this.artifacts = artifacts;
		this.artifactItems = {};
		this.list.empty();
		var hardExudates = {};
		var thicknesses = {};
		var mostRecent = null;
		var visits = [];
		// Because visits are already sorted by time, we can just go forward and
		// and items to the current visit.
		var currentVisit = null;
		for (var i = 0; i < artifacts.length; i++) {
			var li = $('<li/>').addClass("artifact " + artifacts[i]['type']);
			li.append($('<img/>').attr('src', 'artifact/thumbnail/' + artifacts[i]['id']));
			var laterality = artifacts[i]['laterality'];
			if (laterality) {
				li.append($('<div/>').addClass("laterality").text(laterality));
			}
			li.click((function(viewer, artifact) {
				return function() {
					viewer.showArtifact(artifact);
				}
			})(this.viewer, artifacts[i]));
			this.list.append(li);
			this.artifactItems[artifacts[i]['id']] = li;
			var timestamp = artifacts[i]['timestamp'];
			if (currentVisit != null && currentVisit.containsTime(timestamp)) {
				currentVisit.addArtifact(artifacts[i]);
			} else {
				currentVisit = new ArtifactTimeline.Visit(artifacts[i]);
				visits.push(currentVisit);
			}
			if (!(mostRecent > timestamp)) {
				mostRecent = timestamp;
			}
			var hardExudate = artifacts[i]['hardExudates'];
			var thickness = artifacts[i]['thickness'];
			if (hardExudate != null) {
				if (!hardExudates[laterality]) {
					hardExudates[laterality] = [];
				}
				console.log("Adding " + laterality + " HE point...");
				hardExudates[laterality].push(new Timeline.Graph.Point(timestamp, hardExudate));
			}
			if (thickness != null) {
				if (!thicknesses[laterality]) {
					thicknesses[laterality] = [];
				}
				console.log("Adding " + laterality + " thickness point...");
				thicknesses[laterality].push(new Timeline.Graph.Point(timestamp, thickness));
			}
		}
		this.hardExudateGraphs = {};
		this.thicknessGraphs = {};
		this.graphs = [];
		for (var l in hardExudates) {
			console.log("Creating " + l + " hard exudate graph for " + hardExudates[l].length + " points"); // HRC
			if (hardExudates[l].length > 0) {
				var g = new Timeline.Graph(hardExudates[l], 'rgb(204,153,0)');
				this.timeline.addGraph(g);
				this.hardExudateGraphs[l] = g;
				this.graphs.push(g);
			}
		}
		for (var l in thicknesses) {
			console.log("Creating " + l + " thickness graph for " + thicknesses[l].length + " points"); // CSF
			if (thicknesses[l].length > 0) {
				var g = new Timeline.Graph(thicknesses[l], 'rgb(153,51,204)');
				this.timeline.addGraph(g);
				this.thicknessGraphs[l] = g;
				this.graphs.push(g);
			}
		}
		// Now that we have the visits, we can generate the visit graph
		var visitGraph = [];
		for (var i = 0; i < visits.length; i++) {
			var p = new Timeline.PointGraph.Point(visits[i].date, visits[i].createIcon());
			p.onclick = (function(me, visit) {
				return function() {
					me.showVisit(visit);
				}
			})(this, visits[i]);
			visitGraph.push(p);
		}
		this.timeline.addGraph(this.visitGraph = new Timeline.PointGraph(visitGraph));
		console.log("Showing visits on timeline...");
		this.showVisitsOnTimeline();
		this.timeline.draw();
		if (currentVisit != null) {
			this.showVisit(currentVisit);
		}
		if (mostRecent != null)
			this.timeline.scrollTo(mostRecent, 1000);
	},
	_setVisitsVisibility: function(visible) {
		this.createKey(visible ? ArtifactTimeline.TYPE_KEY : ArtifactTimeline.GRAPH_KEY);
		this.showGraphs = !visible;
		if (this.showGraphs) {
			console.log("Showing graphs for laterality " + this.laterality);
			for (var l in this.hardExudateGraphs) {
				this.hardExudateGraphs[l].setVisible(l == this.laterality);
			}
			for (var l in this.thicknessGraphs) {
				this.thicknessGraphs[l].setVisible(l == this.laterality);
			}
		} else {
			this.graphs.forEach(function(g) {
				g.setVisible(false);
			});
		}
		this.visitGraph.setVisible(visible);
		this.timeline.draw();
	},
	showVisitsOnTimeline: function() {
		this._setVisitsVisibility(true);
		this.visitTab.addClass("active");
		this.graphTab.removeClass("active");
	},
	showGraphsOnTimeline: function() {
		this._setVisitsVisibility(false);
		this.visitTab.removeClass("active");
		this.graphTab.addClass("active");
	},
	showVisit: function(visit) {
		// Blank out the list
		this.artifactItems = {};
		this.list.empty();
		// And repopulate it
		var artifacts = visit.artifacts;
		for (var i = 0; i < artifacts.length; i++) {
			var li = $('<li/>').addClass("artifact " + artifacts[i]['type']);
			li.append($('<img/>').attr('src', 'artifact/thumbnail/' + artifacts[i]['id']));
			var laterality = artifacts[i]['laterality'];
			if (laterality) {
				li.append($('<div/>').addClass("laterality").text(laterality));
			}
			li.click((function(viewer, artifact) {
				return function() {
					viewer.showArtifact(artifact);
				}
			})(this.viewer, artifacts[i]));
			if (artifacts[i]['id'] == this.activeArtifactId) {
				li.addClass("active");
				this.activeArtifactItem = li;
			}
			this.list.append(li);
			this.artifactItems[artifacts[i]['id']] = li;
		}
	},
	setActiveArtifact: function(artifact) {
		if (this.activeArtifactItem) {
			this.activeArtifactItem.removeClass("active");
		}
		this.activeArtifactId = artifact['id'];
		this.activeArtifactItem = this.artifactItems[artifact['id']];
		if (this.activeArtifactItem)
			this.activeArtifactItem.addClass("active");
		// Change graphs to match the laterality of the active artifact
		this.laterality = artifact['laterality'];
		if (this.showGraphs) {
			// Update laterality
			this.showGraphsOnTimeline();
		}
		this.timeline.draw();
	},
	getMostRecent: function() {
		return this.artifacts.length == 0 ? null : this.artifacts[this.artifacts.length-1];
	},
	getAllOfType: function(type, laterality) {
		var rv = [];
		for (var i = 0; i < this.artifacts.length; i++) {
			if (this.artifacts[i]['type'] == type) {
				if (laterality && this.artifacts[i]['laterality'] == laterality)
					rv.push(this.artifacts[i]);
			}
		}
		return rv;
	},
	resized: function() {
		this.timeline.resized();
	}
}

ArtifactTimeline.Visit = function(artifact) {
	this.date = artifact['timestamp'];
	this.earliest = this.date;
	this.latest = this.date;
	this.artifacts = [ artifact ];
};

ArtifactTimeline.Visit.prototype = {
	containsTime: function(time) {
		// See if the time is within 24 hours of the middle time
		return time >= this.earliest-Timeline.MS_PER_DAY && time <= this.latest+Timeline.MS_PER_DAY;
	},
	createIcon: function() {
		"use strict";
		// Go through the artifacts and create a set of the types available.
		var types = {};
		this.artifacts.forEach(function(artifact) {
			types[artifact['type']] = true;
		});
		var icons = [];
		for (var t in ArtifactTimeline.ICONS) {
			if (t in types) {
				icons.push(ArtifactTimeline.ICONS[t]);
			}
		}
		if (icons.length == 0)
			return ArtifactTimeline.UNKNOWN_ICON;
		if (icons.length == 1)
			return icons[0];
		return new ArtifactTimeline.MultiEventIcon(icons);
	},
	addArtifact: function(artifact) {
		var d = artifact['timestamp'];
		if (d < this.earliest) {
			this.earliest = d;
		}
		if (this.latest < d) {
			this.latest = d;
		}
		// Move the official date to be the middle
		this.date = Math.round((this.latest - this.earliest) / 2) + this.earliest;
		this.artifacts.push(artifact);
	}
};

function Timeline(container) {
	this.container = $(container);
	var canvas = document.createElement('canvas');
	this.canvas = canvas;
	this.context = canvas.getContext('2d');
	this.$canvas = $(canvas);
	this.$canvas.addClass('timeline');
	this.container.append(this.$canvas);
	this.dayWidth = 1;
	this.visibleDate = new Date().getTime();
	this.headerHeight = 24;
	this.theme = Timeline.THEME;
	this.graphs = [];
	this.resized();
	$(window).resize((function(me) {
		var lastW = 0, lastH = 0;
		return function() {
			var w = $(window).width(), h = $(window).height();
			if (w != lastW || h != lastH) {
				lastW = w;
				lastH = h;
				me.resized();
			}
		}
	})(this));
	(function(me, $c) {
		var startX, startTime;
		$c.mousedrag({
			dragstart: function(event) {
				var p = $c.offset()
				startX = event.pageX - p.left;
				//startY = event.pageY - p.top;
				startTime = me.visibleDate;
				$c.addClass("dragging");
				return true;
			},
			drag: function(event) {
				var p = $c.offset();
				var x = event.pageX - p.left;
				//startY = event.pageY - p.top;
				// Convert the difference in x to a time, and set that time
				me.visibleDate = (startTime - (x - startX) * me.dayWidth * Timeline.MS_PER_DAY);
				me.draw();
			},
			dragstop: function(event) {
				$c.removeClass("dragging");
			}
		});
		$c.mousemove(function(event) {
			var p = $c.offset();
			var x = event.pageX - p.left;
			var y = event.pageY - p.top - me.headerHeight;
			for (var i = me.graphs.length-1; i >= 0; i--) {
				var g = me.graphs[i];
				if (g.visible && g.onmousemove && g.onmousemove(event, me, x, y))
					break;
			}
		});
		$c.click(function(event) {
			// Find x/y on the graph
			var p = $c.offset();
			var x = event.pageX - p.left;
			var y = event.pageY - p.top - me.headerHeight;
			for (var i = me.graphs.length-1; i >= 0; i--) {
				var g = me.graphs[i];
				if (g.visible && g.onclick && g.onclick(event, me, x, y))
					break;
			}
		});
	})(this, this.$canvas);
}

Timeline.MONTHS = [ 'Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec' ];
Timeline.DAYS_PER_MONTH = [ 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 ];
Timeline.MS_PER_DAY = 24*60*60*1000; // milliseconds in a day
Timeline.THEME = {
	"background": "rgb(64,64,64)",
	"border": "rgb(0,0,0)",
	"monthBorder": "rgb(83,83,83)",
	"header": {
		"top": "rgb(153,153,153)",
		"bottom": "rgb(64,64,64)",
		"font": "16px Myriad Pro, Myriad, Helvetica, sans-serif",
		"color": "rgb(255,255,255)",
		"shadow": {
			"offsetX": 0,
			"offsetY": 1,
			"blur": 3.0,
			"color": "rgb(0,0,0)"
		}
	}
};

Timeline.daysInYear = function(year) {
	return DateUtil.isLeapYear(year) ? 366 : 365;
};

Timeline.prototype = {
	draw: function() {
		var ctx = this.context, w = this.canvas.width, h = this.canvas.height;
		ctx.fillStyle = this.theme.background;
		ctx.fillRect(0, 0, w, h);
		var g = ctx.createLinearGradient(0, 0, 0, this.headerHeight);
		g.addColorStop(0, this.theme.header.top);
		g.addColorStop(1, this.theme.header.bottom);
		ctx.fillStyle = g;
		ctx.fillRect(0, 0, w, this.headerHeight);
		ctx.fillStyle = this.theme.border;
		ctx.fillRect(0, this.headerHeight, w, 1);
		// The visible date is the date at the center of window, so figure out
		// the date at the left
		var startDate = new Date();
		startDate.setTime(this.visibleDate - (Timeline.MS_PER_DAY * (w / 2)));
		// Figure out where the year would appear given the current start date
		var x = this.convertDateToX(new Date(startDate.getFullYear(), 0, 1));
		// First, draw the header borders...
		var yw, ty = this.headerHeight, th = h - ty;
		ty += th / 3;
		th = h - ty;
		var toQ = th / 2;
		var toM = th / 4 * 3;
		for (var year = startDate.getFullYear(); x < w; x += yw, year++) {
			var leap = DateUtil.isLeapYear(year);
			var days = 0;
			yw = (leap ? 366 : 365) * this.dayWidth;
			ctx.fillStyle = this.theme.monthBorder;
			for (var month = 1; month < 12; month++) {
				days += Timeline.DAYS_PER_MONTH[month];
				if (leap && month == 2)
					days++;
				var to = (month == 6 ? 0 : ((month % 3) == 0 ? toQ : toM));
				ctx.fillRect(Math.round(x + (days*this.dayWidth)), ty+to, 1, th-to);
			}
			ctx.fillStyle = this.theme.border;
			ctx.fillRect(Math.round(x), 0, 1, h);
		}
		// Then draw the year text
		ctx.font = this.theme.header.font;
		ctx.fillStyle = this.theme.header.color;
		ctx.textAlign = 'center';
		ctx.textBaseline = 'middle';
		if (this.theme.header.shadow) {
			ctx.shadowOffsetX = this.theme.header.shadow.offsetX;
			ctx.shadowOffsetY = this.theme.header.shadow.offsetY;
			ctx.shadowBlur = this.theme.header.shadow.blur;
			ctx.shadowColor = this.theme.header.shadow.color;
		}
		x = this.convertDateToX(new Date(startDate.getFullYear(), 0, 1));
		var y = this.headerHeight / 2;
		for (var year = startDate.getFullYear(); x < w; x += (Timeline.daysInYear(year) * this.dayWidth), year++) {
			ctx.fillText(year.toString(), x + (Timeline.daysInYear(year) * this.dayWidth) / 2, y);
		}
		// Kill shadows
		ctx.shadowOffsetX = 0;
		ctx.shadowOffsetY = 0;
		ctx.shadowBlur = 0;
		ctx.shadowColor = 'transparent black';
		// Transform the context so that 0 is the top of the header
		ctx.save();
		ctx.translate(0, this.headerHeight);
		h -= this.headerHeight;
		for (var i = 0; i < this.graphs.length; i++) {
			this.graphs[i].draw(this, ctx, w, h);
		}
		ctx.restore();
	},
	empty: function() {
		this.graphs = [];
	},
	convertDateToX: function(date) {
		date = DateUtil.toTime(date);
		return (date - this.visibleDate) / Timeline.MS_PER_DAY * this.dayWidth + (this.canvas.width / 2);
	},
	convertXToDate: function(x) {
		
	},
	addGraph: function(graph) {
		this.graphs.push(graph);
	},
	resized: function() {
		this.canvas.width = this.$canvas.width();
		this.canvas.height = this.$canvas.height();
		this.draw();
	},
	scrollTo: function(time, animationTime) {
		if (typeof this.animTimeout == 'number') {
			// Stop any active animation
			clearTimeout(this.animTimeout);
			this.animTimeout = null;
		}
		if (animationTime > 0) {
			var t = 1;
			var me = this;
			var diff = this.visibleDate - time;
			var start = this.visibleDate;
			var change = 1 / (animationTime / 50);
			function tick() {
				t -= change;
				if (t < 0) {
					// Finish
					me.visibleDate = time;
					me.draw();
					me.animTimeout = null;
				} else {
					var p = t<0.5?1-(t*t*2):(1-t)*(1-t)*2;
					// 1 - (t*t);
					me.visibleDate = start - (diff * p);
					me.draw();
					me.animTimeout = setTimeout(tick, 50);
				}
			}
			tick();
		} else {
			// Just jump
			this.visibleDate = time;
			this.draw();
		}
	}
};

Timeline.Graph = function(data, color) {
	this.color = color ? color : "rgb(255,255,255)";
	// Convert the input data into data we can use
	var max = data[0]['value'];
	var min = max;
	this.data = data.concat();
	for (var i = 0; i < data.length; i++) {
		var p = data[i].value;
		if (max < p)
			max = p;
		if (min > p)
			min = p;
	}
	if (max == min) {
		// Fake it so things are centered
		this.max = max + 0.5;
		this.min = min - 0.5;
	} else {
		this.max = max;
		this.min = min;
	}
	// For demo purposes:
	// Set the baseline to be 10% of the difference between min and max
	this.baseLine = (this.max-this.min) * 0.1 + this.min;
};

Timeline.Graph.prototype = {
	visible: true,
	setVisible: function(visible) {
		this.visible = visible;
	},
	draw: function(timeline, ctx, w, h) {
		if (!this.visible)
			return;
		var yo = 0;
		if (h > 6) {
			// Make sure the point stay in bounds
			h -= 6;
			yo = 3;
		}
		var gh = this.max - this.min, min = this.min;
		ctx.lineWidth = 1;
		ctx.strokeStyle = this.color;
		ctx.beginPath();
		// Stroke the baseline
		var y = h - ((this.baseLine - min) / gh) * h + yo;
		ctx.moveTo(0, y);
		ctx.lineTo(w, y);
		ctx.stroke();
		// And now do the graph itself
		ctx.beginPath();
		ctx.lineWidth = 2;
		var xs = new Array(this.data.length);
		var ys = new Array(this.data.length);
		for (var i = 0; i < this.data.length; i++) {
			var p = this.data[i];
			xs[i] = timeline.convertDateToX(p.date);
			ys[i] = h - ((p.value - min) / gh) * h + yo;
			if (i == 0) {
				ctx.moveTo(xs[i], ys[i]);
			} else {
				ctx.lineTo(xs[i], ys[i]);
			}
		}
		ctx.stroke();
		ctx.fillStyle = this.color;
		// Now draw the markers at each point
		for (var i = 0; i < xs.length; i++) {
			ctx.beginPath();
			ctx.arc(xs[i], ys[i], 3, 0, Math.PI*2);
			ctx.fill();
		}
		this.xs = xs;
		this.ys = ys;
	},
	findPointUnder: function(x, y) {
		if (!this.xs)
			return null;
		var padding = 3;
		var minX = x - padding, maxX = x + padding;
		var minY = y - padding, maxY = y + padding;
		for (var i = this.xs.length-1; i >= 0; i--) {
			if (minX <= this.xs[i] && this.xs[i] <= maxX &&
					minY <= this.ys[i] && this.ys[i] <= maxY)
				return this.data[i];
		}
		return null;
	},
	onmousemove: function(event, timeline, x, y) {
		var p = this.findPointUnder(x, y);
		if (p == null) {
			if (this.popup && this.popup.isVisible()) {
				// Kill the popup.
				this.popup.hide();
				this.popupPoint = null;
				this.popup = null;
			}
		} else {
			if (p == this.popupPoint) {
				if (this.popup.isVisible())
					return;
			}
			this.popupPoint = p;
			if (this.popup) {
				this.popup.hide();
			}
			this.popup = new Popup(p.value, event.pageX-16, event.pageY-24);
			return;
		}
	},
	toString: function() {
		return "[Timeline.Graph: color=" + this.color + ", data=" + this.data.length + "]";
	}
};

Timeline.Graph.Point = function(date, value) {
	this.date = DateUtil.toTime(date);
	this.value = value;
};

Timeline.PointGraph = function(data) {
	this.data = data;
	this.data.sort(function(a,b) {
		return a.date < b.date ? -1 : (a.date == b.date ? 0 : 1);
	});
}

Timeline.PointGraph.prototype = {
	visible: true,
	setVisible: function(visible) {
		this.visible = visible;
	},
	draw: function(timeline, ctx, w, h) {
		if (!this.visible)
			return;
		var y = h/2;
		this.py = y;
		for (var i = 0; i < this.data.length; i++) {
			var p = this.data[i];
			p.draw(ctx, timeline.convertDateToX(p.date), y);
		}
	},
	findPointUnder: function(timeline, x, y) {
		for (var i = this.data.length-1; i >= 0; i--) {
			var p = this.data[i];
			var px = timeline.convertDateToX(p.date);
			if (p.contains(x-px,y-this.py)) {
				return p;
			}
		}
		return null;
	},
	onclick: function(event, timeline, x, y) {
		// See if any of our points are under that x,y
		var p = this.findPointUnder(timeline, x, y);
		if (p != null && p.onclick)
			p.onclick();
	},
	onmousemove: function(event, timeline, x, y) {
		var p = this.findPointUnder(timeline, x, y);
		if (p == null) {
			if (this.popup && this.popup.isVisible()) {
				// Kill the popup.
				this.popup.hide();
				this.popupPoint = null;
				this.popup = null;
			}
		} else {
			if (p == this.popupPoint) {
				if (this.popup.isVisible())
					return;
			}
			this.popupPoint = p;
			if (this.popup) {
				this.popup.hide();
			}
			this.popup = new Popup(p.label, event.pageX-16, event.pageY-24);
			return;
		}
	}
};

Timeline.PointGraph.Point = function(date, icon, label) {
	if (icon == null)
		throw Error;
	this.date = DateUtil.toTime(date);
	this.icon = icon;
	if (!label) {
		var d = new Date(this.date);
		this.label = (d.getMonth() + 1) + "/" + d.getDate() + "/" + d.getFullYear();
	}
};

Timeline.PointGraph.Point.prototype = {
	draw: function(ctx, x, y) {
		this.icon.draw(ctx, x, y);
	},
	/**
	 * Determines if a given set of coordinates are within this point. The
	 * coordinates are relative to the center of the point, so 0,0 would mean
	 * dead-center on the point, while -2,-5 would mean above and to the left.
	 */
	contains: function(x, y) {
		return this.icon.contains(x,y);
	}
};

Timeline.EventIcon = function(size, color) {
	this.size = size/2;
	this.color = color;
};

Timeline.EventIcon.prototype = {
	draw: function(ctx, x, y) {
		ctx.save();
		ctx.fillStyle = this.color;
		ctx.shadowOffsetX = 1;
		ctx.shadowOffsetY = 1;
		ctx.shadowBlur = 2;
		ctx.shadowColor = 'rgb(0,0,0)';
		ctx.beginPath();
		ctx.arc(x, y, this.size, 0, Math.PI*2);
		ctx.fill();
		// Kill the shadow
		ctx.shadowColor = 'rgba(0,0,0,0)';
		// Create the gradient for this point
		var g = ctx.createLinearGradient(x - this.size, y - this.size, x + this.size, y + this.size);
		g.addColorStop(0, 'rgba(255,255,255,0.5)');
		g.addColorStop(0.5, 'rgba(255,255,255,0)');
		g.addColorStop(0.5, 'rgba(0,0,0,0)');
		g.addColorStop(1, 'rgba(0,0,0,0.5)');
		ctx.fillStyle = g;
		// And draw it over the path
		ctx.fill();
		ctx.restore();
	},
	contains: function(x, y) {
		return Math.abs(x) <= this.size && Math.abs(y) <= this.size;
	}
};

ArtifactTimeline.ICONS = {
	'oct': new Timeline.EventIcon(10, 'rgb(67,168,224)'),
	'fundus': new Timeline.EventIcon(10, 'rgb(235,98,44)'),
	'fluoro': new Timeline.EventIcon(10, 'rgb(127,169,27)'),
	'scanning': new Timeline.EventIcon(10, 'rgb(177,50,198)')
};
ArtifactTimeline.UNKNOWN_ICON = new Timeline.EventIcon(10, 'rgb(192,192,192)');

ArtifactTimeline.MultiEventIcon = function(icons, padding) {
	if (arguments.length < 2)
		padding = 2;
	this.icons = icons;
	this.padding = padding;
	var h = 0;
	var s = 0;
	for (var i = 0; i < icons.length; i++) {
		var z = icons[i].size;
		if (s < z)
			s = z;
		h += z;
		if (i > 0)
			h += padding;
	}
	this.width = s;
	this.height = h;
	this.halfHeight = h/2;
};

ArtifactTimeline.MultiEventIcon.prototype = {
	draw: function(ctx, x, y) {
		for (var i = 0; i < this.icons.length; i++) {
			this.icons[i].draw(ctx, x, y);
			y += this.icons[i].size * 2 + this.padding;
		}
	},
	contains: function(x, y) {
		return Math.abs(x) <= this.width && Math.abs(y) <= this.height;
	}
};