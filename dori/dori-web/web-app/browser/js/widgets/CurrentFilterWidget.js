(function ($) {

AjaxSolr.CurrentFilterWidget = AjaxSolr.AbstractTextWidget.extend({
  init: function () {
    var self = this;    
    $(this.target).bind('keydown', function(e) {
      if (e.which == 13) {
        var value = $(this).val();
        if (self.set(value)) {
          self.manager.doRequest(0);
        }
      }
    });
  },
  
  afterRequest: function () {
    //in case q was cleared or possibly changed
    var v = this.manager.store.get('q').value || '';
    $(this.target).val(v);
  }

});

})(jQuery);
