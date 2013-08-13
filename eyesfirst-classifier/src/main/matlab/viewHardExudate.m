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

function viewHardExudate(saaFile,cfarFile,clusterFeatureFile,layerBdryFile,intLayerBdryFile,mapFile,printFile)
% a function to view the candidate hard exudate bounding boxes superimposed
% on the cfar image
% dimension order of candidateHE boxes is (fastTime,slowTime,axial)
% dimension order of cfarSAA is (axial,fastTime,slowTime)
load(saaFile);
load(cfarFile);
load(clusterFeatureFile);
load(layerBdryFile);
% changed by Dave
%load(mapFile)
% end change
load(intLayerBdryFile);
nHE = numel(candidateHE);
sliceBuffer = 0;
[aa,bb,cc] = size(cfarSAA);
figSAA = figure;
figCFAR = figure;
%cfarRange = [-3,6];
maxSAA = max(max(max(SAA)));
maxCFAR = max(max(max(cfarSAA)));
SAARange = [0,maxSAA+1];
cfarRange = [-3,maxCFAR+1];
cfarRange2 = [0,maxCFAR+1];
maxVal = max(cfarRange);
maxValSAA = maxSAA+1;

axialBuffer = 1; % buffer added to each end of the axial edge of the bounding box  
fastBuffer = 1;  % buffer added to each end of the fast edge of the bounding box
regionBuffer = 10; % buffer added to produce the enlargement of the region containing the bounding box
axialBufferImage = 100;
colormap('gray')
cmap= colormap;
Ncolors = size(cmap,1);
cmap(Ncolors,:) = [0 0 1];

% changed by Dave 07/11/12
%fundusFig1 = figure;
%fundusFig2 = figure;
% curfundusIm1 = intenseMap1;
% curfundusIm2 = intenseMap2;
%maxValFund1 = max(max(curfundusIm1)) + 1;
%maxValFund2 = max(max(curfundusIm2)) + 1;
% end change




Nslices = length(intLayerBdrys_smooth2d);
NFT = length(intLayerBdrys_smooth2d{1}.smoothThickness2dProf12);
Layer1 = zeros(Nslices,NFT);
Layer2 = zeros(Nslices,NFT);
Layer3 = zeros(Nslices,NFT);
Layer4 = zeros(Nslices,NFT);
Layer5 = zeros(Nslices,NFT);
% changed by Dave 7/11/12
curfundusIm1 = zeros(Nslices,NFT);
% end change
for ii = 1:Nslices
    for jj = 1:length(curx)
        Layer1Samp = SAA([floor(stLayerBdrys{ii}.smooth2dBdryRelIm(jj,1)):ceil(intLayerBdrys_smooth2d{ii}.smooth2dInterMediateBdrys(jj,1))],curx(jj),ii);
        Layer2Samp = SAA([floor(intLayerBdrys_smooth2d{ii}.smooth2dInterMediateBdrys(jj,1)):ceil(intLayerBdrys_smooth2d{ii}.smooth2dInterMediateBdrys(jj,2))],curx(jj),ii);
        Layer3Samp = SAA([floor(intLayerBdrys_smooth2d{ii}.smooth2dInterMediateBdrys(jj,2)):ceil(intLayerBdrys_smooth2d{ii}.smooth2dInterMediateBdrys(jj,3))],curx(jj),ii);
        Layer4Samp =  SAA([floor(intLayerBdrys_smooth2d{ii}.smooth2dInterMediateBdrys(jj,3)):ceil(stLayerBdrys{ii}.smooth2dBdryRelIm(jj,2))],curx(jj),ii);
        Layer5Samp = SAA([floor(stLayerBdrys{ii}.smooth2dBdryRelIm(jj,2)): ceil(stLayerBdrys{ii}.smooth2dBdryRelIm(jj,3))],curx(jj),ii);
        Layer1(ii,curx(jj)) = sum(Layer1Samp);
        Layer2(ii,curx(jj)) = sum(Layer2Samp);
        Layer3(ii,curx(jj)) = sum(Layer3Samp);
        Layer4(ii,curx(jj)) = sum(Layer4Samp);
        Layer5(ii,curx(jj)) = sum(Layer5Samp);
        % changed by Dave 07/11/12
        curL45MP =  ceil((stLayerBdrys{ii}.smooth2dBdryRelIm(jj,2) + stLayerBdrys{ii}.smooth2dBdryRelIm(jj,3))/2);
        fundusSamp = SAA([floor(stLayerBdrys{ii}.smooth2dBdryRelIm(jj,1)):curL45MP],curx(jj),ii);
        curfundusIm1(ii,curx(jj)) = sum(fundusSamp);
        % end change
    end;
