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

function jjCubeInd = cubeMatInd(ii,aa,bb,cc)
kk = floor((ii-1)/(aa*bb))+1;
kk2 = ii-(aa*bb*(kk-1));
jj = floor((kk2-1)/aa)+1;
iinew = kk2-(jj-1)*aa;
jjCubeInd = [iinew ,jj ,kk];
