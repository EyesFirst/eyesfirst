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

function intLayerBdrys = identifyInternalLayers(SAAFile,bdryFile,statFile,internalLayerPar)
ofile = [];
stdv2d = internalLayerPar.stdv2d;
ksf = internalLayerPar.ksf;
dfmf = internalLayerPar.dfmf;
Nlayers = internalLayerPar.Nlayers;
maxRangeCf = 1000;
%quadFig = figure;

load(SAAFile);
load(bdryFile);
load(statFile);
[aa,bb,cc] = size(SAA);
% determine the fast time domain
% im = stLayerBdrys{25}.oimwsla2d;
% maxim = max(im,[],1);
% curx = find(maxim == max(maxim));
% Nx = length(curx);
% masm= ones(Nx,Nlayers);
Nslices = cc;
cmap = gray;
cmap(64,:) = [0 0 1];
cp = thicknessFeatures.cp;
Hval = thicknessFeatures.Hessian;
slowTime = [1:Nslices];
fastTime = [1:bb];
QSA=quadraticSurfaceApprox(cp,Hval,slowTime,fastTime);
mu = 10^(-3);
intLayerBdrys = cell(Nslices,1);
%maxDistNearFovea=fovealmodel2(SAAFile,thicknessMapFile,statFile);


[zlen,xlen,ylen] = size(SAA);
initx = zeros(ylen,1);
finalx = zeros(ylen,1);
for ii = 1:ylen
    curx = (stLayerBdrys{ii}.colOffSet{1}(2)+1):(stLayerBdrys{ii}.colOffSet{2}(1)-1);
    initx(ii)= curx(1);
    finalx(ii) = curx(end);
end
% fprintf('stop here\n');
% if smoothxy == 1
%     topLayer = squeeze(bdryCube(1,:,:));
%     
% bdryfig = figure;
% imfig = figure;
xstart = max(initx);
xend = min(finalx);
curx = [xstart:xend];
for ii = 1:Nslices
    fprintf('STATUS:{"message":"Identifying internal layers (%d/%d)..."}\n', ii, Nslices);
    curim = SAA(:,:,ii);
    [gim,g2im,kim] = gradientImage2d_wrd_kim(curim,stdv2d,dfmf,'dump');
   %  kim=calculateCurvature(curim, stdv2d, dfmf);
    cf = -gim+abs(g2im)+ksf*kim;
    cf = cf(:,curx);
%     cf3d = squeeze(-gim3d(:,:,ii) + abs(g2im3d(:,:,ii)));
    [encf,rowOffSet,colOffSet] = extractNonNan2d_2(cf);
    % colOffSet is now relative to cf;  need to make it relative to SAA
     colOffSet{1} = colOffSet{1}+curx(1)-1;
     colOffSet{2}(1) = colOffSet{2}(1)+curx(1)-1;
     if colOffSet{2}(1) <= bb
        colOffSet{2}(2) = bb; %colOffSet{2}(2)+curx(1)-1;
     else
         colOffSet{2}(2) = colOffSet{2}(1);
     end
%     [encf3d,rowOffSet3d,colOffSet3d] = extractNonNan2d(cf3d);
    [aa0,bb0] = size(encf);
    % cftrim = cf(:,curx);
    imfloor = round(stLayerBdrys{ii}.smooth2dBdryRelIm(:,2));
    imtop = round(stLayerBdrys{ii}.smooth2dBdryRelIm(:,1));
  %  curx = [stLayerBdrys{ii}.colOffSet{1}(2)+1:stLayerBdrys{ii}.colOffSet{2}(1)-1];
    Nx = length(curx);
    masm= ones(Nx,Nlayers);
    %[imFloorColRelFull,imFloorRelFull] = subImage2FullImage([1:length(imfloor)],imfloor,stLayerBdrys{ii}.colOffSet,stLayerBdrys{ii}.rowOffSet);
    %[imFloorColRelSub,imFloorRelSub] = fullImage2SubImage(imFloorColRelFull,imFloorRelFull,colOffSet,rowOffSet);
    [imFloorColRelSub,imFloorRelSub] = fullImage2SubImage(curx,imfloor,colOffSet,rowOffSet);
    %[imTopColRelFull,imTopRelFull] = subImage2FullImage([1:length(imtop)],imtop,stLayerBdrys{ii}.colOffSet,stLayerBdrys{ii}.rowOffSet);
    %[imTopColRelSub,imTopRelSub] = fullImage2SubImage(imTopColRelFull,imTopRelFull,colOffSet,rowOffSet);
    [imTopColRelSub,imTopRelSub] = fullImage2SubImage(curx,imtop,colOffSet,rowOffSet);
    quadBound = QSA(curx,ii)*mu';
    %quadBound = QSA(colOffSet{1}(2)+1:colOffSet{2}(1)-1,ii)*mu';
    %figure(quadFig); clf;plot(quadBound,'k');hold on;
    % original version