end;
% changed by Dave 07/11/12
curfundusIm2 = curfundusIm1;
maxValFund1 = max(max(curfundusIm1)) + 1;
maxValFund2 = max(max(curfundusIm2)) + 1;
% end change
curLayer2 = Layer2;
curLayer3 = Layer3;
curLayer4 = Layer4;
maxValLayer1 = max(max(Layer1)) + 1;
maxValLayer2 = max(max(Layer2)) + 1;
maxValLayer3 = max(max(Layer3)) + 1;
maxValLayer4 = max(max(Layer4)) + 1;
maxValLayer5 = max(max(Layer5)) + 1;
%
% CFAR layers
%
cfarLayer1 = zeros(Nslices,NFT);
cfarLayer2 = zeros(Nslices,NFT);
cfarLayer3 = zeros(Nslices,NFT);
cfarLayer4 = zeros(Nslices,NFT);

for ii = 1:Nslices
    curShift = intLayerBdrys_smooth2d{ii}.curxaug(1)-1;
    for jj = 1:length(curx)
        cfarLayer1Samp = cfarSAA([floor(stLayerBdrys{ii}.smooth2dBdryRelIm(jj,1)):ceil(intLayerBdrys_smooth2d{ii}.smooth2dInterMediateBdrys(jj,1))],curx(jj)-curShift,ii);
        cfarLayer2Samp = cfarSAA([floor(intLayerBdrys_smooth2d{ii}.smooth2dInterMediateBdrys(jj,1)):ceil(intLayerBdrys_smooth2d{ii}.smooth2dInterMediateBdrys(jj,2))],curx(jj)-curShift,ii);
        cfarLayer3Samp = cfarSAA([floor(intLayerBdrys_smooth2d{ii}.smooth2dInterMediateBdrys(jj,2)):ceil(intLayerBdrys_smooth2d{ii}.smooth2dInterMediateBdrys(jj,3))],curx(jj)-curShift,ii);
        cfarLayer4Samp =  cfarSAA([floor(intLayerBdrys_smooth2d{ii}.smooth2dInterMediateBdrys(jj,3)):ceil(stLayerBdrys{ii}.smooth2dBdryRelIm(jj,2))],curx(jj)-curShift,ii);
       
        cfarLayer1(ii,curx(jj)) = sum(cfarLayer1Samp);
        cfarLayer2(ii,curx(jj)) = sum(cfarLayer2Samp);
        cfarLayer3(ii,curx(jj)) = sum(cfarLayer3Samp);
        cfarLayer4(ii,curx(jj)) = sum(cfarLayer4Samp);
       
    end;
end;
curcfarLayer2 = cfarLayer2;
curcfarLayer3 = cfarLayer3;
curcfarLayer4 = cfarLayer4;
maxValcfarLayer1 = max(max(cfarLayer1)) + 1;
maxValcfarLayer2 = max(max(cfarLayer2)) + 1;
maxValcfarLayer3 = max(max(cfarLayer3)) + 1;
maxValcfarLayer4 = max(max(cfarLayer4)) + 1;


%
for ii = 1:nHE
    
    minVox = candidateHE(ii).boundingBoxMinCorner;
    boxWidth = candidateHE(ii).boundingBoxWidth;
    minAxial = minVox(3)-axialBuffer;
    minFast = minVox(1)-fastBuffer;
    minSlow = minVox(2);
    delAxial = boxWidth(3)+axialBuffer;
    delFast = boxWidth(1)+fastBuffer;
    delSlow = boxWidth(2);
    sliceRange = max([minSlow-sliceBuffer,1]):min([minSlow+delSlow-1+sliceBuffer,cc]);
    Nslices = length(sliceRange);
    curLayer = candidateHE(ii).layer;
    curShift = intLayerBdrys_smooth2d{sliceRange(1)}.curxaug(1)-1;
    %
    
    
        FI1 = minSlow*ones(1,delFast);
        FJ1 = (minFast:(minFast+delFast-1)) + curShift;
        FI2 = minSlow:(minSlow+delSlow-1);
        FJ2 = ((minFast+delFast-1)*ones(1,delSlow)) + curShift;
        FI3 = (minSlow+delSlow-1)*ones(1,delFast);
        FJ3 = FJ1;
        FI4 = FI2;
        FJ4 = minFast*ones(1,delSlow)+ curShift;

    curfundusIm1 = imposeSubImage(curfundusIm1,FI1,FJ1,maxValFund1);
    curfundusIm1 = imposeSubImage(curfundusIm1,FI2,FJ2,maxValFund1);
    curfundusIm1 = imposeSubImage(curfundusIm1,FI3,FJ3,maxValFund1);
    curfundusIm1 = imposeSubImage(curfundusIm1,FI4,FJ4,maxValFund1);
