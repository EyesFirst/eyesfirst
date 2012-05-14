(function ($) {

AjaxSolr.RangeFacetWidget = AjaxSolr.AbstractFacetWidget.extend({
	
	currentQuery : null, 
	
	update : function (){		
    return this.changeSelection(function () {   
    	var query;
    	var currentQuery = this.currentQuery;
    	this.manager.store.removeByValue('fq', currentQuery);
    	
    	currentQuery = "{!tag="+this.field+" df="+this.field+"}";
    	$.each($(this.target).find(':checkbox'), function(i,input){
    		if(input.checked){
    			currentQuery = currentQuery+"["+ input.value +" TO " + input.getAttribute("data-end") +"] ";
    		}
    	});

    	this.currentQuery = currentQuery;
    	
    	if(this.currentQuery == "{!tag="+this.field+" df="+this.field+"}"){
    		return;
    	}
    	
      return this.manager.store.addByValue('fq',currentQuery.trim());
    });
	},
	
  afterRequest: function () {
    var respFacet = this.manager.response.facet_counts.facet_ranges[this.field];
    var target = $(this.target);
    var self = this;
    var gap = parseInt(respFacet.gap) - 1;
    var facetEnd;
    var checked;
    var objectedItems = [];
    
    self.currentQuery = null;
    
    var values = self.manager.store.values('fq');
    for(var i = 0; i < values.length; i++){
    	if(values[i].indexOf("{!tag="+this.field+" df="+this.field+"}") > -1){
    		self.currentQuery = values[i];
    	}
    }
    
    $.each(respFacet.counts,function(facetStart,count) {
      facetEnd = parseInt(facetStart) + gap;
      checked = self.currentQuery !== null && self.currentQuery.indexOf("["+facetStart+" TO "+facetEnd+"]") > -1 ? 'checked="checked"' : '';
      objectedItems.push({facetStart:facetStart,count:count,facetEnd:facetEnd,checked:checked});
    });
    if (respFacet.after !== undefined) {
    	var facetStart = respFacet.end;
      checked = self.currentQuery !== null && self.currentQuery.indexOf("["+facetStart+" TO *]") > -1 ? 'checked="checked"' : '';
      objectedItems.push({facetStart:facetStart, facetEnd:"*", count: parseInt(respFacet.after), checked:checked});
    }

    target.empty();
    $(this.template).tmpl({field:this.field, facets: objectedItems, title: this.title}).appendTo(this.target);
    
    $('#'+this.field).click(function() {
    	$.each($(self.target).find(':checkbox'),function(i, input) {
        input.checked = '';
      });
    	self.update();
    	self.manager.doRequest(0);
    });
    
    $(this.target).find(':checkbox').click(function(){
    	self.update();
    	self.manager.doRequest(0);
    });
  }
});

})(jQuery);
