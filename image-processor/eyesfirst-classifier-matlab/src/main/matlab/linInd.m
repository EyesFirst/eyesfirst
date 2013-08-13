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

function LI = linInd(I1,I2,I3,aa,bb,cc)
% I1, I2, I3 are the first, second, and third components of a cubical
% neighborhood 
% of a matrix of dimensions aaXbbXcc.  On return LI is the set of linear
% indices of the points
%

V1 = aa*bb*(I3-1);
V2 = aa*(I2-1);
V3 = I1;
n1 = length(V1);
n2 = length(V2);
n3 = length(V3);
M1 = repmat(V1',1,n2);
M2 = repmat(V2,n1,1);
M12 = M1+M2;
M123 = repmat(M12(:),1,n3)+repmat(V3,n1*n2,1);
LI = sort(M123(:));
