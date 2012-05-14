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

function regionInd = polarSector(centerInd,sector,imageDim,di,dj)
% sector is a cell array of polar segments specified by angle and range
% limits.  Note that sector{k} is defined by counter clockwise rotation from 
% sector{k}.ang(1) to sector{k}.ang(2)and
% -pi<= sector{k}.ang(2) <= pi.  
% imageDim is [Nrows,Ncols] of the image
% di is the resolution along rows and dj is the resolution along columns
% on return, regionInd is a cell array of pixel indices such that the center of the pixels are contained in the indicated region.
% determine the range and azimuth from the center of every pixel to the
% center of the 
Nsectors = length(sector);
% [X,Y] = meshgrid([1:imageDim(2)],[1:imageDim(1)])
rangeMat = zeros(imageDim);
angleMat = zeros(imageDim);
for ii = 1:imageDim(1)
    for jj = 1:imageDim(2)
        rangeMat(ii,jj) = sqrt((di*(ii-centerInd(1)))^2+(dj*(jj-centerInd(2)))^2);
        angleMat(ii,jj) = atan2(-di*(ii-centerInd(1)),dj*(jj-centerInd(2)));
    end
end
regionInd = cell(Nsectors,1);
figure;
chkim = zeros(imageDim);
for kk = 1:Nsectors
    changeBranch = 0;
    ang1 = sector{kk}.ang(1);
    ang2 = sector{kk}.ang(2);
    range1 = sector{kk}.range(1);
    range2 = sector{kk}.range(2);
    if ang2 < ang1
        ang2 = ang2+2*pi;
        changeBranch = 1;
    end
    if changeBranch == 0
       [I,J] = find( rangeMat < range2 & rangeMat >= range1 & angleMat >= ang1 & angleMat < ang2);
    else
        angleMatTemp = angleMat;
        INeg = find(angleMatTemp < 0);
        angleMatTemp(INeg) = angleMatTemp(INeg) + 2*pi;
        [I,J] = find( rangeMat < range2 & rangeMat >= range1 & angleMatTemp >= ang1 & angleMatTemp < ang2);
    end;
    regionInd{kk}.I = I;
    regionInd{kk}.J = J;
    for ll = 1:length(I)
        chkim(I(ll),J(ll)) = kk;
    end;
end; 
imagesc(chkim);colorbar;title(['region ',int2str(kk)]);
hold on; plot(centerInd(2),centerInd(1),'kx','markersize',16,'linewidth',2);