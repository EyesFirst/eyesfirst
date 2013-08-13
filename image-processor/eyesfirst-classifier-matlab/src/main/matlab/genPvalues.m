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

function [pval,TN] = genPvalues(countVec,perctileVec)
% countVec is the number of counts for which a test statistic exceeds the
% percentiles in perctileVec.  The first perctile value is 0 and therefore
% the first count is the total number of counts.  On return, pvalueVec is
% the vector of probabilities of observing the counts separately and
% jointly
%
if perctileVec(1) ~= 0
    error('First entry in perctileVec must be 0\n');
end;
Npercentiles = length(perctileVec);
binCount = [countVec(1:end-1)-countVec(2:end) countVec(end)];
binProbs = [perctileVec(2:end)-perctileVec(1:end-1) 1-perctileVec(end)];
pval = mnpdf(binCount,binProbs);
TN = sum(binCount);

