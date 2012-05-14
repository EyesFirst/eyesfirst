% Copyright 2012 The MITRE Corporation
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

function bdryIm = polarSectorBoundaries(centerInd,boundaryCurves,imageDim,di,dj)
% sector is a cell array of polar segments specified by angle and range
% limits.  Note that sector{k} is defined by counter clockwise rotation from 
% sector{k}.ang(1) to sector{k}.ang(2)and
% -pi<= sector{k}.ang(2) <= pi.  
% imageDim is [Nrows,Ncols] of the image
% di is the resolution along rows and dj is the resolution along columns
% on return, regionInd is a cell array of pixel indices such that the center of the pixels are contained in the indicated region.
% determine the range and azimuth from the center of every pixel to the
% center of the 
% Nsectors = length(sector);
% [X,Y] = meshgrid([1:imageDim(2)],[1:imageDim(1)])
bdryIm = zeros(imageDim);
centerInd = round(centerInd);
UB = 10^6;
rangeMat = zeros(imageDim);
angleMat = zeros(imageDim);
for ii = 1:imageDim(1)
    for jj = 1:imageDim(2)
        rangeMat(ii,jj) = sqrt((di*(ii-centerInd(1)))^2+(dj*(jj-centerInd(2)))^2);
        angleMat(ii,jj) = atan2(-di*(ii-centerInd(1)),dj*(jj-centerInd(2)));
    end
end
Ncircles = length(boundaryCurves.radii);
pixDiag = sqrt(di^2+dj^2);
for ii = 1:Ncircles
    curRadii = boundaryCurves.radii(ii);
    curBdry = abs(rangeMat - curRadii) <= pixDiag/2;
    bdryIm = bdryIm+curBdry;
end;
Nrays = length(boundaryCurves.rays);
for ii = 1:Nrays
    curAngle = boundaryCurves.rays(ii);
    if curAngle > -pi/2 && curAngle <= pi/2
        CJ = centerInd(2):imageDim(2);
    else
        CJ = 1:centerInd(2);
    end;
    [minang,minangIind] = min(abs(angleMat(:,CJ)-curAngle));
    for kk = 1:length(CJ)
        cRange = rangeMat(minangIind(kk),CJ(kk));
        if cRange >=  boundaryCurves.radii(1) && cRange <=  boundaryCurves.radii(end);
            bdryIm(minangIind(kk),CJ(kk)) = 1;
        end;
    end;
end;
