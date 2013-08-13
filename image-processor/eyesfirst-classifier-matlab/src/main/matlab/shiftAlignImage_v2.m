% Copyright 2013 The MITRE Corporation
%
% Licensed under the Apache License, Version 2.0 (the "License");
% you may not use this file except in compliance with the License.
% You may obtain a copy of the License at
%
%     http://www.apache.org/licenses/LICENSE-2.0
%
% Unless required by applicable law or agreed to in writing, software
% distributed under the License is distributed on an "AS IS" BASIS,
% WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
% See the License for the specific language governing permissions and
% limitations under the License.

function SAA = shiftAlignImage_v2(A,z_array,x_array,zdeg,xdeg)
[aa,bb,cc] = size(A);
dy = [1:cc-1];
polyz = polyfit(dy,z_array,zdeg);
%figure;
%plot(dy,z_array,'.r');
%hold on;
%plot(dy,z_array,'b');
%pfz = polyval(polyz,[1:.1:cc-1]);
%plot([1:.1:cc-1],pfz,'g');
%title('axial shift (z)')
%
polyx = polyfit(dy,x_array,zdeg);
%figure;
%plot(dy,x_array,'.r');
%hold on;
%plot(dy,x_array,'b');
%pfx = polyval(polyx,[1:.1:cc-1]);
%plot([1:.1:cc-1],pfx,'g');
%title('horizontal shift (x)')
polyZVal = polyval(polyz,1:cc-1);
polyXVal = polyval(polyx,1:cc-1);
zcorrect =  round(z_array-polyZVal);
xcorrect =  round(x_array-polyXVal);
cumZcorrect = cumsum(zcorrect);
cumXcorrect = cumsum(xcorrect);
maxabsposcumZcorrect = max(cumZcorrect);
maxabsnegcumZcorrect = abs(min(cumZcorrect));
maxabsposcumXcorrect = max(cumXcorrect);
maxabsnegcumXcorrect = abs(min(cumXcorrect));
aacor = aa + maxabsposcumZcorrect+ maxabsnegcumZcorrect;
bbcor = bb + maxabsposcumXcorrect+ maxabsnegcumXcorrect;
cccor = cc;
SAA = zeros(aacor,bbcor,cccor);
lastZshift = 0;
lastXshift = 0;
SAA(1+maxabsnegcumZcorrect:aa+maxabsnegcumZcorrect,1+maxabsnegcumXcorrect:bb+maxabsnegcumXcorrect,1) = A(:,:,1);
fprintf('SAA(%d:%d,%d:%d,1)\n', 1+maxabsnegcumZcorrect, aa+maxabsnegcumZcorrect, 1+maxabsnegcumXcorrect, bb+maxabsnegcumXcorrect);
for jj = 1:cc-1
    curZshift = zcorrect(jj);
    curXshift = xcorrect(jj);
    cumZshift = lastZshift+curZshift;
    cumXshift = lastXshift+curXshift;
    fprintf('SAA(%d:%d,%d:%d,%d)\n', 1+maxabsnegcumZcorrect+cumZshift, aa+maxabsnegcumZcorrect+cumZshift, 1+maxabsnegcumXcorrect+cumXshift, bb+maxabsnegcumXcorrect+cumXshift, jj+1);
    SAA(1+maxabsnegcumZcorrect+cumZshift:aa+maxabsnegcumZcorrect+cumZshift,1+maxabsnegcumXcorrect+cumXshift:bb+maxabsnegcumXcorrect+cumXshift,jj+1) =  A(:,:,jj+1); 
    lastZshift = cumZshift;
    lastXshift = cumXshift;
end;
fprintf('Done.\n');



