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

function [nnim,rowOffSet,colOffSet] = extractNonNan2d_2(im)
[aa,bb]= size(im);
includeCol = zeros(bb,1);
includeRow = zeros(aa,1);
for ii = 1:bb
    curNan = isnan(im(:,ii));
    Inan = find(curNan == 1);
    if length(Inan) < aa
        includeCol(ii) = 1;
    end;
end
for ii = 1:aa
    curNan = isnan(im(ii,:));
    Inan = find(curNan == 1);
    if length(Inan) < bb
        includeRow(ii) = 1;
    end;
end

IncRow = find(includeRow == 1);
IncCol = find(includeCol == 1);
nnim = zeros(length(IncRow),length(IncCol));
for ii = 1:length(IncCol)
    nnim(:,ii) = im(IncRow,IncCol(ii));
end
rowOffSet{1} = [0 0];
rowOffSet{2} = [aa+1 aa+1];
colOffSet{1} = [0 0];
colOffSet{2} = [bb+1 bb+1];
exitLoopFlag = 0;
ctzeroints = 0;
curIncludeVec = includeRow;
while exitLoopFlag == 0
    firstZeroInd = find(curIncludeVec == 0,1,'first');
    curIndex = firstZeroInd;
    if ~isempty(firstZeroInd) 
        curVal = 0;
        lastZeroInd = find(curIncludeVec == 0,1,'last');
        while curVal == 0 && curIndex < lastZeroInd
            curIndex =  curIndex+1;
            curVal = curIncludeVec(curIndex);
        end;
        if firstZeroInd == 1
            ctzeroints = 1;
        else
            ctzeroints = 2;
        end;
        if curIndex == lastZeroInd
            curLastZeroInd = lastZeroInd;
        else
            curLastZeroInd = curIndex-1;
        end;
         rowOffSet{ctzeroints} = [firstZeroInd curLastZeroInd];
         curIncludeVec([firstZeroInd:curLastZeroInd]) = -1*ones(curLastZeroInd-firstZeroInd+1,1);
    else
        exitLoopFlag = 1;
    end
end;
%colOffSet = [];
exitLoopFlag = 0;
ctzeroints = 0;
curIncludeVec = includeCol;
while exitLoopFlag == 0
    firstZeroInd = find(curIncludeVec == 0,1,'first');
    curIndex = firstZeroInd;
    if ~isempty(firstZeroInd) 
        curVal = 0;
        lastZeroInd = find(curIncludeVec == 0,1,'last');
        while curVal == 0 && curIndex < lastZeroInd
            curIndex =  curIndex+1;
            curVal = curIncludeVec(curIndex);
        end;
        if firstZeroInd == 1
            ctzeroints = 1;
        else
            ctzeroints = 2;
        end;
        if curIndex == lastZeroInd
            curLastZeroInd = lastZeroInd;
        else
            curLastZeroInd = curIndex-1;
        end;
         colOffSet{ctzeroints} = [firstZeroInd curLastZeroInd];
         curIncludeVec([firstZeroInd:curLastZeroInd]) = -1*ones(curLastZeroInd-firstZeroInd+1,1);
    else
        exitLoopFlag = 1;
    end
end;
        





