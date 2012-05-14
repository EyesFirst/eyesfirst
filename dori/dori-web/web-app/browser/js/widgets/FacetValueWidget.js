(function ($) {

AjaxSolr.FacetValueWidget = AjaxSolr.AbstractFacetWidget.extend({
	
  afterRequest: function () {
    var self = this;
    var respFacet = this.manager.response.facet_counts.facet_fields[this.field];
    var target = $(this.target);

    var maxCount = 0;
    var objectedItems = [];
    for (var facet in respFacet) {
      var count = parseInt(respFacet[facet]);
      if (count > maxCount) {
        maxCount = count;
      }
      var checked = this.manager.store.find('fq', this.field + ':' + facet) ? 'checked="checked"' : '';
      var label;
      
      if(this.labels != undefined && this.labels[facet] != undefined){
      	label = this.labels[facet];
      } else {
      	label = facet;
      }
      
      objectedItems.push({ name: facet, count: count, label: label, checked: checked});
    }

    target.empty();
    $(this.template).tmpl({field:this.field, facets: objectedItems, title: this.title}).appendTo(this.target);
    
    $('#'+this.field).click(function() {
    	for(var facet in respFacet){
    		self.remove(facet);
    	}
    	self.manager.doRequest(0);
    });
    
    $(this.target).find(':checkbox').click(function(){
    	if (this.checked){
    		self.add(this.value);
		} else {
			self.remove(this.value);
		}
    	self.manager.doRequest(0);
    });
    
  }
});
})(jQuery);
