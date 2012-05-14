(function ($) {

AjaxSolr.CurrentSearchWidget = AjaxSolr.AbstractWidget.extend({
  afterRequest: function () {
    var self = this;
    var links = [];

    var fq = this.manager.store.values('fq');
    for (var i = 0, l = fq.length; i < l; i++) {
      if (fq[i].match(/[\[\{]\S+ TO \S+[\]\}]/)) {
        var field = fq[i].match(/^\w+:/)[0];
        var value = fq[i].substr(field.length + 1, 10);
        links.push($('<a href="#"/>').text('(x) ' + field + value).click(self.removeFacet(fq[i])));
      }
      else {
        links.push($('<a href="#"/>').text('(x) ' + fq[i]).click(self.removeFacet(fq[i])));
      }
    }
    var qParam = this.manager.store.get('q');
    if (qParam.value) {
      links.push($('<a href="#"/>').text('(x) search: ' + qParam.value).click(self.removeQuery()));
    }

    if (links.length > 1) {
      links.unshift($('<a href="#"/>').text('remove all').click(function () {
        self.manager.store.remove('fq');
        self.manager.store.remove('q');
        self.manager.doRequest(0);
        return false;
      }));
    }

    if (links.length) {
      AjaxSolr.theme('list_items', this.target, links);
    }
    else {
      $(this.target).html('<div>(no filters)</div>');
    }
  },

  removeFacet: function (facet) {
    var self = this;
    return function () {
      if (self.manager.store.removeByValue('fq', facet)) {
        self.manager.doRequest(0);
      }
      return false;
    };
  },

  removeQuery: function () {
    var self = this;
    return function () {
      self.manager.store.remove('q');
      self.manager.doRequest(0);
      return false;
    };
  }
});

})(jQuery);
