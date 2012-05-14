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
(function ($) {

//var WADOPATH = "http://eyesfirst.mitre.org/wado"
var WADOPATH = "wado.groovy";
var EFREGEX = /^EF[A-Z0-9]{8}[0-9]$/;

AjaxSolr.theme.prototype.result = function (doc, snippet) {
  var age = doc.study_pat_age_years || '?';
  var lat = doc.laterality || '?';
  //TODO instead of inst_pk, put mm-DDThh:mm:ss
  //line 1
  var pId = null;
  if (doc.pat_id.match(EFREGEX)) {
    pId = parseInt(doc.pat_id.slice(2,-1));
  } else if (doc.pat_id == parseInt(doc.pat_id)) {
    pId = doc.pat_id
  }
  //<canvas title="identicon '+pId+'" width="21" height="21"></canvas>
  var output = '<div><p>'
  if (pId != null)
    output += '<canvas title="identicon '+pId+'" width="21" height="21"></canvas>&nbsp;';
  var patientString = doc.pat_id+'/'+age+'/'+lat+'/'+ doc.modality +'/'+doc.inst_pk;
  output += patientString.htmlEscape()+' ';

  output += '<a href="'+WADOPATH+
            '?requestType=WADO&contentType=application/dicom&useOrig=true&studyUID='+doc.study_iuid+
            '&seriesUID='+doc.series_iuid+'&objectUID='+doc.sop_iuid+
            '&filename='+encodeURIComponent(patientString.replace(/\//g,'-'))+'.dcm">';
  output +=   '<img border="0" title="Download DICOM file" alt="icon" src="images/save.gif" />';
  if (doc.pat_name)
    output += '&nbsp;'+doc.pat_name.htmlEscape();
  output +='</p>';
  //line 2
  output += '<p id="links_' + doc.inst_pk + '" class="links"></p>';
  //line 3
  output += '<p>' + snippet + '</p></div>';
  return output;
};

AjaxSolr.theme.prototype.snippet = function (doc) {
  var output = '';
  if (doc.series_desc)
    output += doc.series_desc.htmlEscape();
  return output;
};

AjaxSolr.theme.prototype.facet = function (facet, handler) {
  var txt = (facet.label || facet.facet) +' ('+facet.count+')';
  return $('<a href="#" class="facet_item"/>').text(txt).click(handler);
};
AjaxSolr.theme.prototype.facet_link = function (value, handler) {
  return $('<a href="#"/>').text(value).click(handler);
};

AjaxSolr.theme.prototype.no_items_found = function () {
  return 'no items found in current selection';
};

})(jQuery);
