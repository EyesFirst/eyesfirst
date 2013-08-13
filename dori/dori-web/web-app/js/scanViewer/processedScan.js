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
/**
 * JavaScript for displaying the UI for the processed scan.
 */

/**
 * List of diagnoses to display in the processed view.
 * FIXME: I'm starting to think that it would make more sense to just move the
 * chart data here in the first place. - dpotter
 */
DICOMViewer.PROCESSED_DIAGNOSES = {
	abnormal_thickness: {
		name: "Retinal Thickness Classifier",
		field: 'affirmAbnormalRetinalThickness',
		chart: { url: 'data/abnormal_thickness.json', width: 320, height: 240 }
	},
	hard_exudates: {
		name: 'Hard Exudate Classifier',
		field: 'affirmHardExudates',
		chart: { url: 'data/hard_exudates.json', width: 320, height: 240 },
		table: { data: [
				[ 1, '0.47', '1.00' ],
				[ 2, '0.07', '0.96' ],
				[ 3, '0.03', '0.84' ],
				[ 4, '0.00', '0.80' ]
			],
			columnHeaders: [ "K", "P(N\u2265K|Healthy)", "P(N\u2265K|Has HE)" ]
		}
	},
	notes: {
		field: 'processedNotes'
	}
};

/**
 * @constructor
 * @param container
 */
function ProcessedFeedbackUI(container, dicomURL, viewer) {
	this._viewer = viewer;
	var ui = $('<div class="processed-diagnoses"></div>');
	container.append(ui);
	var section = $('<section class="machine-interpretation"></section>');
	section.append('<h1>Machine Interpretation:</h1>');
	this._diagnoses = {};
	for (var d in DICOMViewer.PROCESSED_DIAGNOSES) {
		var diag = DICOMViewer.PROCESSED_DIAGNOSES[d];
		var name = diag.name;
		var subsect = $('<section/>').addClass('machine-' + d);
		section.append(subsect);
		this._diagnoses[d] = { section: subsect };
		subsect.append(this._diagnoses[d].title = $('<h2/>').text(name));
		if (diag.chart) {
			var diagObj = this._diagnoses[d];
			this._createChart(diag.chart, subsect, this._diagnoses[d]);
			diagObj.showPDvPFA = (function(div) {
				var pddiv = $('<div/>');
				var pfadiv = $('<div/>');
				div.append(pddiv);
				div.append(pfadiv);
				return function(pd, pfa) {
					pfadiv.text("The probability of a false positive is " + (100*pfa).toFixed(2) + "%.");
					pddiv.text("The probability of a false negative is " + (100*(1-pd)).toFixed(2) + "%.");
				};
			})(subsect);
			diagObj.showScore = (function(subsect) {
				var scoreDiv = $('<div/>');
				subsect.append(scoreDiv);
				return function(score) {
					scoreDiv.text("Score: " + score.toFixed(2));
				};
			})(subsect);
		}
		if (diag.table) {
			var table = $('<table/>').addClass(d + "-table data-table");
			var row;
			if (diag.table.columnHeaders) {
				row = $('<tr/>');
				table.append(row);
				for (var i = 0; i < diag.table.columnHeaders.length; i++) {
					row.append($('<th/>').text(diag.table.columnHeaders[i]));
				}
			}
			for (var r = 0; r < diag.table.data.length; r++) {
				var rowData = diag.table.data[r];
				row = $('<tr/>');
				table.append(row);
				for (var c = 0; c < rowData.length; c++) {
					row.append($('<td/>').text(rowData[c]))
				}
			}
			subsect.append(table);
		}
	}
	ui.append(section);
	new ClinicianReviewUI(section, '', 'Clinician Review of Machine Interpretation:',
			DICOMViewer.PROCESSED_DIAGNOSES_URI,
			'processedQueryString', dicomURL,
			DICOMViewer.PROCESSED_DIAGNOSES, "Agree", "Disagree");
	// Now that the UI is there, go ahead and get the machine info.
	var me = this;
	$.ajax({
		url: DICOMViewer.PROCESSED_DIAGNOSIS_URI + encodeURIComponent(dicomURL),
		dataType: 'json',
		error: function(jqXHR, textStatus, errorThrown) {
			// TODO: Display this error condition.
			console.log("Error loading machine diagnostics.");
		},
		success: function(data, textStatus, jqXHR) {
			// Build the display now that we have everything.
			me._showMachineDiagnostic(data);
		},
		//timeout: 60, // timeout
		type: 'GET'
	});
}

