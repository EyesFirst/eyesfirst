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
if (console.error) {
  jQuery.error = console.error;
}

$(document).ajaxError(function(e, jqxhr, settings, exception) {
  //TODO if authentication failure, do something else.
  alert(alert('An error occured. Try reloading the page.'));
});

//-- INITIALIZE NON-AJAX-SOLR STUFF
var DoriGlobals;
$.ajax({
  url: '../info/globals',
  async: false,//this is why we call $.ajax instead of $.getJSON
  dataType: "json",
  success: function(data) {
    DoriGlobals = data;
  }
});
$(document).ready(function() {
  $('#headerNavTemplate').tmpl(DoriGlobals).appendTo('#headerNav');
  if (DoriGlobals.internal) {
    document.title = 'i-DORI Browser';
    $('h1 em').html('i-DORI');
  }
});

//-- INITIALIZE AJAX-SOLR STUFF
var Manager;//initialized below
$(document).ready(function() {
  Manager = new AjaxSolr.Manager({
    solrUrl: DoriGlobals.solrCoreUrl,
    servlet: DoriGlobals.solrCoreRequestHandler
  });
  Manager.addWidget(new AjaxSolr.ResultWidget({
    id: 'docs',
    target: '#docs'
  }));
  Manager.addWidget(new AjaxSolr.PagerWidget({
    id: 'pagination',
    target: '#pagination',
    prevLabel: '&lt;',
    nextLabel: '&gt;',
    innerWindow: 1,
    renderLinks: function (links) {
      if (this.totalPages) {
        links.unshift(this.pageLinkOrSpan(this.previousPage(), [ 'pager-disabled', 'pager-prev' ], this.prevLabel));
        links.push(this.pageLinkOrSpan(this.nextPage(), [ 'pager-disabled', 'pager-next' ], this.nextLabel));

        var self = this;
        var target = $(this.target);
        $(links).each(function(i,link) {
          target.append(link).append(self.separator);
        });
      }
    },
    renderHeader: function (perPage, offset, total) {
      var obj = {perPage:perPage, offset:offset, total:total};
      $('#paginationHeaderTemplate').tmpl(obj).appendTo(this.target);
    }
  }));

  Manager.addWidget(new AjaxSolr.FacetValueWidget({
    id: 'gender',
    target: '#genderFilter',
    template: '#valueFilterTemplate',
    field: 'pat_gender',
    title: 'Patient Gender',
    labels: {'F':'Female', 'M':'Male', 'O':'Other/Unspecified'}
  }));

  Manager.addWidget(new AjaxSolr.FacetValueWidget({
    id: 'laterality',
    target: '#lateralityFilter',
    template: '#valueFilterTemplate',
    field: 'laterality',
    title: 'Laterality',
    labels: {'L':'Left', 'R':'Right'}
  }));

  Manager.addWidget(new AjaxSolr.FacetValueWidget({
    id: 'modality',
    target: '#modalityFilter',
    template: '#valueFilterTemplate',
    field: 'modality',
    title: 'Modality'
  }));
  Manager.addWidget(new AjaxSolr.FacetValueWidget({
    id: 'efid_issuer_name',
    target: '#issuerFilter',
    template: '#valueFilterTemplate',
    field: 'efid_issuer_name',
    title: 'EFID Issuer'
  }));

  Manager.addWidget(new AjaxSolr.RangeFacetWidget({
    id: 'age',
    target: '#ageFilter',
    template: '#rangeFilterTemplate',
    field: 'study_pat_age_years',
    title: 'Patient Age'
  }));

  Manager.addWidget(new AjaxSolr.CurrentFilterWidget({
    id: 'search',
    target: '#search'
  }));
  Manager.init();
  var params = {
    echoParams: 'none',
    rows: 20,
    facet: true,
    'facet.limit': 20,
    'facet.sort':'lex',
    'facet.mincount': 0,
    'json.nl': 'map'
  };
  for (var name in params) {
    Manager.store.addByValue(name, params[name]);
  }
  //Manager.store.addByValue('q', '*:*');
  Manager.doRequest();

  $('#clear_all').click(function(){
    Manager.store.remove('fq');
    Manager.store.remove('q');
    Manager.doRequest();
  });
});

/** Returns an integer for the patient, based on the patient id. */
function getPatientNumFromId(pat_id) {
  var EFREGEX = /^EF[A-Z0-9]{8}[0-9]$/;
  var pId = null;
  if (pat_id.match(EFREGEX)) {
    pId = parseInt(pat_id.slice(2,-1));
  } else if (pat_id == parseInt(pat_id)) {
    pId = pat_id
  }
  return pId;
}

function getViewerParamStringFromDoc(doc) {
	return 'studyUID='+doc.study_iuid+'&seriesUID='+doc.series_iuid+'&objectUID='+doc.sop_iuid;
}

function getWadoQueryParamStringFromDoc(doc) {
  var age = doc.study_pat_age_years || '?';
  var lat = doc.laterality || '?';
  var patientString = doc.pat_id+'/'+age+'/'+lat+'/'+ doc.modality +'/'+doc.inst_pk;
  return 'requestType=WADO&contentType=application/dicom&useOrig=true&studyUID='+doc.study_iuid+
            '&seriesUID='+doc.series_iuid+'&objectUID='+doc.sop_iuid+
            '&filename='+encodeURIComponent(patientString.replace(/\//g,'-'))+'.dcm';
  
}