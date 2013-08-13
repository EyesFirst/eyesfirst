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

function Im1rs = resampleImage(Im1,scFactor)
% Im1 is the image to be resampled
% scFactor(1) is the expansion factor in the x-direction, i.e., for the
% columns of the image, and scFactor(2) is the expansion factor in the
% y-direction, i.e., for the rows of the image
[aa,bb] = size(Im1);
[rowG,colG] = ndgrid([1:aa],[1:bb]);
F = griddedInterpolant(rowG,colG,Im1,'cubic');
Xss = 1:(1/scFactor(1)):bb;
Yss = 1:(1/scFactor(2)):aa;
[interpRow,interpCol] = ndgrid(Yss,Xss);
Im1rs = max(F(interpRow,interpCol),0);


end

