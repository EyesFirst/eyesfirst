<!DOCTYPE html>
<%--
Copyright 2012 The MITRE Corporation

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
--%>
<html>
<head>
  <title>DORI Browser</title>
  <meta http-equiv="Content-Script-Type" content="text/javascript"/>
  <meta http-equiv="Content-Style-Type" content="text/css"/>
  <dori:favicon/>

  <link type="text/css" rel="stylesheet" href="css/styles.css" media="screen" />

  <!--<script type="text/javascript" src="https://getfirebug.com/firebug-lite.js"></script>-->

  <jq:resources/>
  <!-- <script type="text/javascript">window.alert = function() { console.log.call(console, arguments); throw Error("DO NOT USE ALERT!")}</script> -->
  <script type="text/javascript" src="js/jquery.livequery.js"></script>
  <script type="text/javascript" src="js/jquery.tmpl.min.js"></script>
  <script type="text/javascript" src="ajax-solr/helpers/ajaxsolr.support.js"></script><!-- e.g. String.hmtlEscape() -->

  <script type="text/javascript" src="ajax-solr/core/Core.js"></script>
  <script type="text/javascript" src="ajax-solr/core/AbstractManager.js"></script>
  <script type="text/javascript" src="ajax-solr/managers/Manager.jquery.js"></script>
  <script type="text/javascript" src="ajax-solr/core/Parameter.js"></script>
  <script type="text/javascript" src="ajax-solr/core/ParameterStore.js"></script>
  <script type="text/javascript" src="ajax-solr/core/AbstractWidget.js"></script>
  <script type="text/javascript" src="ajax-solr/core/AbstractFacetWidget.js"></script>
  <script type="text/javascript" src="ajax-solr/core/AbstractTextWidget.js"></script>

  <script type="text/javascript" src="js/widgets/ResultWidget.js"></script>
  <script type="text/javascript" src="ajax-solr/widgets/jquery/PagerWidget.js"></script>
  <script type="text/javascript" src="js/widgets/FacetValueWidget.js"></script>
  <script type="text/javascript" src="js/widgets/CurrentSearchWidget.js"></script>
  <script type="text/javascript" src="js/widgets/CurrentFilterWidget.js"></script>
  <script type="text/javascript" src="js/widgets/RangeFacetWidget.js"></script>

  <script type="text/javascript" src="ajax-solr/helpers/ajaxsolr.theme.js"></script>
  <script type="text/javascript" src="ajax-solr/helpers/jquery/ajaxsolr.theme.js"></script>
  <script type="text/javascript" src="js/theme.js"></script>

  <script type="text/javascript" src="js/identicon_canvas.js"></script>
  
  <script type="text/javascript" src="js/main.js"></script>  
  
