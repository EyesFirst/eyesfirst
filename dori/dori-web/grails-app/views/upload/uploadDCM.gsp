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
  <title>Upload DCM</title>
  <dori:favicon/>
 </head>
 <body>
  <header><h1>Upload DCM</h1></header>
  <p>Upload a DCM file directly. It must be unencrypted, and the EFID for it
  must already exist.</p>
  <g:uploadForm action="receive">
   <p>EFID: <input type="text" name="efid"></p>
   <p>DCM file: <input type="file" name="dicom"></p>
   <p><input type="submit" value="Upload">
  </g:uploadForm>
 </body>
</html>