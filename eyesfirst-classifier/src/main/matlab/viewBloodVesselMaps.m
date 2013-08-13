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

function layerImage = viewBloodVesselMaps(saaFile,cfarFile,clusterFeatureFile,layerBdryFile,intLayerBdryFile,mapFile,gateFile)
% a function to view the candidate hard exudate bounding boxes superimposed
% on the cfar image
% dimension order of candidateHE boxes is (fastTime,slowTime,axial)
% dimension order of cfarSAA is (axial,fastTime,slowTime)
layerImage = struct;
load(saaFile);
%load(cfarFile);
%load(clusterFeatureFile);
load(layerBdryFile);
load(gateFile)
% changed by Dave
load(mapFile)
% end change
load(intLayerBdryFile);
%nHE = numel(candidateHE);
gateExtend = 300;
[aa,bb,cc] = size(SAA);

%cfarRange = [-3,6];
maxSAA = max(max(max(SAA)));
%maxCFAR = max(max(max(cfarSAA)));
SAARange = [0,maxSAA+1];
% cfarRange = [-3,maxCFAR+1];
% cfarRange2 = [0,maxCFAR+1];
floorImFig = figure;
pixelDim.axial = 1.9531; % microns
pixelDim.fastTime = 11.7188; % microns
stBdryCoord = estimateRetinalFloor(layerBdryFile,[],[],floorImFig,pixelDim);
colormap('gray')
cmap= colormap;

saveSliceInd = 68;
%stBdryCoord(saveSliceInd).bdryCoord
%curx_saveInd = stBdryCoord(saveSliceInd).bdryCoord(1,:);
Nslices = length(intLayerBdrys_smooth2d);
NFT = length(intLayerBdrys_smooth2d{1}.smoothThickness2dProf12);
Layer1 = zeros(Nslices,NFT);
Layer2 = zeros(Nslices,NFT);
Layer3 = zeros(Nslices,NFT);
Layer4 = zeros(Nslices,NFT);
Layer5 = zeros(Nslices,NFT);
% Layer6A = zeros(Nslices,NFT);
% Layer6B = zeros(Nslices,NFT);
% Layer6C = zeros(Nslices,NFT);
% Layer6D = zeros(Nslices,NFT);
% Layer6E = zeros(Nslices,NFT);
% Layer6F = zeros(Nslices,NFT);
% changed by Dave 7/11/12
% end change
stLayerSampD = zeros(aa,bb,cc);
% LCX = length(curx_saveInd);
% bdryMat = zeros(7,length(curx_saveInd));
% bdryMat(1,:) = stLayerBdrys{saveSliceInd}.smooth2dBdryRelIm(1:LCX,1)';
% bdryMat(2,:) = floor(intLayerBdrys_smooth2d{saveSliceInd}.smooth2dInterMediateBdrys(1:LCX,1))';
% bdryMat(3,:) = floor(intLayerBdrys_smooth2d{saveSliceInd}.smooth2dInterMediateBdrys(1:LCX,2))';
% bdryMat(4,:) = floor(intLayerBdrys_smooth2d{saveSliceInd}.smooth2dInterMediateBdrys(1:LCX,3))';
% bdryMat(5,:) = floor(stLayerBdrys{saveSliceInd}.smooth2dBdryRelIm(1:LCX,2))';
%bdryMat(6,:) = ceil(stBdryCoord(saveSliceInd).bdryCoord(4,1:LCX)+1)';
%bdryMat(7,:) = aa*ones(1,LCX);
% figure;
% imagesc(squeeze(SAA(:,:,saveSliceInd)));
% hold on;
% plot(curx_saveInd,bdryMat(1,:),'r','linewidth',3)
% plot(curx_saveInd,bdryMat(2,:),'r','linewidth',3)
% plot(curx_saveInd,bdryMat(3,:),'r','linewidth',3)
% plot(curx_saveInd,bdryMat(4,:),'r','linewidth',3)
% plot(curx_saveInd,bdryMat(5,:),'r','linewidth',3)
% plot(curx_saveInd,bdryMat(6,:),'r','linewidth',3)
% plot(curx_saveInd,bdryMat(7,:),'r','linewidth',3)
%curx

for ii = 1:Nslices
    curx = stBdryCoord(ii).bdryCoord(1,:);
   
    for jj = 1:length(curx)
        Layer1Samp = SAA([floor(stLayerBdrys{ii}.smooth2dBdryRelIm(jj,1)):ceil(intLayerBdrys_smooth2d{ii}.smooth2dInterMediateBdrys(jj,1))],curx(jj),ii);
        Layer2Samp = SAA([floor(intLayerBdrys_smooth2d{ii}.smooth2dInterMediateBdrys(jj,1)):ceil(intLayerBdrys_smooth2d{ii}.smooth2dInterMediateBdrys(jj,2))],curx(jj),ii);
        Layer3Samp = SAA([floor(intLayerBdrys_smooth2d{ii}.smooth2dInterMediateBdrys(jj,2)):ceil(intLayerBdrys_smooth2d{ii}.smooth2dInterMediateBdrys(jj,3))],curx(jj),ii);
        Layer4Samp =  SAA([floor(intLayerBdrys_smooth2d{ii}.smooth2dInterMediateBdrys(jj,3)):ceil(stLayerBdrys{ii}.smooth2dBdryRelIm(jj,2))],curx(jj),ii);
        Layer5Samp = SAA([floor(stLayerBdrys{ii}.smooth2dBdryRelIm(jj,2)): ceil(stLayerBdrys{ii}.smooth2dBdryRelIm(jj,3))],curx(jj),ii);
