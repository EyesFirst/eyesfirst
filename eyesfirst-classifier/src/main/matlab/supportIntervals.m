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

function support = supportIntervals(funcdomain,funcvalue);
% asumes that the function is non-negative and finds the
% support intervals, ie intervals on which the funcvalues are greater than
% zero and the integral of the function on the intervals
minval = eps;
Isupp = find(funcvalue > minval);
IsupDif = Isupp(2:end) - Isupp(1:end-1);
Ibreak = find(IsupDif > 1);
NintSup = length(Ibreak)+1;
support = cell(NintSup,1);
suppStart = Isupp(1);
for ii = 1:NintSup-1
    support{ii}.interval = [suppStart:funcdomain(Isupp(Ibreak(ii)))];
    support{ii}.weight = sum(funcvalue([suppStart:funcdomain(Isupp(Ibreak(ii)))]));
    suppStart = Isupp(Ibreak(ii)+ 1);
end;
support{NintSup}.interval = [suppStart:funcdomain(Isupp(end))];
support{NintSup}.weight = sum(funcvalue([suppStart:funcdomain(Isupp(end))]));
end

