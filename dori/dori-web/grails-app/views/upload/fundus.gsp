<!DOCTYPE html>
<%--
Copyright 2013 The MITRE Corporation

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
  <title>Feedback for Image Classifier</title>
  <dori:favicon/>
 </head>
 <body>
  <header><h1>Upload Fundus Image</h1></header>
  <p><strong>Note:</strong> If there is already a fundus photo uploaded for
  the associated image, this will <strong>replace</strong> that!</p>
  <g:uploadForm action="receiveFundus">
   <p>Add fundus: <input type="file" name="studyUID=${request.getParameter("studyUID")}&amp;seriesUID=${request.getParameter("seriesUID")}&amp;objectUID=${request.getParameter("objectUID")}"></p>
   <p><input type="submit" value="Import">
  </g:uploadForm>
 </body>
</html>