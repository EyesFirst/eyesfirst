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

function intLayerBdrys = smoothBoundariesIntLayers(layerFile,saaFile,intLayerSmoothingPar)
% creates a smooth version of the layer boundary estimate and adds it to the layer File
plotFlag = intLayerSmoothingPar.plotFlag;
smoothFac  = intLayerSmoothingPar.smoothFac;
smoothxy  = intLayerSmoothingPar.smoothxy;
bdryfig = intLayerSmoothingPar.bdryfig;
imfig = intLayerSmoothingPar.imfig;
Nlayers = intLayerSmoothingPar.NintLayers;
NsmoothIter = intLayerSmoothingPar.NsmoothIter;
if isempty(smoothFac)
    smoothFac = .1;
end;
load(layerFile)
load(saaFile)
Nslices = length(intLayerBdrys);

% Nlayers = 3;
% figure(figh);
gmap = gray;
gmap(64,:) = [0 0 1];
for ii = 1:Nslices
    curx=intLayerBdrys{ii}.curxaug;
    curim = squeeze(SAA(:,:,ii));
    intLayerBdrys{ii}.smoothInterMediateBdrys = zeros(length(curx),Nlayers);
    for jj = 1:Nlayers
       bdry1 = intLayerBdrys{ii}.interMediateBdrys(:,jj);
%     bdry2 = intLayerBdrys{ii}.interMediateBdrys(:,2);
%     bdry3 = intLayerBdrys{ii}.interMediateBdrys(:,3);
      sf1 = csaps(curx,bdry1,smoothFac);
      smoothBdry1 = fnval(sf1,curx);
      intLayerBdrys{ii}.smoothInterMediateBdrys(:,jj) = smoothBdry1';
    end;
