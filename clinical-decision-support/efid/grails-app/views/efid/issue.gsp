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

--%><!DOCTYPE html>

<html><head><title>Issue EFID</title>
<script type="text/javascript"><!--
function issueEfid() {
	var xhr = new XMLHttpRequest();
	xhr.open("POST", "");
	xhr.onreadystatechange = function() {
		if (xhr.readyState == 4) {
			if (xhr.status != 200) {
				alert("Could not issue EFID: " + xhr.status + " " + xhr.statusText);
			} else {
				var p = document.createElement('p');
				p.appendChild(document.createTextNode("Generated EFID: " + xhr.responseText));
				document.getElementById("efids").appendChild(p);
			}
		}
	};
	xhr.send(null);
}
//--></script>
</head>
<body>
<h1>Issue EFID</h1>
<form method="POST" action="" onsubmit="return false">
<button onclick="issueEfid()">Issue EFID</button>
</form>
<div id="efids"></div>
</body>
</html>