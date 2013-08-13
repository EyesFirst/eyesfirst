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

function interpEI = interpEnface(enfIm,trimOnlyFlag,normalizeFlag)
% removes columns of zeros
dft = 11.7188;
dst = 46.875;
sumIm = sum(enfIm);
fnzCol = find(sumIm ~= 0,1,'first');
lnzCol = find(sumIm ~=0, 1,'last');
enfImTZ = enfIm(:,[fnzCol:lnzCol]);
if normalizeFlag == 1
    enfImTZ = enfImTZ-min(min(enfImTZ));
    enfImTZ = enfImTZ/max(max(enfImTZ));
end
if ~trimOnlyFlag
    [aa,bb] = size(enfImTZ);
    % interpolate to form square pixels
    [rowG,colG] = ndgrid([1:aa],[1:bb]);
    F = griddedInterpolant(rowG,colG,enfImTZ,'cubic');
    rowSS = 1:(dft/dst):aa;

    [interpRow,interpCol] = ndgrid(rowSS,[1:bb]);
    interpEI = F(interpRow,interpCol);
else
    interpEI = enfImTZ;
end;



end

