(function ($) {

AjaxSolr.ResultWidget = AjaxSolr.AbstractWidget.extend({
  beforeRequest: function () {
    $(this.target).html($('<img/>').attr('src', 'images/ajax-loader.gif'));
  },

  //facet_values can be a single value or an array
  facetLinks: function (facet_field, facet_values) {
    var links = [];
    if (facet_values) {
      if (!AjaxSolr.isArray(facet_values))
        facet_values = new Array(facet_values);
      for (var i = 0, l = facet_values.length; i < l; i++) {
//        links.push(AjaxSolr.theme('facet_link', facet_values[i], this.facetHandler(facet_field, facet_values[i])));
      }
    }
    return links;
  },

  facetHandler: function (facet_field, facet_value) {
    var self = this;
    return function () {
      self.manager.store.remove('fq');
      self.manager.store.addByValue('fq', facet_field + ':' + facet_value);
      self.manager.doRequest(0);
      return false;
    };
  },

  afterRequest: function () {
    $(this.target).empty();
    $("#docsTemplate").tmpl({docs:this.manager.response.response.docs}).appendTo(this.target);
    render_identicon_canvases('identicon ');//defined in js/identicon_canvas.js
  },

  init: function () {

  }
});

})(jQuery);