ProcessedFeedbackUI.prototype = {
	_showMachineDiagnostic: function(data) {
		if ('abnormalThickness' in data) {
			if (!('abnormal_thickness' in this._diagnoses)) {
				this._diagnoses['abnormal_thickness'] = { };
			}
			var at = data.abnormalThickness;
			var myAT = this._diagnoses['abnormal_thickness'];
			if ('jointAnomStat' in at) {
				myAT.score = at.jointAnomStat;
				if ('showScore' in myAT) {
					myAT.showScore(myAT.score);
				}
			}
			this._plotPDvPFA(at, myAT);
		}
		if ('hardExudates' in data) {
			// Add the hard exudate data
			if (!('hard_exudates' in this._diagnoses)) {
				this._diagnoses['hard_exudates'] = { };
			}
			var he = data['hardExudates'];
			var myHE = this._diagnoses['hard_exudates'];
			//this._plotPDvPFA(he, myHE);
			myHE.showPDvPFA(he.pd, he.pfa)
			he = he['hardExudates'];
			if (he.length == 0) {
				myHE.section.append($('<div/>').text("Classifier did not find any hard exudates."));
				return;
			}
			var exudates = myHE.exudates = [];
			var list = $('<ul/>');
			myHE.section.append(list);
			for (var i = 0; i < he.length; i++) {
				exudates[i] = new HardExudate(he[i]);
			}
			// Send these to the slice manager
			// FIXME: Need to send it in a way that doesn't use a private member
			this._viewer._sliceManager.setHardExudates(exudates);
			// Sort by normal score (for now)
			exudates.sort(function(a, b) {
				return b.normalScore - a.normalScore;
			});
			for (var i = 0; i < he.length; i++) {
				var li = $('<li><a href="#">Exudate ' + (i+1) + '</a> (' + exudates[i].normalScore.toFixed(2) + ')</li>');
				list.append(li);
				li.find('a').click((function(me, exudate) {
					return function(event) {
						// Coordinates are in order columns, layers, rows
						// (or fast time, slow time, axial)
						me._viewer._sliceManager.selectHardExudate(exudate);
						me._viewer.showSlice(exudate.ellipseCenter.z);
						return false;
					}
				})(this, exudates[i]));
			}
			myHE['onchartready'] = function(charts) {
				var count = he.length + 0.5; // Center it by adding 0.5 (bars run from n to n+1)
				for (var i = 0; i < charts.length; i++) {
					var chart = charts[i];
					// Get the chart's data
					var cd = chart.getData();
					// Add in the new point
					cd.push({
						'color': 'rgb(255,0,0)',
						'data': [ [ count, 0 ] ],
						'lines': { show: false },
						'points': { show: true, symbol: 'cross' },
						'label': 'This Scan'
					});
					chart.setData(cd);
					chart.setupGrid();
					chart.draw();
				}
			};
			if (myHE.charts) {
				myHE['onchartready'](myHE.charts);
			}
		} else {
			this._diagnoses['hard_exudates'].section.append("<div>No hard exudate information provided</div>");
		}
	},
	_plotPDvPFA: function(data, obj, label) {
		if (!label) {
			label = 'This Scan';
		}
		if ('pfa' in data && 'pd' in data) {
			console.log("Charting point at " + data.pfa + "," + data.pd);
			obj['onchartready'] = function(charts) {
				for (var i = 0; i < charts.length; i++) {
					var chart = charts[i];
					// Get the chart's data
					var cd = chart.getData();
					// Add in the new point
					cd.push({
						'color': 'rgb(255,0,0)',
						'data': [ [ data.pfa, data.pd ] ],
						'lines': { show: false },
						'points': { show: true },
						'label': label
					});
					chart.setData(cd);
					chart.setupGrid();
					chart.draw();
				}
			};
			if (obj.charts) {
				console.log("Have chart, invoking immediately.");
				// If the chart is already there, invoke immediately.
				obj['onchartready'](obj.charts);
			}
			if (obj.showPDvPFA) {
				obj.showPDvPFA(data.pd, data.pfa);
			}
		}
	},
	_createChart: function(chart, container, obj) {
		if (!chart.width)
			chart.width = 512;
		if (!chart.height)
			chart.height = 384;
		// Immediately create a container container...
		var div = $('<div/>').addClass('charts');
		container.append(div);
		container = div;
		//var me = this;
		$.ajax({
			url: chart.url,
			dataType: 'json',
			error: function(jqXHR, textStatus, errorThrown) {
				container.text("Unable to load chart. Server said: " + jqXHR.status + " " + jqXHR.statusText);
			},
			success: function(data, textStatus, jqXHR) {
				// As of now, the data returned can be an array.
				if (!('length' in data)) {
					// Convert to single element array
					data = [ data ];
				}
				var i, div, placeholder;
				var charts = new Array(data.length);
				for (i = 0; i < data.length; i++) {
					// Create the chart div
					div = $('<div/>').css('float', 'left');
					container.append(div);
					if (data[i]['title']) {
						div.append($('<h3/>').addClass("chart-title").text(data[i]['title']));
					}
					// Create the chart placeholder
					placeholder = $('<div/>').addClass("chart").css({"width": chart.width, "height": chart.height});
					div.append(placeholder);
					charts[i] = $.plot(placeholder, data[i]['data'], data[i]['options']);
				}
				// And finally:
				container.append($('<div/>').css('clear', 'left'));
				if (charts.length == 1)
					obj.chart = charts[0];
				obj.charts = charts;
				if ('onchartready' in obj) {
					console.log("Chart now ready, invoking callback");
					obj['onchartready'](obj.charts);
				}
			},
			//timeout: 60, // timeout
			type: 'GET'
		});
	},
	_showClinicianAnswers: function() {
		ul = $('<ul class="answer-list"></ul>');
		section.append(ul);
		this._answers = {};
		for (var d in DICOMViewer.PROCESSED_DIAGNOSES) {
			var diag = DICOMViewer.PROCESSED_DIAGNOSES[d];
			ul.append(DICOMViewer.createAnswer(d, diag.name));
		}
	}
}
