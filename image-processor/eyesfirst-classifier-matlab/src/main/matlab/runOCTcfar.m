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

function [cfarSAA,curx] = runOCTcfar(SAAFile,bdryFile,cfarPar)

load(SAAFile);
load(bdryFile);

[zlen,xlen,ylen] = size(SAA);
bdryCube = zeros(3,xlen,ylen);
imcube = zeros(zlen,xlen,ylen);
initx = zeros(ylen,1);
finalx = zeros(ylen,1);
for ii = 1:ylen
    curx = (stLayerBdrys{ii}.colOffSet{1}(2)+1):(stLayerBdrys{ii}.colOffSet{2}(1)-1);
    initx(ii)= curx(1);
    finalx(ii) = curx(end);
%     rowshift = stLayerBdrys{ii}.rowOffSet{1}(2);
   % colshift = stLayerBdrys{ii}.colOffSet{1}(2);
end
% fprintf('stop here\n');
% if smoothxy == 1
%     topLayer = squeeze(bdryCube(1,:,:));
%     
% bdryfig = figure;
% imfig = figure;
xstart = max(initx);
xend = min(finalx);
curx = xstart:xend;
Nx = length(curx);
Nslices = ylen;
layerBdryTop = zeros(Nx,Nslices);
layerBdryFloor = zeros(Nx,Nslices);
for rr = 1:Nslices 
   layerBdryTop(:,rr) = stLayerBdrys{rr}.smooth2dBdryRelIm(:,2);
   layerBdryFloor(:,rr) = stLayerBdrys{rr}.smooth2dBdryRelIm(:,1);
end
Bdrys.top = layerBdryTop;
Bdrys.floor = layerBdryFloor;
clear smooth2dBdry;
SAAtrim = SAA(:,curx,:);
clear SAA;
% pack
cfarPar.sizeA = size(SAAtrim);
[cfarSAA,curx] = octImNormLayer(SAAtrim,Bdrys,cfarPar,curx);
% cfarSAA.normim = normSlice;
% cfarSAA.meanim = meanSlice;
% cfarSAA.stdim = stdSlice;
