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

function stLayerBdrys = smoothBoundaries2_v2(layerFile,saaFile,plotFlag,smoothFac,smoothxy,imfig,NsmoothIter)
% 
% creates a smooth version of the layer boundary estimate and adds it to the layer File
if isempty(smoothFac)
    smoothFac = .1;
end;
load(layerFile)
load(saaFile)
Nslices = length(stLayerBdrys);
Nlayers = 3;
% figure(figh);
gmap = gray;
gmap(64,:) = [0 0 1];
for ii = 1:Nslices
    curx = (stLayerBdrys{ii}.colOffSet{1}(2)+1):(stLayerBdrys{ii}.colOffSet{2}(1)-1);
    curim = squeeze(SAA(:,:,ii));
    bdry1 = stLayerBdrys{ii}.BdryRelIm(:,1);
    bdry2 = stLayerBdrys{ii}.BdryRelIm(:,2);
    bdry3 = stLayerBdrys{ii}.BdryRelIm(:,3);
    sf1 = csaps(curx,bdry1,smoothFac);
    smoothBdry1 = fnval(sf1,curx);
    sf2 = csaps(curx,bdry2,smoothFac);
    smoothBdry2 = fnval(sf2,curx);
    sf3 = csaps(curx,bdry3,smoothFac);
    smoothBdry3 = fnval(sf3,curx);
    stLayerBdrys{ii}.smoothBdryRelIm = [smoothBdry1' smoothBdry2' smoothBdry3'];
end;



ylen = length(stLayerBdrys);
[zlen,xlen] = size(stLayerBdrys{1}.oimwla);
bdryCube = zeros(3,xlen,ylen);
imcube = zeros(zlen,xlen,ylen);
initx = zeros(ylen,1);
finalx = zeros(ylen,1);
for ii = 1:ylen
    curx = (stLayerBdrys{ii}.colOffSet{1}(2)+1):(stLayerBdrys{ii}.colOffSet{2}(1)-1);
    initx(ii)= curx(1);
    finalx(ii) = curx(end);
    rowshift = stLayerBdrys{ii}.rowOffSet{1}(2);
   % colshift = stLayerBdrys{ii}.colOffSet{1}(2);
   if smoothxy == 0
        bdryCube(:,curx,ii) = rowshift*ones(3,length(curx))+stLayerBdrys{ii}.smoothBdryRelIm';
       % imcube(:,:,ii) = stLayerBdrys{ii}.oimwsla;
   else
        bdryCube(:,curx,ii) = rowshift*ones(3,length(curx))+stLayerBdrys{ii}.BdryRelIm';
       % imcube(:,:,ii) = stLayerBdrys{ii}.oimwla;
   end
end
% fprintf('stop here\n');
% if smoothxy == 1
%     topLayer = squeeze(bdryCube(1,:,:));
%     
% bdryfig = figure;
% imfig = figure;
xstart = max(initx);
xend = min(finalx);
ystart = 1;

% modified by Salim
yend = ylen;% 128;
% yend = 31;

sspx = .1;
sspy = .05; % spline smoothness parameter

x = [xstart:xend];
Lx = length(x);
y = [ystart:yend];
Ly = length(y);
z1 = squeeze(bdryCube(1,x,y));
z2 = squeeze(bdryCube(2,x,y));
z3 = squeeze(bdryCube(3,x,y));
if smoothxy == 1
    [spa,pm] = csaps({x,y},z1,[sspx sspy]);
    zval = fnval(spa,{x,y});
else
    zval1 = zeros(Lx,Ly);
    for jj = 1:length(x)
        z1v = z1(jj,:);
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
       zval1(jj,:) = z1val;
    end
end;

if smoothxy == 1
    [spa,pm] = csaps({x,y},z2,[sspx sspy]);
    zval2 = fnval(spa,{x,y});
else
    zval2 = zeros(Lx,Ly);
    for jj = 1:length(x)
        z1v = z2(jj,:);
     %   for kk = 1:NsmoothIter
          [spa,pm] = csaps(y,z1v,sspy);
          z1val = fnval(spa,y); 
%           IG = find(z1val > z1v);
%           if ~isempty(IG)
%              z1val(IG) = z1v(IG);
%           end;
       %   z1v = z1val;
      %  end;
       zval2(jj,:) = z1val;
    end
end;


if smoothxy == 1
    [spa,pm] = csaps({x,y},z3,[sspx sspy]);
    zval3 = fnval(spa,{x,y});
else
    zval3 = zeros(Lx,Ly);
    for jj = 1:length(x)
        z1v = z3(jj,:);
      %  for kk = 1:NsmoothIter
          [spa,pm] = csaps(y,z1v,sspy);
          z1val = fnval(spa,y); 
%           IG = find(z1val > z1v);
%           if ~isempty(IG)
%              z1val(IG) = z1v(IG);
%           end;
         % z1v = z1val;
       % end;
       zval3(jj,:) = z1val;
    end
end;

for ii = 1:Nslices
    curim = squeeze(SAA(:,:,ii));
    curx = (stLayerBdrys{ii}.colOffSet{1}(2)+1):(stLayerBdrys{ii}.colOffSet{2}(1)-1);
    stLayerBdrys{ii}.smooth2dBdryRelIm = [zval1(:,ii) zval2(:,ii) zval3(:,ii)]; % aligned to full image cube
    rowshift = stLayerBdrys{ii}.rowOffSet{1}(2);
    maxabsim = max(max(abs(curim))); % check that this is not a NaN
    oimwsla2d = curim*.98;
    [aa0,bb0] = size(oimwsla2d);
    % bb = length(x);
    for jj = 1:Lx
        for kk = 1:Nlayers
          % rowvec = rowshift+ceil(stLayerBdrys{ii}.smooth2dBdryRelIm(jj,kk))+[-3 -2 -1 0 1 2 3];
           rowvec = ceil(stLayerBdrys{ii}.smooth2dBdryRelIm(jj,kk))+[-3 -2 -1 0 1 2 3]; % row alignment taken care of in construction of zval
           Ivalid = find(rowvec >= 1 & rowvec <= aa0);
           if length(Ivalid) > 0
             % oimwsla2d(rowvec(Ivalid),jj+stLayerBdrys{ii}.colOffSet{1}(2)) = maxabsim*ones(length(Ivalid),1);
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
    stLayerBdrys{ii}.oimwsla2d = oimwsla2d;
    smoothThickness2dProf12 = zeros(1,bb0);
    smoothThickness2dProf13 = zeros(1,bb0);
    smoothThickness2dProf12(x) = (stLayerBdrys{ii}.smooth2dBdryRelIm(:,2)-stLayerBdrys{ii}.smooth2dBdryRelIm(:,1)+1)';
    smoothThickness2dProf13(x) = (stLayerBdrys{ii}.smooth2dBdryRelIm(:,3)-stLayerBdrys{ii}.smooth2dBdryRelIm(:,1)+1)';
    stLayerBdrys{ii}.smoothThickness2dProf12 = smoothThickness2dProf12;
    stLayerBdrys{ii}.smoothThickness2dProf13 = smoothThickness2dProf13;
    % remove unnecessary output
    stLayerBdrys{ii}.oimwla = [];
    % This is not unnecessary:
    %stLayerBdrys{ii}.BdryRelIm = [];
    stLayerBdrys{ii}.smoothBdryRelIm = [];
    stLayerBdrys{ii}.thicknessProf12 = [];
    stLayerBdrys{ii}.thicknessProf13 = [];
    stLayerBdrys{ii}.smoothBdryRelIm = [];
    stLayerBdrys{ii}.x_smooth2d = x;
end
% matPos = strfind(layerFile,'.mat');
% if isempty(matPos)
%     smoothlayerFile = [layerFile,'_smooth2d'];
% else
%     smoothlayerFile = [layerFile(1:matPos-1),'_smooth2d.mat'];
% end;
% save(smoothlayerFile,'stLayerBdrys');

