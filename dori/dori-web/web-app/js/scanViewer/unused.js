/*
 * This just contains some unused code that used to allow synthesizing different
 * slice views. It was determined to not actually be useful, and is therefore
 * dumped into this file in case it ever needs to be revived. The current
 * system doesn't really allow for this.
 */

SliceManager.Canvas.prototype.showSlice = function(type, slice, sliding) {
	if (type == 'x') {
		// Need the offscreen canvas for this - synthesize the slice, then
		// display it. X slices show z,y slices.
		var ctx = this._offscreenCanvas.getContext('2d');
		this._offscreenCanvas.width = this._depth;
		this._offscreenCanvas.height = this._height;
		for (var x = 0; x < this._depth; x++) {
			ctx.drawImage(this.slices[x], slice, 0, 1, this._height, x, 0, 1, this._height);
		}
		this._canvas.width = this._depth;
		this._canvas.height = this._height;
		var context = this._canvas.getContext('2d');
		context.drawImage(this._offscreenCanvas, 0, 0);
	} else if (type == 'y') {
		// Need the offscreen canvas for this - synthesize the slice, then
		// display it. Y slices show z,x slices.
		var ctx = this._offscreenCanvas.getContext('2d');
		this._offscreenCanvas.width = this._width;
		this._offscreenCanvas.height = this._depth;
		for (var y = 0; y < this._depth; y++) {
			ctx.drawImage(this.slices[y], 0, slice, this._width, 1, 0, y, this._width, 1);
		}
		this._drawSlice(this._offscreenCanvas, this._width, this._depth);
	} else if (type == 'z') {
		this._slice = slice;
		this._drawSlice(this.slices[slice], this._width, this._height);
	}
};