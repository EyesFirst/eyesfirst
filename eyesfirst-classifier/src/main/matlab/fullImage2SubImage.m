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

function [subColInd,subRowInd] = fullImage2SubImage(colInd,rowInd,colOffSet,rowOffSet);
subx = zeros(2,1);
subx(1) = colOffSet{1}(2)+1;
subx(2) = colOffSet{2}(1)-1;
I1 = find(colInd >= subx(1) & colInd <= subx(2));
subColInd = colInd(I1) - subx(1) +1;
subRowInd = rowInd(I1) - rowOffSet{1}(2);
end

