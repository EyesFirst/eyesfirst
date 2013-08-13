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

function  ZCMat = findZeroCrossingsNegInterior(dv)
% zcMat = findZeroCrossingsNegInterior(dv)
% dv is a data vector.  On return zcMat is a 7XN matrix where N is the
% number of intervals on which dv is negative.  For each interval, dv(1,ii)
% is the last positive index of the run; dv(2,ii)
% is the interpolated zero crossing between the left hand zero crossing,
% dv(3,ii) is the index of the first negative of the run; dv(4,ii) is the index of the
% last negative of the run; and dv(5,ii) is the interpolated zero crossing
% at the right hand end, and dv(6,ii) is the first positive index after the
% run of negatives; dv(7,ii) is the minimum value on the run
Ineg = dv < 0;
Ipos2Neg = find(Ineg(2:end)- Ineg(1:(end-1)) == 1);
delVal = dv(Ipos2Neg) - dv(Ipos2Neg+1);
interpZC = Ipos2Neg+dv(Ipos2Neg)./delVal;
if Ineg(end) == 0
    NumZC = length(Ipos2Neg);
else
    NumZC = length(Ipos2Neg)-1;
end
ZCMat = zeros(7,NumZC);
for jj = 1:NumZC
    firstNegInd = Ipos2Neg(jj)+1;
    lastNegInd = firstNegInd+find(Ineg(firstNegInd:end) == 0,1,'first')-2;
    termZC = lastNegInd - (dv(lastNegInd)/(dv(lastNegInd+1)-dv(lastNegInd)));
    minVal = min(dv(firstNegInd:lastNegInd));
    ZCMat(:,jj) = [Ipos2Neg(jj);interpZC(jj);Ipos2Neg(jj)+1;lastNegInd;termZC;(lastNegInd+1);minVal];
end
ZCMat;

end

