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

function runoctFundusRegistration()
octImDir = 'C:\matlab_work\eyesFirst\vessels';
octImFile = [octImDir,filesep,'vessel_data_v2.mat'];
load(octImFile)
fundusImFile = 'R_450.jpg';
bw_VEF = gen_fundus(fundusImFile);
dimInterpOCT = size(line_filtered_image);
dimFundus = size(bw_VEF);
Nslices = 128;
dimOCT = [Nslices dimInterpOCT(2)];
useSmoothFlag = 1; % smooths the vessel images before cross correlating them (if 0 then vessel images not smoothed).  Initial testing with useSmoothFlag = 0 produced
% unsatisfactory results
octInterpFlag = 1; % oct image has been interpolated to approximately square pixels
[CCPeak,scFactor,regCube,O2FrowInd,O2FcolInd] = octFundusRegistration(line_filtered_image,dimOCT,bw_VEF,octInterpFlag,[],useSmoothFlag);
%
% reorder CCPeak so that the coordinates are [fastTime,slowTime];
tempVar = CCPeak(2);
CCPeak(2) = CCPeak(1);
CCPeak(1) = tempVar;
% make an image of the registered OCT and fundus
regOF = zeros([dimFundus 3]);
ft_oct = 1:dimInterpOCT(2);
st_oct = 1:dimInterpOCT(1);
interpCoordFlag = 1;
[ft_fundus,st_fundus] = OCTenface2FundusTensorForm(ft_oct,st_oct,interpCoordFlag,CCPeak,dimFundus,dimOCT,dimInterpOCT,scFactor);
regOF(st_fundus,ft_fundus,1) = line_filtered_image/max(max(line_filtered_image));
regOF(:,:,2) = bw_VEF/max(max(bw_VEF));
figure;
imagesc(regOF)
% resample the vessel enhance enface OCT to match the fundus and map onto the fundus
%[aa1,bb1] = size(Im1); % note rows are slow-time and columns are fast-time
line_filtered_image_interp2 = resampleImage(line_filtered_image,scFactor);
% [rowG,colG] = ndgrid([1:dimInterpOCT(1)],[1:dimInterpOCT(2)]);
% F = griddedInterpolant(rowG,colG,line_filtered_image,'cubic');
% Xss = 1:(1/scFactor(1)):dimInterpOCT(2);
% Yss = 1:(1/scFactor(2)):dimInterpOCT(1);
% [interpRow,interpCol] = ndgrid(Yss,Xss);
% line_filtered_image_interp2 = max(F(interpRow,interpCol),0);
[rr,ss] = size(line_filtered_image_interp2);
regOF2 = zeros([dimFundus, 3]);
regOF2(CCPeak(2)+[0:(rr-1)],CCPeak(1)+[0:(ss-1)],1) = line_filtered_image_interp2/max(max(line_filtered_image_interp2));
regOF2(:,:,2) = bw_VEF/max(max(bw_VEF));
figure;
imagesc(regOF2)
% Do the same for the original enface images
layerFile = 'layerFile_original';
load(layerFile)
% define the combination of layers 1-5 of interest. Sum of all 5 is the
% typical enface image; L2-(L4+L5) is a vessel enhanced image;
curLayerCombo = layerImage.L2 - (layerImage.L3+layerImage.L4);
% resample to a square image
trimOnlyFlag = 0;
normalizeFlag = 1;
curLayerCombo_square = interpEnface(curLayerCombo,trimOnlyFlag,normalizeFlag);
% rescale to match fundus
curLayerCombo_FS = resampleImage(curLayerCombo_square,scFactor);
% align with fundus
regOF3 = zeros([dimFundus, 3]);
[rr,ss] = size(curLayerCombo_FS);
regOF3(CCPeak(2)+[0:(rr-1)],CCPeak(1)+[0:(ss-1)],1) = curLayerCombo_FS/max(max(curLayerCombo_FS));
regOF3(:,:,2) = bw_VEF/max(max(bw_VEF));
figure;
imagesc(regOF3)

%
ft_oct =  [1;1;dimInterpOCT(2);dimInterpOCT(2)];% start with the corners
st_oct = [1;Nslices;Nslices;1];
interpCoordFlag = 0; % find coordinates in the original OCT image ( if set to 1, then it finds points in the interpolated image
[ft_fundus_corner,st_fundus_corner] = OCTenface2Fundus(ft_oct,st_oct,interpCoordFlag,CCPeak,dimFundus,dimOCT,dimInterpOCT,scFactor);
[ft_OCT,st_OCT] = Fundus2OCTenface(ft_fundus_corner,st_fundus_corner,interpCoordFlag,CCPeak,dimFundus,dimOCT,dimInterpOCT,scFactor);
%
interpCoordFlag = 1; % find coordinates in the original OCT image ( if set to 1, then it finds points in the interpolated image
st_oct = [1;dimInterpOCT(1);dimInterpOCT(1);1];
[ft_fundus_corner,st_fundus_corner] = OCTenface2Fundus(ft_oct,st_oct,interpCoordFlag,CCPeak,dimFundus,dimOCT,dimInterpOCT,scFactor);
[ft_OCT,st_OCT] = Fundus2OCTenface(ft_fundus_corner,st_fundus_corner,interpCoordFlag,CCPeak,dimFundus,dimOCT,dimInterpOCT,scFactor)
end

