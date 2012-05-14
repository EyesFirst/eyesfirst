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

function exceedClust = clusterCFAR(cfarIm,thresh1,thresh2,d1,d2,d3)
% thresh1 >= thresh2
sizeIm = size(cfarIm);
%BinIm = find(cfarIm >= thresh);
imDim = length(sizeIm);
% CmT = cfarIm - thresh*ones(size(cfarIm));
% thIm = ones(size(cfarIm))-~max(CmT,zeros(size(CmT)));
% cs = thIm;
% VIm = cfarIm.*thIm;
% VIm = VIm(:);
ci = 1;
exceedClust = [];
ctClust = 0;
VIm1 = cfarIm(:);
VIm2 = cfarIm(:);
if imDim == 3
    [aa,bb,cc] = size(cfarIm);
   % binaryIm = ones(size(VIm));
    I01 = find(VIm1 < thresh1);
   % binaryIm(I0) = zeros(size(I0));
    VIm1(I01) = zeros(size(I01));
    I02 = find(VIm2 < thresh2);
   % binaryIm(I0) = zeros(size(I0));
    VIm2(I02) = zeros(size(I02));
    while ~isempty(ci)
       ci = find(VIm1 > 0,1,'first');
       if ~isempty(ci)
           curClust = [];
           ctClust = ctClust+1;
           ptCur = 1;
           cubeInd = cubeMatInd(ci,aa,bb,cc);
           VIm1(ci) = 0;
           VIm2(ci) = 0;
           curClust = [curClust; ci cubeInd];
           curHood = neighborhood(ci,d1,d2,d3,aa,bb,cc); % excludes ci
           curHoodVal = VIm2(curHood(:,1)); 
           I1 = find(curHoodVal >= thresh2); 
           curClust = [curClust;curHood(I1,:)];
           VIm1(curHood(I1,1)) = 0;
           VIm2(curHood(I1,1)) = 0;
           ptLast = size(curClust,1);
           ptCur = min(ptCur+1,ptLast+1);
           while ptLast >= ptCur
               ci = curClust(ptCur,1);
               curHood = neighborhood(ci,d1,d2,d3,aa,bb,cc); % excludes ci
               curHoodVal = VIm2(curHood(:,1));
               I1 = find(curHoodVal >= thresh2);
               if ~isempty(I1)
                  curClust = [curClust;curHood(I1,:)];
                  VIm1(curHood(I1,1)) = 0;
                  VIm2(curHood(I1,1)) = 0;
                  ptLast = size(curClust,1);
               end
               ptCur = min(ptCur+1,ptLast+1);
           end;
           exceedClust{ctClust}.clust = curClust;
       end;
    end;
elseif imDim == 2
    [aa,bb] = size(cfarIm);
    I01 = find(VIm1 < thresh1);
   % binaryIm(I0) = zeros(size(I0));
    VIm1(I01) = zeros(size(I01));
    I02 = find(VIm2 < thresh2);
   % binaryIm(I0) = zeros(size(I0));
    VIm2(I02) = zeros(size(I02));
    while ~isempty(ci)
       ci = find(VIm1 > 0,1,'first');
       if ~isempty(ci)
           curClust = [];
           ctClust = ctClust+1;
           ptCur = 1;
           cubeInd = rectangleMatInd(ci,aa);
           VIm1(ci) = 0;
           VIm2(ci) = 0;
           curClust = [curClust; ci cubeInd];
           curHood = neighborhood2d(ci,d1,d2,aa,bb); % excludes ci
           curHoodVal = VIm2(curHood(:,1)); 
           I1 = find(curHoodVal >= thresh2); 
           curClust = [curClust;curHood(I1,:)];
           VIm1(curHood(I1,1)) = 0;
           VIm2(curHood(I1,1)) = 0;
           ptLast = size(curClust,1);
           ptCur = min(ptCur+1,ptLast+1);
           while ptLast >= ptCur
               ci = curClust(ptCur,1);
               curHood = neighborhood2d(ci,d1,d2,aa,bb); % excludes ci
               curHoodVal = VIm2(curHood(:,1));
               I1 = find(curHoodVal >= thresh2);
               if ~isempty(I1)
                  curClust = [curClust;curHood(I1,:)];
                  VIm1(curHood(I1,1)) = 0;
                  VIm2(curHood(I1,1)) = 0;
                  ptLast = size(curClust,1);
               end
               ptCur = min(ptCur+1,ptLast+1);
           end;
           exceedClust{ctClust}.clust = curClust;
       end;
    end;
end;
               
           
           
       
       
    
    


