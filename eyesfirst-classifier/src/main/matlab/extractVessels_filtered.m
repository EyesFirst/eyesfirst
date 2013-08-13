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

function [rowZCmidPoint,colZCmidPoint,oct_vessel_binary] = extractVessels_filtered(baseIm)
%
% gradient sigmas used on vessel filtered images
gradsigma1 = [3,3];% gaussian smoothing parameters
grad2sigma = [3,3];% [3,3]
% gradient sigmas used on L2-(L4+L5)
% gradsigma1 = [5,5];% gaussian smoothing parameters
% grad2sigma = [5,5];% [3,3]
mf = 4; % defines the extent of the smoothing filters as +/- mf*std
%gradVecField = gradientImageVector2d_wzp_sqim(cim,gradsigma1,mf);
[n2gim,g2im,kim] = gradientImage2d_wrd_kim_wzp_sq(baseIm,gradsigma1,grad2sigma,mf);
[rowZC,colZC] = extractZeroCrossings(g2im);
[colMinDepth,rowMinDepth,colGrossWidth,rowGrossWidth,rowZCmidPoint,colZCmidPoint] = zeroCrossingStats(rowZC,colZC);
 depthThresh =  -1.5e-3;% value used on vessel filtered image ;%-1.0E-3;%-1.5e-3;
%depthThresh = -.125; % value used on L2-(L4+L5)
widthThresh = 30;%25;%20;%15;
Irow = find(rowMinDepth <= depthThresh & rowGrossWidth <= widthThresh);
Icol = find(colMinDepth <= depthThresh & colGrossWidth <= widthThresh);
[aa,bb] = size(g2im);
figure; [C,h] = contour([1:bb],[1:aa],g2im,[0,0]);
hold on;
plot(rowZCmidPoint(Irow,2),rowZCmidPoint(Irow,1),'k*')
plot(colZCmidPoint(Icol,2),colZCmidPoint(Icol,1),'r+')
figure;imagesc(baseIm);
hold on;
plot(rowZCmidPoint(Irow,2),rowZCmidPoint(Irow,1),'k*')
plot(colZCmidPoint(Icol,2),colZCmidPoint(Icol,1),'r+')
% make binary vessel image
oct_vessel_binary = zeros(size(baseIm));
for jj = 1:length(Irow)
    oct_vessel_binary(rowZCmidPoint(Irow(jj),1),rowZCmidPoint(Irow(jj),2)) = 1;
end
for jj = 1:length(Icol)
    oct_vessel_binary(colZCmidPoint(Icol(jj),1),colZCmidPoint(Icol(jj),2)) = 1;
end
end

