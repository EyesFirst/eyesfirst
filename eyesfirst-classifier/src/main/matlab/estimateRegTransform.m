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

function [scFactor_sr,estTrans,resid,residPercent] = estimateRegTransform(imPts1,imPts2,ofile)
% [scFactor_sr,transFactor] = estimateRegTransform(imPts1,imPts2)
% imPts1 and imPts2 are lists of corresponding points in two images
% The transform from im1 to im2 is postulated to be a linear scaling in
% each dimension  plus a translation.  On return scFactor_sr is the vector
% of scale factors, and transFactor is the translation.
% the coordinates of imPts1 and imPts2 are  (column coordinates,row
% coordinates) from their respective images or (fastTime,slowTime) for
% enface OCT images



[Npts1,dim1] = size(imPts1);
[Npts2,dim2] = size(imPts2);
Npairs = Npts1*(Npts1-1)/2;
if Npts1 ~= Npts2 || dim1 ~= dim2
    error('size mismatch\n');
end
d1Mat = zeros(Npairs,dim1);
d2vec = zeros(Npairs,1);
ctPairs = 0;
% If Y = A+DX where D is diagonal, then D^2*delX^2 = delY^2
for ii = 1:Npts1
    pt11 = imPts1(ii,:);
    pt21 = imPts2(ii,:);
    for jj = (ii+1):Npts2
        pt12 = imPts1(jj,:);
        pt22 = imPts2(jj,:);
        ctPairs = ctPairs+1;
        d1Mat(ctPairs,:) = (pt11-pt12).^2; % squared coordinate distances
        d2vec(ctPairs) = sum((pt21-pt22).^2); % distance between pairs of points 
    end;
end;
scFactor = linsolve(d1Mat,d2vec); % scFactor*d1Mat = d2Vec in least squares sense
scFactor_sr = scFactor.^(.5); % first coordinate is the fast time, and second is the slow time
resid = d1Mat*scFactor-d2vec;
residPercent = ((d1Mat*scFactor-d2vec)./d2vec)*100;   
% solve for the translation
TRmat = diag(scFactor_sr);
imPts1t = imPts1';
imPts2t = imPts2';
multiTrans = imPts2t - TRmat*imPts1t; 
estTrans = mean(multiTrans,2);
save(ofile,'scFactor_sr','estTrans','resid','residPercent','imPts1','imPts2');
end

