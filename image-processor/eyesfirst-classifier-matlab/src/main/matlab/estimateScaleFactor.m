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

function [scFactor_sr,residPercent,resid] = estimateScaleFactor(imPts1,imPts2,Im1,Im2)
% estimates the diagonal matrix of a linear transform hypothesized to be a
% translation plus a transformation by a positive diagonal matrix.  ImPts1 and
% ImPts2 are lists of corresponding points, where each row is a point, in
% the two spaces.  
[Npts1,dim1] = size(imPts1);
[Npts2,dim2] = size(imPts2);
Npairs = Npts1*(Npts1-1)/2;
if Npts1 ~= Npts2 || dim1 ~= dim2
    error('size mismatch\n');
end
d1Mat = zeros(Npairs,dim1);
d2vec = zeros(Npairs,1);
ctPairs = 0;
for ii = 1:Npts1
    pt11 = imPts1(ii,:);
    pt21 = imPts2(ii,:);
    for jj = (ii+1):Npts2
        pt12 = imPts1(jj,:);
        pt22 = imPts2(jj,:);
        ctPairs = ctPairs+1;
        d1Mat(ctPairs,:) = (pt11-pt12).^2;
        d2vec(ctPairs) = sum((pt21-pt22).^2);
    end;
end;
scFactor = linsolve(d1Mat,d2vec);
scFactor_sr = scFactor.^(.5);
resid = d1Mat*scFactor-d2vec;
residPercent = ((d1Mat*scFactor-d2vec)./d2vec)*100;   
% solve for the translation
TRmat = diag(scFactor_sr);
imPts1t = imPts1';
imPts2t = imPts2';
multiTrans = imPts2t - TRmat*imPts1t;
estTrans = mean(multiTrans,2);
% registration version 1 image 1 to image 2
% no interpolation
registerIm1 = zeros(size(Im2));
[aa1,bb1] = size(Im1);
[aa2,bb2] = size(Im2);
for ii = 1:aa1 % row index or y-coordinate
    for jj = 1:bb1 % column index or x-coordinate
        trInd = round(estTrans+TRmat*[jj;ii]); % note that the order of the components is (fast-time (jj),slow-time(ii))
        if trInd(1) <= bb2 & trInd(2) <= aa2
           registerIm1(trInd(2),trInd(1)) = Im1(ii,jj); % note that rows are slow-time and columns are fast-time
        end;
    end;
end;
regCube1 = zeros(aa2,bb2,3);
regCube1(:,:,1) = registerIm1;
regCube1(:,:,2) = Im2;
% interpolate the oct image 
[rowG,colG] = ndgrid([1:aa1],[1:bb1]);
F = griddedInterpolant(rowG,colG,Im1,'cubic');
Xss = 1:(1/scFactor_sr(1)):498;
Yss = 1:(1/scFactor_sr(2)):512;
[interpRow,interpCol] = ndgrid(Yss,Xss);
im1Interp = F(interpRow,interpCol);
C = normxcorr2(im1Interp,Im2);
[I,J] = find(C==max(max(C)));
if length(I) == 1
    offset = [I,J] - size(im1Interp)
else
    error('correlation function has multiple maxima\n')
end
offset(1) = offset(1);
regCube2 = zeros(aa2,bb2,3);
[aa1_i,bb1_i] = size(im1Interp);
regCube2([offset(1):(offset(1)+aa1_i-1)],[offset(2):(offset(2)+bb1_i-1)],1)  = im1Interp;
regCube2(:,:,2) = Im2;
end

