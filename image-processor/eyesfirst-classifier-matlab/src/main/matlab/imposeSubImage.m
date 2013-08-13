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

function supIm = imposeSubImage(curIm,I,J,repVal)
% replaces pixels with indices I(ii),J(ii) with value repVal
[aa,bb] = size(curIm);
if length(I) ~= length(J)
    error('index vectors must be the same length\n')
end;
if max(I) > aa || max(J) > bb || min(I) < 1 || min(J) < 1
    error('some index is out of range\n');
end;
Npix = length(I);
supIm = curIm;
for ii = 1:Npix
    supIm(I(ii),J(ii)) = repVal;
end;


