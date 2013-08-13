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

function thickFeatures = calculateThicknessStats(thicknessMapFile,statPar,pixelDim)
skewnessFlag = statPar.skewnessFlag; % if 1 does not correct for bias; this is the default.  If 0 then corrects for bias
kurtosisFlag = statPar.kurtosisFlag; % if 1 does not correct for bias; this is the default.  If 0 then corrects for bias
NanBuf = 20;
minThick = 25;
di = pixelDim.slowTime; % microns per pixel slow time 
dj = pixelDim.fastTime; % microns per pixel fast time
% dk = pixelDim.axial; % microns per pixel axial
thicknessSF = 1; % dk/5; % thickness scale factor
load(thicknessMapFile)
ssp = .01; % spline smoothness parameter
[aa,bb] = size(intenseMap1);

% modified by Salim
x=[1:aa];
% x = [1:128];
% x = [1:31];

Lx = length(x);
y = [NanBuf+1:bb-NanBuf];
Ly = length(y);
z = intenseMap1(x,y);
[spa,pm] = csaps({x,y},z,[ssp ssp]);
figure;fnplt(spa);
% calculate gradient and Hessian of spa:
gspa = fndir(spa,eye(2));
Hspa = fndir(gspa,eye(2)); % note that if the value at a point is reshaped to 2X2 then the result is the Hessian matrix at the point
eigImage = zeros(2,Lx,Ly);
Hval = fnval(Hspa,{x,y});
gval = fnval(gspa,{x,y});
 f =  @(x)splineVal(x,spa,gspa,Hspa);
 x0 = [x(floor(Lx/2));y(floor(Ly/2))];
 options = optimset;
 options = optimset(options,'derivativeCheck','off');
 options = optimset(options,'diagnostics','on');
 options = optimset(options,'display','iter');
 options = optimset(options,'gradObj','on');
 options = optimset(options,'largeScale','on');
 options = optimset(options,'Hessian','on');
[cp,fcp,exitflag,output,grad,hessian] = fminunc(f,x0,options);
sector{1}.range = [0 500]; % radius in microns
sector{1}.ang = [-pi pi];
sector{2}.range = [500 1500];
sector{2}.ang = [-pi/4 pi/4];
sector{3}.range = [500 1500];
sector{3}.ang = [pi/4 3*pi/4];
sector{4}.range = [500 1500];
sector{4}.ang = [3*pi/4 -3*pi/4];
sector{5}.range = [500 1500];
sector{5}.ang = [-3*pi/4 -pi/4];
sector{6}.range = [1500 3000];
sector{6}.ang = [-pi/4 pi/4];
sector{7}.range = [1500 3000];
sector{7}.ang = [pi/4 3*pi/4];
sector{8}.range = [1500 3000];
sector{8}.ang = [3*pi/4 -3*pi/4];
sector{9}.range = [1500 3000];
sector{9}.ang = [-3*pi/4 -pi/4];

imageDim = [aa,bb];
centerInd = cp;
regionInd = polarSector(centerInd,sector,imageDim,di,dj);
% get statistics
Nsectors = length(sector);
stStats = cell(Nsectors,1);
thickMap1 = thickMap1*thicknessSF;
thickMap2 = thickMap2*thicknessSF;
thickMapAve = (thickMap1+thickMap2)/2;
meanMap1 = zeros(size(thickMap1));
medianMap1 = zeros(size(thickMap1));
meanMap2 = zeros(size(thickMap1));
medianMap2 = zeros(size(thickMap1));
meanMapAve = zeros(size(thickMap1));
medianMapAve = zeros(size(thickMap1));

