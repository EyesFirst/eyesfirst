function EyesFirst() { }

/**
 * Creates HTML to copy over all stylesheets from this page to the given other
 * page.
 */
EyesFirst.copyCSSLinks = function(doc) {
	if (!doc.writeln) {
		doc.writeln = function() { doc.write.apply(this, arguments); }
	}
	// Copy stylesheets over
	$('link').each(function() {
		if (this.getAttribute('rel') == 'stylesheet') {
			// Clone that to the new page (as best as we can)
			doc.write('<link');
			for (var i = 0; i < this.attributes.length; i++) {
				doc.write(' ');
				doc.write(this.attributes[i].name);
				doc.write('="');
				doc.write(EyesFirst.escapeHTML(this.attributes[i].value));
				doc.write('"');
			}
			doc.writeln('>');
		}
	});
};

/**
 * Escape HTML entities. Why is this not a standard part of the JavaScript
 * API by now?
 */
EyesFirst.escapeHTML = function(text) {
	if (text == null)
		return 'null';
	if (typeof text != 'string')
		text = text.toString();
	return text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
};