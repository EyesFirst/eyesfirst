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

function imout = filterRows(imin);
[aa,bb] = size(imin);
sumImIn = sum(imin,2);
supInt = supportIntervals([1:aa],sumImIn);
Nints = length(supInt);
weightFac = .05;
rowFilt = ones(aa,bb);
if Nints == 1
   imout = imin;
else
    supvals = zeros(Nints,1);
    for ii = 1:Nints
        supvals(ii) = supInt{ii}.weight;
    end;
    [maxwt,maxind] = max(supvals);
    if maxind == 1
        imout = imin;
    else
        I1 = find(supvals <= maxwt*weightFac);
        I2 = find(I1 < maxind,1,'last');
        if ~isempty(I2)
           lastZeroRow = supInt{I2}.interval(end);
           rowFilt(1:lastZeroRow,:) = zeros(lastZeroRow,bb);
           imout = imin.*rowFilt;
        else
            imout = imin;
        end;
    end;
end;
        
        