thickRange1 = -Inf*ones(2,Nsectors);
thickRange2 = -Inf*ones(2,Nsectors);
thickRangeAve = -Inf*ones(2,Nsectors);
for ii = 1:Nsectors
    curI = regionInd{ii}.I;
    curJ = regionInd{ii}.J;
    if ~isempty(curI)
        LI = length(curI);
        kk = length(curI);
        thick1t = zeros(LI,1);
        thick2t = zeros(LI,1);
        for kk = 1:LI
            thick1t(kk) = thickMap1(curI(kk),curJ(kk));
            thick2t(kk) = thickMap2(curI(kk),curJ(kk));
        end;
        Igmin = find(thick1t > minThick & thick2t > minThick);
        thick1 = thick1t(Igmin);
        thick2 = thick2t(Igmin);
        stStats{ii}.thick1 = thick1;
        stStats{ii}.thick2 = thick2;
        thickAve = (thick1+thick2)/2;
        stStats{ii}.thickAve = thickAve;
        thickRange1(1,ii) = min(thick1);
        thickRange1(2,ii) = max(thick1);
        thickRangeAve(1,ii) = min(thickAve);
        thickRangeAve(2,ii) = max(thickAve);
        thickRange2(1,ii) = min(thick2);
        thickRange2(2,ii) = max(thick2);
        stStats{ii}.I = curI;
        stStats{ii}.J = curJ;
        stStats{ii}.meanThick1 = mean(thick1);
        stStats{ii}.stdThick1 = std(thick1);
        stStats{ii}.medianThick1 = median(thick1);
        stStats{ii}.meanThick2 = mean(thick2);
        stStats{ii}.stdThick2 = std(thick2);
        stStats{ii}.medianThick2=  median(thick2);
        stStats{ii}.meanThickAve = mean(thickAve);
        stStats{ii}.stdThickAve = std(thickAve);
        stStats{ii}.medianThickAve=  median(thickAve);
        stStats{ii}.skewness1 = skewness(thick1,skewnessFlag);
        stStats{ii}.skewness2 = skewness(thick2,skewnessFlag);
        stStats{ii}.skewnessAve = skewness(thickAve,skewnessFlag);
        stStats{ii}.kurtosis1 = kurtosis(thick1,kurtosisFlag);
        stStats{ii}.kurtosis2 = kurtosis(thick2,kurtosisFlag);
        stStats{ii}.kurtosisAve = kurtosis(thickAve,kurtosisFlag);
        stStats{ii}.field = ii;
    else
        stStats{ii}.meanThick1 = 0;% mean(thick1);
        stStats{ii}.stdThick1 = 0;% std(thick1);
        stStats{ii}.medianThick1 = 0; %median(thick1);
        stStats{ii}.meanThickAve = 0;% mean(thick1);
        stStats{ii}.stdThickAve = 0;% std(thick1);
        stStats{ii}.medianThickAve = 0; %median(thick1);
        stStats{ii}.meanThick2 = 0;  %mean(thick2);
        stStats{ii}.stdThick2 = 0; %std(thick2);
        stStats{ii}.medianThick2=  0; %median(thick2);
        stStats{ii}.skewness1 = nan; % skewness(thick1,skewnessFlag);
        stStats{ii}.skewness2 = nan; %skewness(thick2,skewnessFlag);
        stStats{ii}.skewnessAve = nan; %skewness(thickAve,skewnessFlag);
        stStats{ii}.kurtosis1 = nan; %kurtosis(thick1,kurtosisFlag);
        stStats{ii}.kurtosis2 = nan; %kurtosis(thick2,kurtosisFlag);
        stStats{ii}.kurtosisAve = nan; %kurtosis(thickAve,kurtosisFlag);
    end;
    for rr = 1:LI
        meanMap1(curI(rr),curJ(rr)) = stStats{ii}.meanThick1;
        meanMapAve(curI(rr),curJ(rr)) = stStats{ii}.meanThickAve;
        meanMap2(curI(rr),curJ(rr)) = stStats{ii}.meanThick2;
    end;
end

% determine thickness range
I1nn = find(thickRange1(1,:)>0);
I2nn = find(thickRange2(1,:)>0);
maps.meanMap1 = meanMap1;
maps.meanMapAve = meanMapAve;
maps.meanMap2 = meanMap2;
Icentral = floor(cp(1))+ [-1 0 1];
Jcentral = floor(cp(2))+ [-1 0 1];
centThick1 = thickMap1(Icentral,Jcentral);
centThick2 = thickMap2(Icentral,Jcentral);
centThickAve = thickMapAve(Icentral,Jcentral);
stStats{Nsectors+1}.I = Icentral;
stStats{Nsectors+1}.J = Jcentral;
stStats{Nsectors+1}.thick1 = centThick1;
stStats{Nsectors+1}.thick2 = centThick2;
stStats{Nsectors+1}.thickAve = centThickAve;
thickFeatures.cp = cp;
thickFeatures.stats = stStats;
cpx = cp(1);
cpy = cp(2);
Hval = fnval(Hspa,{cpx,cpy});
Hval = reshape(Hval,[2,2]);
thickFeatures.Hessian = Hval;
thickFeatures.maps = maps;
%save(statfile,'thickFeatures');


