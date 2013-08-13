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
/*
 * Hard exudate handlers
 */


// Some helper classes:

/**
 * Point in 3D space.
 * @constructor
 */
function Point3D(x, y, z) {
	this.x = x;
	this.y = y;
	this.z = z;
}

Point3D.prototype = {
	toString: function() {
		return "(" + this.x + ", " + this.y + ", " + this.z + ")";
	}
};

function Rectangle3D(x, y, z, width, height, depth) {
	if (arguments.length == 1 && typeof x == 'object') {
		this.x = x.x;
		this.y = x.y;
		this.z = x.z;
		this.width = x.width;
		this.height = x.height;
		this.depth = x.depth;
	} else {
		this.x = x;
		this.y = y;
		this.z = z;
		this.width = width;
		this.height = height;
		this.depth = depth;
	}
}

Rectangle3D.prototype = {
	contains: function(x, y, z) {
		if (arguments.length == 1) {
			if (typeof x != 'object')
				throw Error('Expected point');
			y = x.y;
			z = x.z;
			x = x.x;
		}
		return (this.x <= x && x <= (this.x + this.width)) &&
			(this.y <= y && y <= (this.y + this.height)) &&
			(this.z <= z && z <= (this.z + this.depth));
	},
	containsX: function(x) {
		return this.x <= x && x <= (this.x + this.width);
	},
	containsY: function(y) {
		return this.y <= y && y <= (this.y + this.height);
	},
	containsZ: function(z) {
		return this.z <= z && z <= (this.z + this.depth);
	},
	toString: function() {
		return "(" + this.x + ", " + this.y + ", " + this.z + ") [" +
			this.width + " x " + this.height + " x " + this.depth + "]";
	}
}

/**
 * An individual hard exudate
 * @constructor
 */
function HardExudate(json) {
	// An individual hard exudate has the following values:
	// TODO: Check values
	this.maxCfar = json['maxCfarValue'];
	this.normalScore = json['normalScore'];
	this.layer = json['layer'];
	this.numVoxels = json['numVoxels'];
	// For backwards compatability reasons, use boundingBoxMinCorner if
	// boundingBoxMinCornerShift isn't in the JSON
	var p = "boundingBoxMinCornerShift" in json ? json["boundingBoxMinCornerShift"] : json["boundingBoxMinCorner"];
	var d = json["boundingBoxWidth"];
	// Coordinates are in order columns, layers, rows
	// (or fast time, slow time, axial)
	// (or x, z, y)
	this.boundingBox = new Rectangle3D(p[0], p[2], p[1], d[0], d[2], d[1]);
	this.layerProportion = json["layerProportion"];
	p = json["ellipseCenter"];
	this.ellipseCenter = new Point3D(p[0], p[2], p[1]);
}

HardExudate.prototype = {
	isInLayer: function(layer) {
		return this.boundingBox.containsZ(layer);
	},
	getBoundingBox: function() {
		return this.boundingBox;
	}
};