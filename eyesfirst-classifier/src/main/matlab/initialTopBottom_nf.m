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

function [cimTB,polyGate] = initialTopBottom_nf(cim,cgate,imposeBound)
[aa,bb] = size(cim);
maxval = max(max(cim));
sf = .99;
plotflag = 0;
cimTB = (255*sf)*(cim/maxval);
buf = 8;
costFuncFlag = 2; % least squares
boundFlag_lg = -1; % lower bound
boundFlag_ug = 1; %upperbound
pdeg_lg_or = 2; %4
pdeg_ug_or = 2; %3
constrainCoef = 1;
maxabspolyest = 1000;
xv = [1+buf:bb-buf+1]';
%imfigh = figure;
% figh = figure;
ss = 1;
% figOR = figure;
Nstd = 3;
pdeg_lg0 = 2;
pdeg_ug0 = 2;
maxUG = 900;
minUG = 200;
maxLG = 800;
minLG = 100;
%     clf(figh);
%     clf(figOR);
% for jj = 1:cc
    lowerGate = cgate(1,1+buf:bb-buf+1)';
    IL = find(lowerGate > 0);
    lowerGate2 = lowerGate(IL);
    xvl2 = xv(IL);
    upperGate = cgate(2,1+buf:bb-buf+1)';
    IU = find(upperGate > 0);
    upperGate2 = upperGate(IU);
    xvu2 = xv(IU);
    % lowerBound lowerGate
  %  plcolor = 'b';
    % remove outliers first 
    [roxvLG,royvLG,outIndLG] = removeOutLier_nf(xvl2,lowerGate2,pdeg_lg0,0,Nstd,1);
    maxabsLGpolyval = [];
    maxabsUGpolyval = [];
    maxLGpolyval = maxLG +1;
    maxUGpolyval = maxUG +1;
    minLGpolyval = minLG -1;
    minUGpolyval = minUG -1;
    pdeg_lg = pdeg_lg_or;
    
    hold on;
    plcolor = 'r';
    [roxvUG,royvUG,outIndUG] = removeOutLier_nf(xvu2,upperGate2,pdeg_ug0,0,Nstd,1);
    pdeg_ug = pdeg_ug_or;
   
    while (maxUGpolyval > maxUG || minUGpolyval <= minUG) && pdeg_ug >= 1
       cLB = -Inf*ones(pdeg_ug+1,1);
       cUB = Inf*ones(pdeg_ug+1,1);
       if constrainCoef == 1
           cUB(1) = 0;
       end;
       [pug,fval,exitflag,output] = estPolyBCwcb_nf(roxvUG,royvUG,pdeg_ug,boundFlag_ug,costFuncFlag,cLB,cUB);
       extrapug = polyval(pug,[1:ss:bb]);
       maxUGpolyval = max(extrapug);
       minUGpolyval = min(extrapug);
       pdeg_ug = pdeg_ug-1;
    end;
    
    
    while (( maxLGpolyval > maxLG || minLGpolyval <= minLG) && pdeg_lg >= 1)
        cLB = -Inf*ones(pdeg_lg+1,1);
        cUB = Inf*ones(pdeg_lg+1,1);
        if imposeBound == 1
            if pdeg_lg == length(pug)-1
                cUB(1) = min([0 pug(1)]);
            else
                cUB(1) = 0;
            end;
        end;
        [plg,fval,exitflag,output] = estPolyBCwcb_nf(roxvLG,royvLG,pdeg_lg,boundFlag_lg,costFuncFlag,cLB,cUB);
        extraplg = polyval(plg,[1:ss:bb]);
        maxLGpolyval = max(extraplg);
        minLGpolyval = min(extraplg);
        pdeg_lg = pdeg_lg-1;
    end;
%     hold on;
%     plcolor = 'r';
%     [roxvUG,royvUG,outIndUG] = removeOutLier(xvl2,upperGate2,pdeg_ug0,0,Nstd,figOR,1);
%     pdeg_ug = pdeg_ug_or;
%     while (maxUGpolyval > maxUG || minUGpolyval <= minUG) && pdeg_ug >= 1
%        [pug,fval,exitflag,output] = estPolyBCwcb(roxvUG,royvUG,pdeg_ug,boundFlag_ug,costFuncFlag,figh,plcolor);
%        extrapug = polyval(pug,[1:ss:bb]);
%        maxUGpolyval = max(extrapug);
%        minUGpolyval = min(extrapug);
%        pdeg_ug = pdeg_ug-1;
%     end;
    for kk = 1:bb
        lowbdry = floor(extraplg(kk));
        upbdry = ceil(extrapug(kk));
        if lowbdry > upbdry
            tempbdry = lowbdry;
            lowbdry = upbdry;
            upbdry = tempbdry;
        end;
        rowvec1 = min(max(2,lowbdry),aa-1)+[-1 0 1];
        rowvec2 = min(max(2,upbdry),aa-1)+[1 0 1];
        cimTB(rowvec1,kk) = 255*ones(3,1);
        cimTB(rowvec2,kk) = 255*ones(3,1);
    end;
    % clf(imfigh);
    %figure(imfigh);imagesc(squeeze(cimTB(:,:)));colorbar;colormap('gray');
  %   title(['slice ',int2str(jj)]);
   % fprintf('hit any key to continue\n');
  %  pause
   
%     clf(figh);
%     clf(figOR);
    polyGate.lower = plg;
    polyGate.upper = pug;
    polyGate.extrapLower = extraplg;
    polyGate.extrapUpper = extrapug;
% end;
% save(ofile,'polyGate','SAATB');