%         Layer6ASamp = SAA([ceil(stLayerBdrys{ii}.smooth2dBdryRelIm(jj,3)+1):ceil(imageGate(2,curx(jj),ii))],curx(jj),ii);
%         Layer6BSamp = SAA([ceil(stLayerBdrys{ii}.smooth2dBdryRelIm(jj,3)+1):end],curx(jj),ii);
%         Layer6CSamp = SAA([ceil(stBdryCoord(ii).bdryCoord(4,jj)+1):ceil(imageGate(2,curx(jj),ii))],curx(jj),ii);
%         Layer6DSamp = SAA([ceil(stBdryCoord(ii).bdryCoord(4,jj)+1):end],curx(jj),ii);
%         Layer6ESamp = SAA([ceil(stLayerBdrys{ii}.smooth2dBdryRelIm(jj,3)+1):ceil(min(gateExtend+imageGate(2,curx(jj),ii),aa))],curx(jj),ii);
%         Layer6FSamp = SAA([ceil(stBdryCoord(ii).bdryCoord(4,jj)+1):ceil(min(gateExtend+imageGate(2,curx(jj),ii),aa))],curx(jj),ii);
        Layer1(ii,curx(jj)) = sum(Layer1Samp);
        Layer2(ii,curx(jj)) = sum(Layer2Samp);
        Layer3(ii,curx(jj)) = sum(Layer3Samp);
        Layer4(ii,curx(jj)) = sum(Layer4Samp);
        Layer5(ii,curx(jj)) = sum(Layer5Samp);
%         Layer6A(ii,curx(jj)) = sum(Layer6ASamp);
%       %  stLayerSamp([ceil(stLayerBdrys{ii}.smooth2dBdryRelIm(jj,3)+1):ceil(imageGate(2,curx(jj),ii))],curx(jj),1) = Layer6ASamp;
%         Layer6B(ii,curx(jj)) = sum(Layer6BSamp);
%        % stLayerSamp([ceil(stLayerBdrys{ii}.smooth2dBdryRelIm(jj,3)+1):end],curx(jj),2) = Layer6BSamp;
%         Layer6C(ii,curx(jj)) = sum(Layer6CSamp);
%        % stLayerSamp([ceil(stBdryCoord(ii).bdryCoord(4,jj)+1):ceil(imageGate(2,curx(jj),ii))],curx(jj),3) = Layer6CSamp;
%         Layer6D(ii,curx(jj)) = sum(Layer6DSamp);
%         stLayerSampD([ceil(stBdryCoord(ii).bdryCoord(4,jj)+1):end],curx(jj),ii) = Layer6DSamp;
%         Layer6E(ii,curx(jj)) = sum(Layer6ESamp);
%        % stLayerSamp([ceil(stLayerBdrys{ii}.smooth2dBdryRelIm(jj,3)+1):ceil(min(gateExtend+imageGate(2,curx(jj),ii),aa))],curx(jj),5) = Layer6ESamp;
%         Layer6F(ii,curx(jj)) = sum(Layer6FSamp);
%        % stLayerSamp([ceil(stBdryCoord(ii).bdryCoord(4,jj)+1):ceil(min(gateExtend+imageGate(2,curx(jj),ii),aa))],curx(jj),6) = Layer6FSamp;
    end;
end;
layerImage.L1 = Layer1;
layerImage.L2 = Layer2;
layerImage.L3 = Layer3;
layerImage.L4 = Layer4;
layerImage.L5 = Layer5;
% layerImage.L6A = Layer6A;
% layerImage.L6B = Layer6B;
% layerImage.L6C = Layer6C;
% layerImage.L6D = Layer6D;
% layerImage.L6E = Layer6E;
% layerImage.L6F = Layer6F;
% layerImage.layerSampD = stLayerSampD;
figure; 
% subplot(3,2,1);imagesc(Layer1)
% title('Layer 1')
subplot(2,2,1);imagesc(Layer2)
title('Layer 2')
subplot(2,2,2);imagesc(Layer3)
title('Layer 3')
subplot(2,2,3);imagesc(Layer4)
title('Layer 4')
subplot(2,2,4);imagesc(Layer5)
title('Layer 5')
% % subplot(3,2,6);imagesc(Layer6)
% %title('Layer 5')
% figure;
% subplot(2,2,1);imagesc(Layer6B)
% title('Layer 6B')
% subplot(2,2,2);imagesc(Layer6D)
% title('Layer 6D')
% subplot(2,2,3);imagesc(Layer6E)
% title('Layer 6E')
% subplot(2,2,4);imagesc(Layer6F)
% title('Layer 6F')