%
    curfundusIm2 = imposeSubImage(curfundusIm2,FI1,FJ1,maxValFund2);
    curfundusIm2 = imposeSubImage(curfundusIm2,FI2,FJ2,maxValFund2);
    curfundusIm2 = imposeSubImage(curfundusIm2,FI3,FJ3,maxValFund2);
    curfundusIm2 = imposeSubImage(curfundusIm2,FI4,FJ4,maxValFund2);
    
    if curLayer == 2
        curLayer2 = imposeSubImage(curLayer2,FI1,FJ1,maxValLayer2);
        curLayer2 = imposeSubImage(curLayer2,FI2,FJ2,maxValLayer2);
        curLayer2 = imposeSubImage(curLayer2,FI3,FJ3,maxValLayer2);
        curLayer2 = imposeSubImage(curLayer2,FI4,FJ4,maxValLayer2);
        %
        curcfarLayer2 = imposeSubImage(curcfarLayer2,FI1,FJ1,maxValLayer2);
        curcfarLayer2 = imposeSubImage(curcfarLayer2,FI2,FJ2,maxValLayer2);
        curcfarLayer2 = imposeSubImage(curcfarLayer2,FI3,FJ3,maxValLayer2);
        curcfarLayer2 = imposeSubImage(curcfarLayer2,FI4,FJ4,maxValLayer2);
    elseif curLayer == 3
        curLayer3 = imposeSubImage(curLayer3,FI1,FJ1,maxValLayer3);
        curLayer3 = imposeSubImage(curLayer3,FI2,FJ2,maxValLayer3);
        curLayer3 = imposeSubImage(curLayer3,FI3,FJ3,maxValLayer3);
        curLayer3 = imposeSubImage(curLayer3,FI4,FJ4,maxValLayer3);
        %
        curcfarLayer3 = imposeSubImage(curcfarLayer3,FI1,FJ1,maxValLayer2);
        curcfarLayer3 = imposeSubImage(curcfarLayer3,FI2,FJ2,maxValLayer2);
        curcfarLayer3 = imposeSubImage(curcfarLayer3,FI3,FJ3,maxValLayer2);
        curcfarLayer3 = imposeSubImage(curcfarLayer3,FI4,FJ4,maxValLayer2);
    elseif curLayer == 4
        curLayer4 = imposeSubImage(curLayer4,FI1,FJ1,maxValLayer4);
        curLayer4 = imposeSubImage(curLayer4,FI2,FJ2,maxValLayer4);
        curLayer4 = imposeSubImage(curLayer4,FI3,FJ3,maxValLayer4);
        curLayer4 = imposeSubImage(curLayer4,FI4,FJ4,maxValLayer4);
        %
        curcfarLayer4 = imposeSubImage(curcfarLayer4,FI1,FJ1,maxValLayer2);
        curcfarLayer4 = imposeSubImage(curcfarLayer4,FI2,FJ2,maxValLayer2);
        curcfarLayer4 = imposeSubImage(curcfarLayer4,FI3,FJ3,maxValLayer2);
        curcfarLayer4 = imposeSubImage(curcfarLayer4,FI4,FJ4,maxValLayer2);
    end;
 
    
    for jj = 1:Nslices;
        curCFARslice = squeeze(cfarSAA(:,:,sliceRange(jj)));
        curShift = intLayerBdrys_smooth2d{sliceRange(jj)}.curxaug(1)-1;
        [aa,bb] = size(curCFARslice);
        % add top edge to curCFARslice
        I1 = minAxial*ones(1,delFast);
        J1 = minFast:(minFast+delFast-1);
        I2 = minAxial:(minAxial+delAxial-1);
        J2 = (minFast+delFast-1)*ones(1,delAxial);
        I3 = (minAxial+delAxial-1)*ones(1,delFast);
        J3 = J1;
        I4 = I2;
        J4 = minFast*ones(1,delAxial);
        curCFARslice = imposeSubImage(curCFARslice,I1,J1,maxVal);
        curCFARslice = imposeSubImage(curCFARslice,I2,J2,maxVal);
        curCFARslice = imposeSubImage(curCFARslice,I3,J3,maxVal);
        curCFARslice = imposeSubImage(curCFARslice,I4,J4,maxVal);
        figure(figCFAR);clf(figCFAR);
