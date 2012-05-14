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

function pdm = pixelDensityMap(clmat,hdim,vdim,opat,thresh)
[aa,bb] = size(clmat);
pdm = zeros(aa,bb);
for ii= 1:aa
    if strcmpi(opat,'UC')
       minI = ii;
       maxI = min(ii+hdim-1,aa);
    elseif strcmpi(opat,'CC')
        minI = max(ii-floor((vdim-1)/2),1);
        maxI = min(ii+ceil((vdim-1)/2),bb);
    end;
    I = minI:maxI;
    for jj = 1:bb
        minJ = max(jj-floor((vdim-1)/2),1);
        maxJ = min(jj+ceil((vdim-1)/2),bb);
        J = minJ:maxJ;
        samp = clmat(I,J);
        N1s = sum(sum(samp));
        frac1 = N1s/(length(I)*length(J));
        if frac1 >= thresh
           pdm(ii,jj) = frac1;
        else
            pdm(ii,jj) = 0;
        end;
    end;
end;
