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

function  [polyGate,SAATB] =  initialLayerId_4b_v2(SAA,ofile,edf,ndil,sliceInd)
[aa,bb,cc] = size(SAA);
maxNumLayers = 10;
minmax = 1.0e-4;
minLowerGateVal = 24;
imageGate = zeros(2,bb,cc);
initLayers = zeros(maxNumLayers,6,bb,cc);
maxval = max(max(max(SAA)));
sf = .75;
plotflag = 0;
initBdryIm = (255*sf)*(SAA/maxval);
initclpd = [0.5; 0.5];
maxNumIt = 12;
initcl{1}.mean = 20;
initcl{1}.covm = 16;
initcl{2}.mean = 60;
initcl{2}.covm = 25;
plotFlag0 = 0;
% figh1 = figure;
 figh2 = figure;
% figTB1 = figure;
% figTBOR = figure;
% fighTBim = figure;
 figDens = figure;
 figMorph = figure;
 figSem = figure;
%gateInd = zeros(2,bb,cc);
polyGate = cell(cc,1);
SAATB = zeros(size(SAA));
maxjumpThresh = 50;
highStatePixFracThresh = 0.1;
nullGateVal = 5;
%for ii = 1:cc
    
%sliceInd provides start and end indices of slices to be processed
for ii = sliceInd(1):sliceInd(2)
    fprintf('STATUS:{"message":"Running gating (layer %d/%d)..."}\n', ii, sliceInd(2));
    curim = squeeze(SAA(:,:,ii));
    curimv = curim(:);
    I1 = find(curimv > 0);
    curimv2 = curimv(I1)';
    
    %Run sem classification algorithm to fit gaussian mixture model
    [clstat,clpd,cclmap,ppmclf,maclmap,stclest,maxclMapInd]=semclassify(curimv2,curimv2,initcl,initclpd,maxNumIt);
    initclpd = clpd;
    initcl = clstat;
    maxNumIt = 5;
    imv = zeros(aa*bb,1);
    imv(I1) = maxclMapInd;
    imv_bin = imv;
    I1 = find(imv == 1);
    imv_bin(I1) = zeros(size(I1));
    I2 = find(imv == 2);
    imv_bin(I2) = ones(size(I2));
    imv_bin = reshape(imv_bin,aa,bb);
    figure(figSem);imagesc(imv_bin);title('intensity classification')
    
    %define structured element for erosion and dilation
    sel = strel('rectangle',[edf,edf]);
    
    %apply image erosion to reduce noise in vitreous
    benchmark = now;
    imv_e_d = imerode(imv_bin,sel);
    benchmark = now - benchmark;
    %fprintf('BENCHMARK: Image erosion took ');
    %print_time(benchmark);
    %fprintf('\n');
    figure(figMorph);imagesc(imv_e_d);title('image erosion');
 
    for ll = 1:ndil
       %apply binary image dialation to reduce noise in retina produced
       %during erosion
       imv_e_d = imdilate(imv_e_d,sel);
    end;
    %imv_e_d = imdilate(imv_e_d1,sel);
    figure(figMorph);imagesc(imv_e_d);title('image dilation');
    %print(figMorph, '-dpng', sprintf('/Users/dpotter/EyesFirst/Another OCT/test3/debug_gating/layer_%d.png', ii));
  
    %generate pixel density map for computation of gradient image
    imv_e_d_20_3 = pixelDensityMap(imv_e_d,20,3,'UC',.5);
    imv_e_d_20_3_fr = filterRows(imv_e_d_20_3);
    %figure;imagesc(imv_e_d_20_3_fr)
    figure(figDens);imagesc(imv_e_d_20_3_fr); title('pixel density');
  
    %compute the gradient image, returned in gim, and if g2flag == 1, the derivative of the
    %gradient image in the direction of the gradient, returned in g2im
    [gim,g2im] = gradientImage2d(imv_e_d_20_3_fr,3,4,'imd_e_d_20_3');
    FirstNonEmptyNonNullGate = 0;
    LastEmptyGate = 0;
    for jj = 1:bb
        %if jj == 330
        %     fprintf('jj = \n',jj)
        %end;
        curvec2 = g2im(:,jj);
        curvec1 = gim(:,jj);
        if max(max(curvec2)) > minmax && max(max(curvec1)) > minmax 
            mt2 = 0.1*max(curvec2);
            mt1 = 0.1*max(curvec1);
            ppg = spaps([1:aa],curvec1,mt1);
            % pp2 = spaps([1:aa],curvec2,mt2);
            %find maxima in gradient as bounds of relevant region of an A
            %scan
           gate = extractGate3(ppg,plotFlag0,[]);
            if ~isempty(gate) && min(gate) > nullGateVal
               imageGate(:,jj,ii) = gate';
               if FirstNonEmptyNonNullGate == 0
                   FirstNonEmptyNonNullGate = jj;
                   % fill in the first empties
                   if FirstNonEmptyNonNullGate > 1
                      imageGate(:,1:FirstNonEmptyNonNullGate-1,ii) = repmat(gate',1,FirstNonEmptyNonNullGate-1);
                   end;
               end
            else
                if FirstNonEmptyNonNullGate ~= 0
                    imageGate(:,jj,ii) = imageGate(:,jj-1,ii);
                end;
            end
%             figure(figh1);
%             hold off
        else
            if FirstNonEmptyNonNullGate ~= 0
                imageGate(:,jj,ii) = imageGate(:,jj-1,ii);
            end;
        end;
%           cv = imv_e3_d3_20_3(:,jj);
%           mt1 = 0.1*max(cv);
%           pp1 = spaps([1:aa],cv,mt1);
%           pp1val = fnval(pp1,[1:aa]);
%           Ipo = find(pp1val > 0);
%           if length(Ipo) >= 2
%              gateInd(:,jj,ii) = [Ipo(1) Ipo(end)]';
%              initBdryIm([Ipo(1) Ipo(end)],jj,ii) = 255*ones(2,1);
%           end;  
    end
    if plotflag == 1
        figure(figh2)
        imagesc(squeeze(initBdryIm(:,:,ii)));colorbar;title(['initial layers slice ',int2str(ii)]);
    end;
    curGate = squeeze(imageGate(:,:,ii));
    curGateLower = curGate(1,:);
    curGateUpper = curGate(2,:);
    
    LGNN = find(curGateLower > 0);
    %extract jumps between adjacent gates
    lowerGateJumps = curGateLower(LGNN(1:end-1))-curGateLower(LGNN(2:end));
    IUJa = find(lowerGateJumps <= -maxjumpThresh); % jump up
    IDJa = find(lowerGateJumps >= maxjumpThresh);  % jump down
    [IUJ,IDJ] = sandwich(IUJa,IDJa);
    
    %if there are no jumps, no extrapolation necessary
    if isempty(IUJ) && isempty(IDJ)
        imposeBound = 0;
        extrapLowerGate = curGateLower;
    else %there are jumps, extrapolation is necessary based on jumps
        extrapLowerGate = curGateLower;
        %if there are only upward jumps
        if ~isempty(IUJ) && isempty(IDJ)
            %start low and one jump up
            if IUJ(1) > 1
                curSlope = curGateLower(LGNN(IUJ(1))) - curGateLower(LGNN(IUJ(1)-1));
            else
                curSlope = 0;
            end;
            highStateRuns = [LGNN(IUJ(1)+1) LGNN(end) LGNN(IUJ(1)) curGateLower(LGNN(IUJ(1))) curSlope]; %length(LGNN)-LGNN(IUJ(1))];
            extrapLowerGate(LGNN(IUJ(1)+1):LGNN(end)) = floor(max(minLowerGateVal,min(curGateLower(LGNN(IUJ(1)+1):LGNN(end)),floor(curGateLower(LGNN(IUJ(1))) + curSlope*([LGNN(IUJ(1)+1):LGNN(end)]-LGNN(IUJ(1)))))));
        %if there are only downward jumps
        elseif isempty(IUJ) && ~isempty(IDJ) 
            %start high and one jump down
           if IDJ(1) < LGNN(end)-1
                curSlope = curGateLower(LGNN(IDJ(1)+2)) - curGateLower(LGNN(IDJ(1)+1));
            else
                curSlope = 0;
            end;
            highStateRuns = [LGNN(1) LGNN(IDJ(1)) LGNN(IDJ(1)+1) curGateLower(LGNN(IDJ(1)+1)) curSlope];
            extrapLowerGate([LGNN(1):LGNN(IDJ(1))]) = floor(max(minLowerGateVal,min(curGateLower([LGNN(1):LGNN(IDJ(1))]),curGateLower(LGNN(IDJ(1)+1)) + curSlope*([LGNN(1):LGNN(IDJ(1))]-LGNN(IDJ(1)+1)))));

        %if there are both upward and downward jumps
        elseif ~isempty(IUJ) && ~isempty(IDJ)
            %[IUJ,IDJ] = sandwich(IUJ,IDJ);
            if IDJ(1) < IUJ(1) % first jump is down 
                NhighRuns = 1+length(IUJ);
                FJD = 1; % frst jump down flag
            else
                NhighRuns = length(IUJ);
                FJD = 0;
            end
            highStateRuns = zeros(NhighRuns,5);
            for ll = 1:NhighRuns
                if FJD == 1 
                    if ll == 1
                        if IDJ(1)+2 <= IUJ(1)
                            curSlope = curGateLower(LGNN(IDJ(1)+2)) - curGateLower(LGNN(IDJ(1)+1));
                        else
                            curSlope = 0; 
                        end;
                        highStateRuns(1,1:5) = [LGNN(1) LGNN(IDJ(1)) LGNN(IDJ(1)+1) curGateLower(LGNN(IDJ(1)+1)) curSlope];
                        extrapLowerGate(LGNN(1):LGNN(IDJ(1))) = floor(max(minLowerGateVal,min(curGateLower(LGNN(1):LGNN(IDJ(1))),floor(curGateLower(LGNN(IDJ(1))+1) + curSlope*([LGNN(1):LGNN(IDJ(1))]-LGNN(IDJ(1)+1))))));
                        % highStateRuns(1,2) = IDJ(1)-1;
                    else
                        highStateRuns(ll,1) = LGNN(IUJ(ll-1)+1);% IDJ(1)-1];
                        % determine run length 
                        njdi = find(IDJ >  IUJ(ll-1),1,'first');
                        njd = IDJ(njdi);
                        if ~isempty(njd)
                            highStateRuns(ll,2) = LGNN(IUJ(ll-1)) + njd-IUJ(ll-1);
                            curSlope = (curGateLower(LGNN(njd+1))-curGateLower(LGNN(IUJ(ll-1))))/(njd-IUJ(ll-1)+2);
                            highStateRuns(ll,[3:5]) = [LGNN((IUJ(ll-1))), curGateLower((IUJ(ll-1))), curSlope];
                            extrapLowerGate(LGNN(IUJ(ll-1))+1:LGNN(njd)) = floor(max(minLowerGateVal,floor(curGateLower(LGNN(IUJ(ll-1))) + curSlope*([LGNN(IUJ(ll-1))+1:LGNN(njd)]-LGNN(IUJ(ll-1)))))) ;
                        else
                            if IUJ(ll-1)-(IDJ(ll-1)+1) > 0
                                curSlope = curGateLower(LGNN(IUJ(ll-1))) - curGateLower(LGNN(IUJ(ll-1)-1));
                            else
                               curSlope = 0;
                            end
                            highStateRuns(ll,2:5) = [LGNN(end) LGNN(IUJ(ll-1)) curGateLower(LGNN(IUJ(ll-1))) curSlope];%
                            extrapLowerGate([LGNN(IUJ(ll-1))+1:LGNN(end)]) = floor(max(minLowerGateVal,min(curGateLower(LGNN(IUJ(ll-1))+1:LGNN(end)),floor(curGateLower(LGNN(IUJ(ll-1))) + curSlope*([LGNN(IUJ(ll-1))+1:LGNN(end)]-LGNN(IUJ(ll-1)))))));
                        end;
                    end;
                else
                    highStateRuns(ll,1) = LGNN(IUJ(ll))+1;
                     % determine run length 
                    njdi = find(IDJ >  IUJ(ll),1,'first');
                    njd = IDJ(njdi);
                    if ~isempty(njd)
                       % highStateRuns(ll,2) = LGNN(IUJ(ll)) + njd-IUJ(ll);
                       curSlope = (curGateLower(LGNN(njd+1)) - curGateLower(LGNN(IUJ(ll))))/(LGNN(njd+1) - LGNN(IUJ(ll)));
                       highStateRuns(ll,2:5) = [LGNN(njd), LGNN(njd+1), curGateLower(LGNN(IUJ(ll))), curSlope];
                       extrapLowerGate([LGNN(IUJ(ll))+1:LGNN(njd)]) = floor(min(curGateLower([LGNN(IUJ(ll))+1:LGNN(njd)]),floor(curGateLower(LGNN(IUJ(ll))) + curSlope*([LGNN(IUJ(ll))+1:LGNN(njd)]-LGNN(IUJ(ll))))));
                    else   
                        if IUJ(ll)- (IDJ(ll-1)+1) > 0
                            curSlope = curGateLower(LGNN(IUJ(ll))) - curGateLower(LGNN(IUJ(ll)-1));
                        else
                           curSlope = 0;
                        end
                        highStateRuns(ll,2:5) = [LGNN(end), LGNN(IUJ(ll)), curGateLower(LGNN(IUJ(ll))), curSlope]; %
                        extrapLowerGate([LGNN(IUJ(ll))+1:LGNN(end)]) = floor(max(minLowerGateVal,min(curGateLower([LGNN(IUJ(ll))+1:LGNN(end)]),floor(curGateLower(LGNN(IUJ(ll))) + curSlope*([LGNN(IUJ(ll))+1:LGNN(end)]-LGNN(IUJ(ll)))))));
                    end;
                end;
            end;
        end;
        imposeBound = 1;
%         highStatePix = sum((highStateRuns(:,2)-highStateRuns(:,1)+1));
%         if highStatePix/length(LGNN) >= highStatePixFracThresh
%             imposeBound = 1;
%             % interpolate/extrapolate across highState jumps
%             
%         else
%             imposeBound = 0;
%         end
    end;
%         if maxlowerjump > maxjumpThresh
%             imposeBound = 1;
%         else
%             imposeBound = 0;
%         end;
    % [curTB,curPolyGate] = initialTopBottom_v2(curim,curGate,figTB1,figTBOR,fighTBim);
    if imposeBound == 1
        curGate(1,:) = extrapLowerGate;
    end;
    % imposeBound2 = 1;
    %run spline fit against extrapolated gates
    [curTB,curPolyGate] = initialTopBottom_v3_nf(curim,curGate,imposeBound);
    SAATB(:,:,ii) = curTB;
    polyGate{ii}.lower = curPolyGate.lower;
    polyGate{ii}.upper = curPolyGate.upper;
    polyGate{ii}.extrapLower = curPolyGate.extrapLower;
    polyGate{ii}.extrapUpper = curPolyGate.extrapUpper;

    
end;
 save(ofile,'imageGate','initBdryIm','initLayers','SAATB','polyGate');
  