%         maxInterLayerDist = repmat(round((imFloorRelSub-imTopRelSub)/Nlayers),1,Nlayers+1);
%     minInterLayerDist = 15*ones(Nx,Nlayers-1);
%     minInterLayerDist = [10*ones(Nx,1) minInterLayerDist 25*ones(Nx,1)];
    
    maxInterLayerDist = repmat(round((imFloorRelSub-imTopRelSub)/Nlayers),1,Nlayers+1);
    minInterLayerDist = 15*ones(Nx,Nlayers-1);
    minInterLayerDist = [10*ones(Nx,1) minInterLayerDist 25*ones(Nx,1)];
    for layerNum = 1:Nlayers
       % minInterLayerDist(:,layerNum) = floor(min(minInterLayerDist(:,layerNum),(maxDistNearFovea(ii,curx(1):curx(1)+Nx-1).')*mu(layerNum)));
       minInterLayerDist(:,layerNum) = floor(min(minInterLayerDist(:,layerNum),quadBound));
       % maxDistNearFovea(maxDistNearFovea(curx:curx+Nx-1,ii)<minInterLayerDist(1,layerNum))
    end
    % figure(quadFig); plot(minInterLayerDist);title(['slice ',int2str(ii)]);
    [csRelTop,csRelIm,BdryRelTop,BdryRelIm] = insertMultiLayer(encf,imFloorRelSub,imTopRelSub,masm,maxRangeCf,Nlayers,maxInterLayerDist,minInterLayerDist,ofile);
    maxabsim = max(max(abs(curim))); % check that this is not a NaN
    oimwla = curim*.98;
    oimsla = oimwla;
  %  [aa0,bb0] = size(oimwla);
    for kk = 1:Nx
        % etch in top layer
        oimwla(imtop(kk)+[-2 -1 0 1 2],curx(kk)) = maxabsim*ones(5,1);
        % etch in bottom layer
         oimwla(imfloor(kk)+[-2 -1 0 1 2],curx(kk)) = maxabsim*ones(5,1);
    end;
    % etch in intermediate layers
    for kk = 1:bb0
        for jj = 1:Nlayers
            rowvec = rowOffSet{1}(2)+BdryRelIm(kk,jj)+[-2 -1 0 1 2];
            Ivalid = find(rowvec >= 1 & rowvec <= aa0);
            if length(Ivalid) > 0
                oimwla(rowvec(Ivalid),kk+colOffSet{1}(2)) = maxabsim*ones(length(Ivalid),1);
            end;
        end;
    end;
     
     
     intLayerBdrys{ii}.top = imtop;
     intLayerBdrys{ii}.floor  = imfloor;
     intLayerBdrys{ii}.interMediateBdrys   = BdryRelIm+rowOffSet{1}(2)*ones(size(BdryRelIm));
     intLayerBdrys{ii}.curx = curx;
     intLayerBdrys{ii}.costFunc = [];
     intLayerBdrys{ii}.oimwla = oimwla;
     intLayerBdrys{ii}.curxaug = [colOffSet{1}(2)+1:colOffSet{2}(1)-1];
     figure(internalLayerPar.sliceFig);
     % imagesc(oimwla)
     imagesc(oimwla);colormap(cmap);title(['slice ',int2str(ii),' with boundaries']);
%hold on 
end;

    
    
    