%         cmap = contrast(curCFARslice);% colormap('gray')
        Ncolors = size(cmap,1);
        cmap(Ncolors,:) = [0 0 1]; 
        [aa,bb] - size(curCFARslice);
      %  curShift = intLayerBdrys_smooth2d{sliceRange(jj)}.curxaug(1)-1;
        ftrange = curShift+[1:bb];
        arange = [1:bb];
        subplot(2,1,1),imagesc(ftrange,arange,curCFARslice,cfarRange)
        title(['cluster ',int2str(ii),'; CFAR slice ',int2str(sliceRange(jj))]);
        colormap(cmap);
        %
        % plot the blow up of the region containing the HE
        %
        HEregionJ = max(1,(minFast-regionBuffer)):min(((minFast+delFast-1)+regionBuffer),bb);
        HEregionI = max(1,(minAxial-axialBufferImage)):min(aa,(minAxial+delAxial-1+axialBufferImage));
        HEregion = curCFARslice(HEregionI,HEregionJ);
       
        subplot(2,1,2), imagesc(HEregionJ+curShift,HEregionI,HEregion,cfarRange);%,cfarRange2);
        colormap(cmap);
        title(['enlargement cluster ',int2str(ii),'; CFAR slice ',int2str(sliceRange(jj))]);
        % image the SAA cube
        curSAAslice = squeeze(SAA(:,:,sliceRange(jj)));
        % make the coordinate adjustment to convert cfar coordinates to SAA
        % coordinates 
        
        minAxialSAA = minAxial;
        minSlowSAA = minSlow;
        minFastSAA = minFast + curShift;
        I1 = minAxialSAA*ones(1,delFast);
        J1 = minFastSAA:(minFastSAA+delFast-1);
        I2 = minAxialSAA:(minAxialSAA+delAxial-1);
        J2 = (minFastSAA+delFast-1)*ones(1,delAxial);
        I3 = (minAxialSAA+delAxial-1)*ones(1,delFast);
        J3 = J1;
        I4 = I2;
        J4 = minFastSAA*ones(1,delAxial);
        curSAAslice = imposeSubImage(curSAAslice,I1,J1,maxValSAA);
        curSAAslice = imposeSubImage(curSAAslice,I2,J2,maxValSAA);
        curSAAslice = imposeSubImage(curSAAslice,I3,J3,maxValSAA);
        curSAAslice = imposeSubImage(curSAAslice,I4,J4,maxValSAA);
        figure(figSAA)
       
        subplot(2,1,1),imagesc(curSAAslice);%SAARange);
        colormap(cmap);
        title(['cluster ',int2str(ii),'; SAA slice ',int2str(sliceRange(jj))]);
        %
        HEregionJSAA = max(1,(minFastSAA-regionBuffer)):min(((minFastSAA+delFast-1)+regionBuffer),bb);
        HEregionISAA = max(1,(minAxialSAA-axialBufferImage)):min(aa,(minAxialSAA+delAxial-1+axialBufferImage));
        HEregionSAA = curSAAslice(HEregionISAA,HEregionJSAA);
       
        subplot(2,1,2), imagesc(HEregionJSAA,HEregionISAA,HEregionSAA);
        colormap(cmap);
        title(['enlargement cluster ',int2str(ii),'; SAA slice ',int2str(sliceRange(jj))]);
        %print(figCFAR,printFile,'-dpsc2','-append');
        %print(figSAA,printFile,'-dpsc2','-append');
    end;
end;
fundusFig1 = figure;
%         cmap = contrast(intenseMap1);% colormap('gray')
%         Ncolors = size(cmap,1);
%         cmap(Ncolors,:) = [0 0 1];
I1 = find(curfundusIm1(:) > 0);
mv1 = mean(curfundusIm1(I1));
std1 = std(curfundusIm1(I1));
Nstdm1 = mv1/std1;
maxVal = max(curfundusIm1(I1));
NstdMax = (maxVal-mv1)/std1;
minVal = max([0 mv1-NstdMax*std1]);
imagesc(curfundusIm1,[minVal,maxVal]);
colormap(cmap);
title('Psuedo fundus image1')
%print(fundusFig1,printFile,'-dpsc2','-append');
fundusFig2 = figure;


