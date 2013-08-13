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

function [LayerImages,VesselEnhancedImage]= create_enface_Images(saaFile,layerBdryFile,intLayerBdryFile,mapFile)
% a function to view the candidate hard exudate bounding boxes superimposed
% on the cfar image
% dimension order of candidateHE boxes is (fastTime,slowTime,axial)
% dimension order of cfarSAA is (axial,fastTime,slowTime)
load(saaFile);

load(layerBdryFile);
load(mapFile)
load(intLayerBdryFile);

%cfarRange = [-3,6];
maxSAA = max(max(max(SAA)));

SAARange = [0,maxSAA+1];

maxValSAA = maxSAA+1;

axialBuffer = 1; % buffer added to each end of the axial edge of the bounding box  
fastBuffer = 1;  % buffer added to each end of the fast edge of the bounding box
regionBuffer = 10; % buffer added to produce the enlargement of the region containing the bounding box
axialBufferImage = 100;
colormap('gray')
cmap= colormap;
Ncolors = size(cmap,1);
cmap(Ncolors,:) = [0 0 1];
fundusFig1 = figure;
fundusFig2 = figure;
%curfundusIm1 = intenseMap1;
%curfundusIm2 = intenseMap2;
%maxValFund1 = max(max(curfundusIm1)) + 1;
%maxValFund2 = max(max(curfundusIm2)) + 1;
Nslices = length(intLayerBdrys_smooth2d);
NFT = length(intLayerBdrys_smooth2d{1}.smoothThickness2dProf12);
Layer1 = zeros(Nslices,NFT);
Layer2 = zeros(Nslices,NFT);
Layer3 = zeros(Nslices,NFT);
Layer4 = zeros(Nslices,NFT);
Layer5 = zeros(Nslices,NFT);
curfundusIm1 = zeros(Nslices,NFT);
curx = intLayerBdrys_smooth2d{1}.curx;
for ii = 1:Nslices
    for jj = 1:length(curx)
        Layer1Samp = SAA([floor(stLayerBdrys{ii}.smooth2dBdryRelIm(jj,1)):ceil(intLayerBdrys_smooth2d{ii}.smooth2dInterMediateBdrys(jj,1))],curx(jj),ii);
        Layer2Samp = SAA([floor(intLayerBdrys_smooth2d{ii}.smooth2dInterMediateBdrys(jj,1)):ceil(intLayerBdrys_smooth2d{ii}.smooth2dInterMediateBdrys(jj,2))],curx(jj),ii);
        Layer3Samp = SAA([floor(intLayerBdrys_smooth2d{ii}.smooth2dInterMediateBdrys(jj,2)):ceil(intLayerBdrys_smooth2d{ii}.smooth2dInterMediateBdrys(jj,3))],curx(jj),ii);
        Layer4Samp =  SAA([floor(intLayerBdrys_smooth2d{ii}.smooth2dInterMediateBdrys(jj,3)):ceil(stLayerBdrys{ii}.smooth2dBdryRelIm(jj,2))],curx(jj),ii);
        Layer5Samp =  SAA([floor(stLayerBdrys{ii}.smooth2dBdryRelIm(jj,2)): ceil(stLayerBdrys{ii}.smooth2dBdryRelIm(jj,3))],curx(jj),ii);
        curL45MP =  ceil((stLayerBdrys{ii}.smooth2dBdryRelIm(jj,2) + stLayerBdrys{ii}.smooth2dBdryRelIm(jj,3))/2);
        fundusSamp = SAA([floor(stLayerBdrys{ii}.smooth2dBdryRelIm(jj,1)):curL45MP],curx(jj),ii);
        Layer1(ii,curx(jj)) = sum(Layer1Samp);
        Layer2(ii,curx(jj)) = sum(Layer2Samp);
        Layer3(ii,curx(jj)) = sum(Layer3Samp);
        Layer4(ii,curx(jj)) = sum(Layer4Samp);
        Layer5(ii,curx(jj)) = sum(Layer5Samp);
        curfundusIm1(ii,curx(jj)) = sum(fundusSamp);
    end;
