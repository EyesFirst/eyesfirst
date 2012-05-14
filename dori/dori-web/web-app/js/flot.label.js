/**
 * A plugin for displaying axes titles on Flot charts. (Called "titles" to
 * clarify the difference between them and "tick labels.")
 * <p>
 * Currently you can only label then <em>entire</em> set of axes (only
 * options.xaxis and options.yaxis are used), and they must be on the left and
 * bottom of the graph.
 */

(function($){
	// Quick one-off jQuery plugin for rotating text (used by the label hack)

	$.fn.rotate = function(deg) {
		var css3 = "rotate(" + deg + "deg)";
		this.css({
			"-moz-transform": css3,
			"-o-transform": css3,
			"-webkit-transform": css3
		});
	};

	// This says it's a plugin, but we're really lying, because actually using
	// the flot plugin API is nearly impossible for this. So, step 1: CHEAT! By
	// hijacking the flot function.
	var oldplot = $['plot'];

	$['plot'] = function(container, data, options) {
		container = $(container);
		if (options) {
			var padx = 0, pady = 0, xtitle = null, ytitle = null;
			if ('xaxis' in options && 'title' in options['xaxis']) {
				// Temporarily append the x-axis title...
				xtitle = $('<div/>')
						.text(options['xaxis']['title'])
						.css("font-size", "85%");
				$(container).append(xtitle);
				pady = xtitle.height();
				xtitle.detach();
			}
			if ('yaxis' in options && 'title' in options['yaxis']) {
				// Temporarily append the y-axis title...
				ytitle = $('<div/>')
						.text(options['yaxis']['title'])
						.css("font-size", "85%");
				$(container).append(ytitle);
				padx = ytitle.height();
				ytitle.detach();
			}
			if (padx + pady > 0) {
				var realContainer = container;
				container = $("<div/>").css({
					"width": realContainer.width() - padx,
					"height": realContainer.height() - pady
				});
				realContainer.append(container);
				realContainer.css({
					"padding-left": padx,
					"padding-bottom": pady
				});
			}
			// Actually call flot now
			var res = oldplot(container, data, options);
			if (xtitle) {
				// x-axis label hack.
				var offset = res.getPlotOffset();
				xtitle.css({
					"position": "absolute",
					"width": res.width() + "px",
					"text-align": "center",
					"left": offset.left + "px",
					"top": (res.height() + offset.top + offset.bottom) + "px"
				});
				$(container).append(xtitle);
			}
			if (ytitle) {
				// y-axis label hack.
				var offset = res.getPlotOffset();
				ytitle.css({
					"position": "absolute",
					"width": res.height() + "px",
					"text-align": "center",
					"top": (res.height()/2) + "px"
				});
				$(container).append(ytitle);
				ytitle.css("left", -(ytitle.width()/2 + ytitle.height()));
				// Left depends on how wide it is
				ytitle.rotate(270);
			}
			return res;
		} else {
			return oldplot(container, data, options);
		}
	}
	// Copy the plugin reference:
	$['plot']['plugins'] = oldplot['plugins'];
	// This should allow plugins added afterwards to still hook into flot
})(jQuery);