I2 = find(curfundusIm2(:) > 0);
mv2 = mean(curfundusIm2(:));
std2 = std(curfundusIm2(:));
Nstdm2 = mv2/std2;
maxVal = max(curfundusIm2(:));
NstdMax = (maxVal-mv2)/std2;
minVal = max([0 mv2-NstdMax*std2]);

imagesc(curfundusIm2,[minVal,maxVal]);
colormap(cmap);
title('Psuedo fundus image2')
%print(fundusFig2,printFile,'-dpsc2','-append');

LayerFig1 = figure;
I2 = find(Layer1(:) > 0);
mv2 = mean(Layer1(I2));
std2 = std(Layer1(I2));
Nstdm2 = mv2/std2;
maxVal = max(Layer1(I2));
NstdMax = (maxVal-mv2)/std2;
minVal = max([0 mv2-NstdMax*std2]);
imagesc(Layer1,[minVal,maxVal]);
colormap(cmap);
title('Layer1')
%print(LayerFig1,printFile,'-dpsc2','-append');
LayerFig2 = figure;
I2 = find(Layer2(:) > 0);
mv2 = mean(Layer2(I2));
std2 = std(Layer2(I2));
Nstdm2 = mv2/std2;
maxVal = max(Layer2(I2));
NstdMax = (maxVal-mv2)/std2;
minVal = max([0 mv2-NstdMax*std2]);
imagesc(Layer2,[minVal,maxVal]);
colormap(cmap);
title('Layer2')
%print(LayerFig2,printFile,'-dpsc2','-append');
LayerFig3 = figure;

I2 = find(Layer3(:) > 0);
mv2 = mean(Layer3(I2));
std2 = std(Layer3(I2));
Nstdm2 = mv2/std2;
maxVal = max(Layer3(I2));
NstdMax = (maxVal-mv2)/std2;
minVal = max([0 mv2-NstdMax*std2]);
imagesc(Layer3,[minVal,maxVal]);
colormap(cmap);
title('Layer3')
%print(LayerFig3,printFile,'-dpsc2','-append');

LayerFig4 = figure;

I2 = find(Layer4(:) > 0);
mv2 = mean(Layer4(I2));
std2 = std(Layer4(I2));
Nstdm2 = mv2/std2;
maxVal = max(Layer4(I2));
NstdMax = (maxVal-mv2)/std2;
minVal = max([0 mv2-NstdMax*std2]);
imagesc(Layer4,[minVal,maxVal]);
colormap(cmap);
title('Layer4')
%print(LayerFig4,printFile,'-dpsc2','-append');
LayerFig5 = figure;

I2 = find(Layer5(:) > 0);
mv2 = mean(Layer5(I2));
std2 = std(Layer5(I2));
Nstdm2 = mv2/std2;
maxVal = max(Layer5(I2));
NstdMax = (maxVal-mv2)/std2;
minVal = max([0 mv2-NstdMax*std2]);
imagesc(Layer5,[minVal,maxVal]);
colormap(cmap);
title('Layer5')
%print(LayerFig5,printFile,'-dpsc2','-append');

LayerFig2_overLay = figure;
I2 = find(curLayer2(:) > 0);
mv2 = mean(curLayer2(I2));
std2 = std(curLayer2(I2));
Nstdm2 = mv2/std2;
maxVal = max(Layer2(I2));
NstdMax = (maxVal-mv2)/std2;
minVal = max([0 mv2-NstdMax*std2]);
imagesc(curLayer2,[minVal,maxVal]);
colormap(cmap);
title('Layer2 with clusters')
print(LayerFig2_overLay,printFile,'-dpsc2','-append');

LayerFig3_overLay = figure;

I2 = find(curLayer3(:) > 0);
mv2 = mean(curLayer3(I2));
std2 = std(curLayer3(I2));
Nstdm2 = mv2/std2;
maxVal = max(curLayer3(I2));
NstdMax = (maxVal-mv2)/std2;
minVal = max([0 mv2-NstdMax*std2]);
imagesc(curLayer3,[minVal,maxVal]);
colormap(cmap);
title('Layer3 with clusters')
print(LayerFig3_overLay,printFile,'-dpsc2','-append');

LayerFig4_overLay = figure;