%     sf2 = csaps(curx,bdry2,smoothFac);
%     smoothBdry2 = fnval(sf2,curx);
%     sf3 = csaps(curx,bdry3,smoothFac);
%     smoothBdry3 = fnval(sf3,curx);
%     intLayerBdrys{ii}.smoothInterMediateBdrys= [smoothBdry1' smoothBdry2' smoothBdry3'];
end;



ylen = length(intLayerBdrys);
[zlen,xlen] = size(intLayerBdrys{1}.oimwla);
bdryCube = zeros(Nlayers,xlen,ylen);
imcube = zeros(zlen,xlen,ylen);
initx = zeros(ylen,1);
finalx = zeros(ylen,1);
for ii = 1:ylen
    curx = intLayerBdrys{ii}.curxaug;
    initx(ii)= curx(1);
    finalx(ii) = curx(end);
    %rowshift = intLayerBdrys{ii}.rowOffSet{1}(2);
   
   if smoothxy == 0
        bdryCube(:,curx,ii) = intLayerBdrys{ii}.smoothInterMediateBdrys';
   else
        bdryCube(:,curx,ii) = intLayerBdrys{ii}.interMediateBdrys';
   end
end

xstart = max(initx);
xend = min(finalx);
ystart = 1;
yend = 128;

sspx = .1;
sspy = .05; % spline smoothness parameter

x = [xstart:xend];
Lx = length(x);
y = [ystart:yend];
Ly = length(y);
for hh = 1:Nlayers
   z1 = squeeze(bdryCube(hh,x,y));
% z2 = squeeze(bdryCube(2,x,y));
% z3 = squeeze(bdryCube(3,x,y));
    if smoothxy == 1
        [spa,pm] = csaps({x,y},z1,[sspx sspy]);
        zval = fnval(spa,{x,y});
    else
        zval1 = zeros(Lx,Ly);
        for jj = 1:length(x)
            z1v = z1(jj,:);
            if hh == 1
                for kk = 1:NsmoothIter
                  [spa,pm] = csaps(y,z1v,sspy);
                  z1val = fnval(spa,y); 
                  if kk < NsmoothIter
                     IG = find(z1val > z1v);
                  else
                      IG = [];
                  end;
                  if ~isempty(IG)
                     z1val(IG) = z1v(IG);
                  end;
                  z1v = z1val;
                end;
            else
               [spa,pm] = csaps(y,z1v,sspy);
               z1val = fnval(spa,y);
            end;
           zval1(jj,:) = z1val;
        end
    end;
    for ii = 1:Nslices 
       intLayerBdrys{ii}.smooth2dInterMediateBdrys(:,hh) = zval1(:,ii);
    end
end;

% if smoothxy == 1
%     [spa,pm] = csaps({x,y},z2,[sspx sspy]);
%     zval2 = fnval(spa,{x,y});
% else
%     zval2 = zeros(Lx,Ly);
%     for jj = 1:length(x)
%         z1v = z2(jj,:);
%      %   for kk = 1:NsmoothIter
%           [spa,pm] = csaps(y,z1v,sspy);
%           z1val = fnval(spa,y); 
% %           IG = find(z1val > z1v);
% %           if ~isempty(IG)
% %              z1val(IG) = z1v(IG);
% %           end;
%        %   z1v = z1val;
%       %  end;
%        zval2(jj,:) = z1val;
%     end
% end;


% if smoothxy == 1
%     [spa,pm] = csaps({x,y},z3,[sspx sspy]);
%     zval3 = fnval(spa,{x,y});
% else
%     zval3 = zeros(Lx,Ly);
%     for jj = 1:length(x)
%         z1v = z3(jj,:);
%       %  for kk = 1:NsmoothIter
%           [spa,pm] = csaps(y,z1v,sspy);
%           z1val = fnval(spa,y); 
% %           IG = find(z1val > z1v);
% %           if ~isempty(IG)
% %              z1val(IG) = z1v(IG);
% %           end;
%          % z1v = z1val;
%        % end;
%        zval3(jj,:) = z1val;
%     end
% end;

for ii = 1:Nslices
    curim = squeeze(SAA(:,:,ii));
    curx = intLayerBdrys{ii}.curxaug;
   % intLayerBdrys{ii}.smooth2dInterMediateBdrys = [zval1(:,ii) zval2(:,ii) zval3(:,ii)]; % aligned to full image cube
    %rowshift = intLayerBdrys{ii}.rowOffSet{1}(2);
    maxabsim = max(max(abs(curim))); % check that this is not a NaN
    oimwsla2d = curim*.98;
    [aa0,bb0] = size(oimwsla2d);
    % bb = length(x);
    for jj = 1:Lx
        for kk = 1:Nlayers
          
           rowvec = ceil(intLayerBdrys{ii}.smooth2dInterMediateBdrys(jj,kk))+[-3 -2 -1 0 1 2 3]; % row alignment taken care of in construction of zval
           Ivalid = find(rowvec >= 1 & rowvec <= aa0);
           if length(Ivalid) > 0
            
             oimwsla2d(rowvec(Ivalid),x(jj)) = maxabsim*ones(length(Ivalid),1);
           end;
        end;
    end;
    if plotFlag == 1
        figure(imfig);
        imagesc(oimwsla2d);
        colormap(gmap);
        title(['slice ',int2str(ii)])
    end;
    
    intLayerBdrys{ii}.oimwsla2d = oimwsla2d;
    smoothThickness2dProf12 = zeros(1,bb0);
    smoothThickness2dProf13 = zeros(1,bb0);
    smoothThickness2dProf12(x) = (intLayerBdrys{ii}.smooth2dInterMediateBdrys(:,2)-intLayerBdrys{ii}.smooth2dInterMediateBdrys(:,1)+1)';
    smoothThickness2dProf13(x) = (intLayerBdrys{ii}.smooth2dInterMediateBdrys(:,3)-intLayerBdrys{ii}.smooth2dInterMediateBdrys(:,1)+1)';
    intLayerBdrys{ii}.smoothThickness2dProf12 = smoothThickness2dProf12;
    intLayerBdrys{ii}.smoothThickness2dProf13 = smoothThickness2dProf13;
    intLayerBdrys{ii}.oimwla = [];
    intLayerBdrys{ii}.costFunc = [];
    intLayerBdrys{ii}.smoothInterMediateBdrys = [];
    intLayerBdrys{ii}.interMediateBdrys = [];
end


% matPos = strfind(layerFile,'.mat');
% if isempty(matPos)
%     smoothlayerFile = [layerFile,'_smooth2d'];
% else
%     smoothlayerFile = [layerFile(1:matPos-1),'_smooth2d.mat'];
% end;
% save(smoothlayerFile,'intLayerBdrys');