</head>
<body>
<div id="container">

  <header id="pageHead">
    <h1><em>DORI</em>: An EyesFirst Project</h1>
    <nav id="headerNav">
    </nav>
  </header>
  <script id="headerNavTemplate" type="text/x-jquery-tmpl">
    <span>Welcome, {{= username}}</span> <span class="sep">|</span>
    <!--<a href="about.html">About</a> <span class="sep">|</span>-->
    <!--<a href="help.html">Help</a> <span class="sep">|</span>-->
    <!--TODO <a href="account.html">Account</a> <span class="sep">|</span>-->
    <g:link controller="logout">Sign out</g:link>
  </script>

  <div id="searchFacets">
  <section id="currentFilter">
    <header>
      <a href="#" id="clear_all">[ clear all ]</a>
      <h1>Keyword</h1>
    </header>
    <ul>
      <li><input id="search" type="search" placeholder="search text" id="query" name="query"/></li>
    </ul>
  </section>

  <script id="valueFilterTemplate" type="text/x-jquery-tmpl">
    <header>
      <a href="#" id="{{= field}}">[ clear ]</a>
      <h1>{{= title}}</h1>
    </header>
    <ul>
      {{each(i, facet) facets}}
        <li><label><input type="checkbox" value="{{= facet.name}}" {{= facet.checked}} /> {{= facet.label}} ({{= facet.count}})</label>
      {{/each}}
    </ul>
  </script>

  <script id="rangeFilterTemplate" type="text/x-jquery-tmpl">
    <header>
      <a href="#" id="{{= field}}">[ clear ]</a>
      <h1>{{= title}}</h1>
    </header>
    <ul>
      {{each(i, facet) facets}}
        <li><label>
          <input type="checkbox" value="{{= facet.facetStart}}" data-end="{{= facet.facetEnd}}" {{= facet.checked}}/> 
          {{= facet.facetStart}} {{if facet.facetEnd !== "*"}} - {{= facet.facetEnd}} {{else}} + {{/if}} ({{= facet.count}})
        </label></li>
      {{/each}}
    </ul>
  </script>
  <section id="modalityFilter">
  </section>

  <section id="ageFilter">
  </section>

  <section id="genderFilter">
  </section>

  <section id="lateralityFilter">
  </section>
  
  <section id="issuerFilter">
  </section>

  </div><!--searchFacets-->

  <div id="searchResults">

    <div id="pagination">
    </div>
    <script id="paginationHeaderTemplate" type="text/x-jquery-tmpl">
      <span id="resultsTotal">displaying
        <span id="itemStart">{{= Math.min(total, offset + 1)}}</span> to
        <span id="itemEnd">{{= Math.min(total, offset + perPage)}}</span> of
        <span id="itemTotal">{{= total}}</span> results
      </span>
    </script>

    <div id="docs">
    </div>
    <script id="docsTemplate" type="text/x-jquery-tmpl">
      <table>
        <thead>
          <tr>
            <th>&nbsp;<!-- identicon column --></th>
            <th>Patient ID</th>
            <th>Date Updated</th>
            <th>Image ID</th>
            <th>Age</th>
            <th>Modality</th>
            <th>EFID Issuer</th>
            <th>&nbsp;<!-- original image link column --></th>
            <th>&nbsp;<!-- processed image link column --></th>
            <th>&nbsp;<!-- diagnosis link column --></th>
            <th>&nbsp;<!-- feedback link column --></th>
            <th>&nbsp;<!-- export link column --></th>
            <th>&nbsp;<!-- add fundus column --></th>
          </tr>
        </thead>
        <tbody>
          {{each(i, doc) docs}}
          <tr>
            <td><canvas title="identicon {{= getPatientNumFromId(doc.pat_id)}}" width="15" height="15"></canvas></td>
            <td>{{= doc.efid}}</td>
            <td><time datetime="{{= doc.updated_time}}">{{= doc.updated_time}}</time></td>
            <td>{{= doc.inst_pk}}</td>
            <td>{{= doc.study_pat_age_years || '?'}}</td>
            <td>{{= doc.modality}}</td>
            <td>{{= doc.efid_issuer_name}}</td>
            <td><a href="${g.createLink(controller:'oct-scan-viewer')}?{{= getViewerParamStringFromDoc(doc)}}">View Original</a></td>
            <td><a href="${g.createLink(controller:'oct-scan-viewer')}?{{= doc.processed_query_string}}&processed=true" {{if doc.processed_query_string == null}} style="display: none" {{/if}}>View Processed</a></td>
            <td><a href="${g.createLink(controller:'diagnosis')}?rawQueryString={{= encodeURIComponent(getViewerParamStringFromDoc(doc))}}">View Diagnoses</a></td>
            <td><a href="${g.createLink(controller:'feedback')}?processedQueryString={{= encodeURIComponent(doc.processed_query_string)}}" {{if doc.processed_query_string == null}} style="display: none" {{/if}}>View Feedback</a></td>
            <td><a href="${g.createLink(controller:'browser', action:'export')}/{{= doc.efid}}/{{= doc.dori_id}}">Export</a></td>
            <td><a href="${g.createLink(controller:'upload', action:'fundus')}?{{= getViewerParamStringFromDoc(doc)}}">Add fundus</a></td>
          </tr>
          {{/each}}
        </tbody>
      </table>
    </script>
  </div>

</div><!--container-->
</body>
</html>