end;
LayerImages.L1 = Layer1;
LayerImages.L2 = Layer2;
LayerImages.L3 = Layer3;
LayerImages.L4 = Layer4;
LayerImages.L5 = Layer5;

VesselEnhancedImage = (LayerImages.L2)./(LayerImages.L4 + LayerImages.L5);


% Interpolate to 512 rows
[nrows,ncols] = size(VesselEnhancedImage);

VEI = zeros(nrows*4,ncols);
for icol = 1:ncols
    big_col = oneD_bl_interp(VesselEnhancedImage(:,icol),4,0,0);
    VEI(:,icol) = real(big_col);
end
%figure(3),imagesc(VEI),colormap(gray)

extra_cols = isnan(VEI(1,:));
good_cols = sum(extra_cols ==0);
CVEI = zeros(nrows*4,good_cols);

current_col = 1;
for icol = 1:ncols
    if extra_cols(icol) == 0
        CVEI(:,current_col) = VEI(:,icol);
        current_col = current_col + 1;
    end
end


ACVEI = imadjust(CVEI);
%figure(4),imagesc(ACVEI),colormap(gray),axis image,colorbar

line_filter_kernel = zeros(32,32);

wdth = 1.0;
g_kernel = exp(-(([-2.5 -1.5 -.5 .5 1.5 2.5]')/(1.414*wdth)).^2);

for kcol = 1:32
line_filter_kernel(14:19,kcol) = g_kernel;
end


[NR,NC] = size(ACVEI);
line_filtered_images = zeros(NR,NC,18);
line_filtered_images(:,:,1)  = conv2(ACVEI,imrotate(line_filter_kernel,  0,'bilinear','crop'), 'same');
line_filtered_images(:,:,2)  = conv2(ACVEI,imrotate(line_filter_kernel, 10,'bilinear','crop'), 'same');
line_filtered_images(:,:,3)  = conv2(ACVEI,imrotate(line_filter_kernel, 20,'bilinear','crop'), 'same');
line_filtered_images(:,:,4)  = conv2(ACVEI,imrotate(line_filter_kernel, 30,'bilinear','crop'), 'same');
line_filtered_images(:,:,5)  = conv2(ACVEI,imrotate(line_filter_kernel, 40,'bilinear','crop'), 'same');
line_filtered_images(:,:,6)  = conv2(ACVEI,imrotate(line_filter_kernel, 50,'bilinear','crop'), 'same');
line_filtered_images(:,:,7)  = conv2(ACVEI,imrotate(line_filter_kernel, 60,'bilinear','crop'), 'same');
line_filtered_images(:,:,8)  = conv2(ACVEI,imrotate(line_filter_kernel, 70,'bilinear','crop'), 'same');
line_filtered_images(:,:,9)  = conv2(ACVEI,imrotate(line_filter_kernel, 80,'bilinear','crop'), 'same');
line_filtered_images(:,:,10) = conv2(ACVEI,imrotate(line_filter_kernel, 90,'bilinear','crop'), 'same');
line_filtered_images(:,:,11) = conv2(ACVEI,imrotate(line_filter_kernel,100,'bilinear','crop'), 'same');
line_filtered_images(:,:,12) = conv2(ACVEI,imrotate(line_filter_kernel,110,'bilinear','crop'), 'same');
line_filtered_images(:,:,13) = conv2(ACVEI,imrotate(line_filter_kernel,120,'bilinear','crop'), 'same');
line_filtered_images(:,:,14) = conv2(ACVEI,imrotate(line_filter_kernel,130,'bilinear','crop'), 'same');
line_filtered_images(:,:,15) = conv2(ACVEI,imrotate(line_filter_kernel,140,'bilinear','crop'), 'same');
line_filtered_images(:,:,16) = conv2(ACVEI,imrotate(line_filter_kernel,150,'bilinear','crop'), 'same');
line_filtered_images(:,:,17) = conv2(ACVEI,imrotate(line_filter_kernel,160,'bilinear','crop'), 'same');
line_filtered_images(:,:,18) = conv2(ACVEI,imrotate(line_filter_kernel,170,'bilinear','crop'), 'same');


[line_filtered_image,direction_image] = max(line_filtered_images,[],3);
VesselEnhancedImage = line_filtered_image;

