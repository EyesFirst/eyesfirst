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

function [colMinDepth,rowMinDepth,colGrossWidth,rowGrossWidth,rowZCmidPoint,colZCmidPoint] = zeroCrossingStats(rowZC,colZC)
nRows = numel(rowZC);
nCol = numel(colZC);
rowMinDepth = [];
colMinDepth = [];
rowGrossWidth = [];
colGrossWidth = [];
rowZCmidPoint = [];
colZCmidPoint = [];
for ii = 1:nRows
    if ~isempty(rowZC(ii).ZCMat)
       rowMinDepth = [rowMinDepth;rowZC(ii).ZCMat(7,:)'];
       rowGrossWidth = [rowGrossWidth;(rowZC(ii).ZCMat(5,:)-rowZC(ii).ZCMat(2,:))'];
       midPtRow = (round((rowZC(ii).ZCMat(5,:)+rowZC(ii).ZCMat(2,:))/2))';
       rowZCmidPoint = [rowZCmidPoint;[ii*ones(length(midPtRow),1) midPtRow]];
    end;
end;
for ii = 1:nCol
    if ~isempty(colZC(ii).ZCMat)
       colMinDepth = [colMinDepth;colZC(ii).ZCMat(7,:)'];
       colGrossWidth = [colGrossWidth;(colZC(ii).ZCMat(5,:)-colZC(ii).ZCMat(2,:))'];
       midPtCol = (round((colZC(ii).ZCMat(5,:)+colZC(ii).ZCMat(2,:))/2))';
       colZCmidPoint = [colZCmidPoint;[midPtCol ii*ones(length(midPtCol),1)]];
    end
end;



end

