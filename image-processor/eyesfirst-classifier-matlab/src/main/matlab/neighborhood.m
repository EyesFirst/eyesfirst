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

function cnHood = neighborhood(ci,d1,d2,d3,aa,bb,cc)
% ci is a linear index into a 3d array of size aXbXc
% d1, d2, and d3 are integers that define a cubical neighborhood of 
% ci of size d1, d2, and d3 in dimensions 1, 2, and 3 respectively
% on return cnHood is a four column matrix such that each row is a point
% in the neighborhood of ci and the columns are 1) the linear index, and
% the three matrix indices of the point. 
%
ciCubeInd = cubeMatInd(ci,aa,bb,cc);
I1a = ciCubeInd(1)+[-d1:d1];
I1g0 = find(I1a > 0 & I1a <= aa);
I1 = I1a(I1g0);
I2a = ciCubeInd(2)+[-d2:d2];
I2g0 = find(I2a > 0 & I2a <= bb);
I2 = I2a(I2g0);
I3a = ciCubeInd(3)+[-d3:d3];
I3g0 = find(I3a > 0 & I3a <= cc);
I3 = I3a(I3g0);
LI = linInd(I1,I2,I3,aa,bb,cc);
cubeInd = cubeMatInd(LI,aa,bb,cc);
Nhood = size(cubeInd,1)-1;
ii = find(LI == ci);
if ii == 1
   curHood = cubeInd(2:end,:);
   cnHood = [LI(2:end) curHood];
elseif ii > 1 && ii <= Nhood
   curHood = [cubeInd(1:ii-1,:);cubeInd(ii+1:Nhood+1,:)];
   cnHood = [[LI(1:ii-1);LI(ii+1:Nhood+1)] curHood];
else
    curHood = [cubeInd(1:Nhood,:)];
    cnHood = [LI(1:Nhood) curHood];
end;


    

