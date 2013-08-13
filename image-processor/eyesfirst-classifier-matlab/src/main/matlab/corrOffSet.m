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

function [offset,regCube,O2FrowInd,O2FcolInd] = corrOffSet(Im1,Im2,scFactor_sr)
% interpolate the oct image 
[aa1,bb1] = size(Im1); % note rows are slow-time and columns are fast-time
[rowG,colG] = ndgrid([1:aa1],[1:bb1]);
F = griddedInterpolant(rowG,colG,Im1,'cubic');
Xss = 1:(1/scFactor_sr(1)):bb1;
Yss = 1:(1/scFactor_sr(2)):aa1;
[interpRow,interpCol] = ndgrid(Yss,Xss);
im1Interp = max(F(interpRow,interpCol),0);
C = normxcorr2(im1Interp,Im2);
[I,J] = find(C==max(max(C)));
if length(I) == 1
    offset = [I,J] - size(im1Interp);
else
    error('correlation function has multiple maxima\n')
end
[aa2,bb2] = size(Im2);
regCube = zeros(aa2,bb2,3);
[aa1_i,bb1_i] = size(im1Interp);
O2FrowInd = [offset(1):(offset(1)+aa1_i-1)];
O2FcolInd = [offset(2):(offset(2)+bb1_i-1)];
regCube([offset(1):(offset(1)+aa1_i-1)],[offset(2):(offset(2)+bb1_i-1)],1)  = im1Interp;
regCube(:,:,2) = Im2;
maxRegCube = max(max(max(regCube)));
figure;imagesc(regCube*1/maxRegCube)
end

