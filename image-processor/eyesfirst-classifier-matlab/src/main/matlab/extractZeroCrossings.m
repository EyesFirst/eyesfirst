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

function [rowZC,colZC] = extractZeroCrossings(g2im)
% g2im is an image.  On return ezc finds for every row and every column the
% zeros crossings of g2im such that the values inbetween successive zero
% crossings are negative.
[aa,bb] = size(g2im);
rowZC = struct('rowIndex',[],'ZCMat',[]);
colZC = struct('columnIndex',[],'ZCMat',[]);
for ii = 1:aa
    curVal = g2im(ii,:);
    ZCMat = findZeroCrossingsNegInterior(curVal);
    rowZC(ii).rowIndex = ii;
    rowZC(ii).ZCMat = ZCMat;
end;
for jj = 1:bb
    curVal = g2im(:,jj);
    ZCMat = findZeroCrossingsNegInterior(curVal);
    colZC(jj).columnIndex = jj;
    colZC(jj).ZCMat = ZCMat;
end