I2 = find(curLayer4(:) > 0);
mv2 = mean(curLayer4(I2));
std2 = std(curLayer4(I2));
Nstdm2 = mv2/std2;
maxVal = max(curLayer4(I2));
NstdMax = (maxVal-mv2)/std2;
minVal = max([0 mv2-NstdMax*std2]);
imagesc(curLayer4,[minVal,maxVal]);
colormap(cmap);
title('Layer4 with clusters')
print(LayerFig4_overLay,printFile,'-dpsc2','-append');
%
% cfarLayer1fig = figure;
% I2 = find(cfarLayer1(:) > 0);
% mv2 = mean(cfarLayer1(I2));
% std2 = std(cfarLayer1(I2));
% Nstdm2 = mv2/std2;
% maxVal = max(cfarLayer1(I2));
% NstdMax = (maxVal-mv2)/std2;
% minVal = max([0 mv2-NstdMax*std2]);
% imagesc(cfarLayer1,[minVal,maxVal]);
% colormap(cmap);
% title('cfarLayer1')
% print(cfarLayer1fig,printFile,'-dpsc2','-append');
% 
% 
% %
% cfarLayer2fig = figure;
% I2 = find(cfarLayer2(:) > 0);
% mv2 = mean(cfarLayer2(I2));
% std2 = std(cfarLayer2(I2));
% Nstdm2 = mv2/std2;
% maxVal = max(cfarLayer2(I2));
% NstdMax = (maxVal-mv2)/std2;
% minVal = max([0 mv2-NstdMax*std2]);
% imagesc(cfarLayer2,[minVal,maxVal]);
% colormap(cmap);
% title('cfarLayer2')
% print(cfarLayer2fig,printFile,'-dpsc2','-append');
% %
% cfarLayer3fig = figure;
% I2 = find(cfarLayer3(:) > 0);
% mv2 = mean(cfarLayer3(I2));
% std2 = std(cfarLayer3(I2));
% Nstdm2 = mv2/std2;
% maxVal = max(cfarLayer3(I2));
% NstdMax = (maxVal-mv2)/std2;
% minVal = max([0 mv2-NstdMax*std2]);
% imagesc(cfarLayer3,[minVal,maxVal]);
% colormap(cmap);
% title('cfarLayer3')
% print(cfarLayer3fig,printFile,'-dpsc2','-append');
%     %
% cfarLayer4fig = figure;
% I2 = find(cfarLayer4(:) > 0);
% mv2 = mean(cfarLayer4(I2));
% std2 = std(cfarLayer4(I2));
% Nstdm2 = mv2/std2;
% maxVal = max(cfarLayer4(I2));
% NstdMax = (maxVal-mv2)/std2;
% minVal = max([0 mv2-NstdMax*std2]);
% imagesc(cfarLayer4,[minVal,maxVal]);
% colormap(cmap);
% title('cfarLayer4')
% print(cfarLayer4fig,printFile,'-dpsc2','-append');

%

 %
% curcfarLayer2fig = figure;
% I2 = find(curcfarLayer2(:) > 0);
% mv2 = mean(curcfarLayer2(I2));
% std2 = std(curcfarLayer2(I2));
% Nstdm2 = mv2/std2;
% maxVal = max(curcfarLayer2(I2));
% NstdMax = (maxVal-mv2)/std2;
% minVal = max([0 mv2-NstdMax*std2]);
% imagesc(curcfarLayer2,[minVal,maxVal]);
% colormap(cmap);
% title('curcfarLayer2')
% print(curcfarLayer2fig,printFile,'-dpsc2','-append');
% %
% curcfarLayer3fig = figure;
% I2 = find(curcfarLayer3(:) > 0);
% mv2 = mean(curcfarLayer3(I2));
% std2 = std(curcfarLayer3(I2));
% Nstdm2 = mv2/std2;
% maxVal = max(curcfarLayer3(I2));
% NstdMax = (maxVal-mv2)/std2;
% minVal = max([0 mv2-NstdMax*std2]);
% imagesc(curcfarLayer3,[minVal,maxVal]);
% colormap(cmap);
% title('curcfarLayer3')
% print(curcfarLayer3fig,printFile,'-dpsc2','-append');
%     %
% curcfarLayer4fig = figure;
% I2 = find(curcfarLayer4(:) > 0);
% mv2 = mean(curcfarLayer4(I2));
% std2 = std(curcfarLayer4(I2));
% Nstdm2 = mv2/std2;
% maxVal = max(curcfarLayer4(I2));
% NstdMax = (maxVal-mv2)/std2;
% minVal = max([0 mv2-NstdMax*std2]);
% imagesc(curcfarLayer4,[minVal,maxVal]);
% colormap(cmap);
% title('curcfarLayer4')
% print(curcfarLayer4fig,printFile,'-dpsc2','-append');   


