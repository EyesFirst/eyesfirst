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

function [BdryRelIm,rowOffSet,colOffSet,oimwla,thicknessProf12,thicknessProf13,intensityProf12,intensityProf13,intensityProf1B] = identifyLayers2D_wrd_kim(curim,imtop,imfloor,Nlayers,mas,maxILdist,minILdist,costFigH,layerFigH,dfstd,ksf)
%UNTITLED2 Summary of this function goes here
%   Detailed explanation goes here
%dfstd = 2;
dfmf = 4;
maxRangeCf = 1000;
[gim,g2im,kim] = gradientImage2d_wrd_kim(curim,dfstd,dfmf,'dump');
cf = -gim+abs(g2im)+ksf*kim;
figure(costFigH);
imagesc(cf);
title('cost function')
[encf,rowOffSet,colOffSet] = extractNonNan2d(cf);
%figure;
%imagesc(squeeze(SAATB(:,:,sliceInd)));
%imtop = floor(polyGate{sliceInd}.extrapLower);
coff1 = colOffSet{1}(2)+1;
 coffend = colOffSet{2}(1)-1;
 rowshift = rowOffSet{1}(2);
%imfloor = ceil(polyGate{sliceInd}.extrapUpper);
[aa,bb] = size(encf);
imfloorShift = max(min(imfloor(coff1:coffend)-rowshift,aa),1);
imtopShift = min(max(imtop(coff1:coffend)-rowshift,1),aa);
retZone = imfloorShift-imtopShift;
retZoneZero = find(retZone <= 0);
if ~isempty(retZoneZero)
    imtopShift = ones(size(imfloorShift));
end;
% cfwg = encf;
 % [aa,bb] = size(encf);
% maxcfwg = max(max(cfwg));
% for ii = 1:bb
%     cfwg(imfloorShift(ii),ii) = maxcfwg;
%     cfwg(imtopShift(ii),ii) = maxcfwg;
% end;
% figure;
% imagesc(cfwg);

% maxILdist = 300; %250;
% minILdist = 20;
% maxInterLayerDist = repmat([maxILdist 75],bb,1);
% minInterLayerDist =repmat([minILdist 20],bb,1);
maxInterLayerDist = repmat(maxILdist,bb,1);
minInterLayerDist =repmat(minILdist,bb,1);
% [csRelTop,csRelIm,BdryRelTop,BdryRelIm] = findLayer(encf,imfloorShift,imtopShift,mas,maxRangeCf,'dump');
[csRelTop,csRelIm,BdryRelTop,BdryRelIm] = findMultiLayer(encf,imfloorShift,imtopShift,mas,maxRangeCf,Nlayers,maxInterLayerDist,minInterLayerDist,[]);
% encfwla = encf;
% minencf = min(min(encf));
% [aacf,bbcf]= size(encf);
% 
% for ii = 1:bbcf
%    for jj = 1:Nlayers
%        encfwla(BdryRelIm(ii,jj),ii) = minencf;
%    end;
% end;
% figure; imagesc(encfwla);
% title('cost function with embedded layers')
% original image with embedded layers

maxabsim = max(max(abs(curim))); % check that this is not a NaN
oimwla = curim*.98;
oimsla = oimwla;
[aa0,bb0] = size(oimwla);
for ii = 1:bb
   for jj = 1:Nlayers
       rowvec = rowshift+BdryRelIm(ii,jj)+[-3 -2 -1 0 1 2 3];
       Ivalid = find(rowvec >= 1 & rowvec <= aa0);
       if length(Ivalid) > 0
          oimwla(rowvec(Ivalid),ii+colOffSet{1}(2)) = maxabsim*ones(length(Ivalid),1);
       end;
   end;
end;
% smooth boundaries 
% smoothBdrysRelIm = zeros(size(BdryRelIm));
% for jj = 1:Nlayers
%     curBdry = BdryRelIm(:,jj);
%     mt1 = 10000;
%     [smoothCurBdry,smvals] = spaps([1:bbcf],curBdry,mt1);
%     smoothBdrysRelIm(:,jj) = round(smvals);
% end;
figure(layerFigH);
cmap = gray;
cmap(64,:) = [0 0 1];
imagesc(oimwla);colormap(cmap);title('original image with boundaries');
% figure(layerFigH);
% for ii = 1:bbcf
%    for jj = 1:Nlayers
%        oimsla(rowshift+smoothBdrysRelIm(ii,jj)+[-3 -2 -1 0 1 2 3],ii+colOffSet{1}(2)) = maxabsim*ones(7,1);
%    end;
% end;
% imagesc(oimsla);colormap(cmap);title('original image with boundaries');
% save(ofile,'csRelTop','csRelIm','BdryRelTop','BdryRelIm','rowOffSet','colOffSet','encf','curim','gim','g2im','encfwla','oimwla','oimsla');
% append 
% save(ofileBase,'csRelTop','csRelIm','BdryRelTop','BdryRelIm','rowOffSet','colOffSet','encf','curim','gim','g2im','oimwla');

% generate profiles
% thicknessProf1,thicknessProf2,intensityProf1,intensityProf2

% maxabsim = max(max(abs(curim))); % check that this is not a NaN
% oimwla = curim*.98;
% oimsla = oimwla;
% for ii = 1:bbcf
%    for jj = 1:Nlayers
%        oimwla(rowshift+BdryRelIm(ii,jj)+[-3 -2 -1 0 1 2 3],ii+colOffSet{1}(2)) = maxabsim*ones(7,1);
%    end;
% end;
[aa,bb] = size(curim);
[xx,yy] = size(BdryRelIm);
intensityProf12 = zeros(1,bb);
intensityProf13 = zeros(1,bb);
intensityProf1B = zeros(1,bb);
thicknessProf12 = zeros(1,bb);
thicknessProf13 = zeros(1,bb);
thicknessProf12(colOffSet{1}(2)+[1:xx]) = (BdryRelIm(:,2)-BdryRelIm(:,1)+1)';
thicknessProf13(colOffSet{1}(2)+[1:xx]) = (BdryRelIm(:,3)-BdryRelIm(:,1)+1)';
for ii = 1:xx
   intensityProf12(ii+colOffSet{1}(2)) =  sum(curim(rowshift+[BdryRelIm(ii,1):BdryRelIm(ii,2)],ii+colOffSet{1}(2)));
   intensityProf13(ii+colOffSet{1}(2)) =  sum(curim(rowshift+[BdryRelIm(ii,1):BdryRelIm(ii,3)],ii+colOffSet{1}(2)));
%   intensityProf1B(ii+colOffSet{1}(2)) =  sum(curim(rowshift+[BdryRelIm(ii,1):end],ii+colOffSet{1}(2)));
end

