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

function [rowZCmidPoint,colZCmidPoint,fundus_vessel_binary] = extractVessels_fundus(baseIm,fundusGradientFile)
%
if isempty(fundusGradientFile)
    gradsigma1 = [15,15];% gaussian smoothing parameters
    grad2sigma = [15,15];% [3,3]
    mf = 4; % defines the extent of the smoothing filters as +/- mf*std
    %gradVecField = gradientImageVector2d_wzp_sqim(cim,gradsigma1,mf);
    [n2gim,g2im,kim] = gradientImage2d_wrd_kim_wzp_sq(baseIm,gradsigma1,grad2sigma,mf);
    [rowZC,colZC] = extractZeroCrossings(g2im);
    [colMinDepth,rowMinDepth,colGrossWidth,rowGrossWidth,rowZCmidPoint,colZCmidPoint] = zeroCrossingStats(rowZC,colZC);
    save('fundus_gradient_file')
else
    load(fundusGradientFile)
end;
depthThresh = -.750e-4;%-1.0E-3;%-1.5e-3;
% widthThresh = 30;%25;%20;%15;
widthUpperBound = 80;
widthLowerBound = 10;
Irow = find(rowMinDepth <= depthThresh & rowGrossWidth <= widthUpperBound &   rowGrossWidth >= widthLowerBound );
Icol = find(colMinDepth <= depthThresh & colGrossWidth <= widthUpperBound &   colGrossWidth >= widthLowerBound);
[aa,bb] = size(g2im);
% figure; [C,h] = contour([1:bb],[1:aa],g2im,[0,0]);
% hold on;
% plot(rowZCmidPoint(Irow,2),rowZCmidPoint(Irow,1),'k*')
% plot(colZCmidPoint(Icol,2),colZCmidPoint(Icol,1),'r+')
figure;
imagesc(baseIm)
colormap('gray')
hold on;
plot(rowZCmidPoint(Irow,2),rowZCmidPoint(Irow,1),'k*')
plot(colZCmidPoint(Icol,2),colZCmidPoint(Icol,1),'r+')
% make fundus_vessel_binary
fundus_vessel_binary = zeros(size(baseIm));
for jj = 1:length(Irow)
    fundus_vessel_binary(rowZCmidPoint(Irow(jj),1),rowZCmidPoint(Irow(jj),2)) = 1;
end
for jj = 1:length(Icol)
    fundus_vessel_binary(colZCmidPoint(Icol(jj),1),colZCmidPoint(Icol(jj),2)) = 1;
end

