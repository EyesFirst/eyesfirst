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

function nsv = extractNormalScoreFeature_gf(cf,normPrctiles)
nsv = [];
testPrctiles = [90 95 99];
% normalPrctVec = [25 50 75 90 95 99];
normalPrctVec = [0 25 50 75 80 90 95 99 99.5 99.9];
[aap,bbp] = size(normPrctiles);
positionThresh = 0;
% nsv.file = curFileName;
% nsv.fileTag = fileTag;
nsv.NSlayer1 = [];
nsv.NSlayer2 = [];
nsv.NSlayer3 = [];
nsv.NSlayer4 = [];
Nclusters = length(cf);
for jj = 1:Nclusters
    if cf{jj}.clusterLayer.cfar == 1
        nsv.NSlayer1 = [nsv.NSlayer1 cf{jj}.normalScore1];
    elseif cf{jj}.clusterLayer.cfar == 2  && cf{jj}.clusterLayer.cfarProportionatePos >= positionThresh
        nsv.NSlayer2 = [nsv.NSlayer2 cf{jj}.normalScore1];
    elseif cf{jj}.clusterLayer.cfar == 3
        nsv.NSlayer3 = [nsv.NSlayer3 cf{jj}.normalScore1];
    elseif cf{jj}.clusterLayer.cfar == 4
        nsv.NSlayer4 = [nsv.NSlayer4 cf{jj}.normalScore1];
    end;
end;
testPrctileMat = zeros(4,length(testPrctiles));
testPrctileMat(1,:) = prctile(nsv.NSlayer1,testPrctiles);
testPrctileMat(2,:) = prctile(nsv.NSlayer2,testPrctiles);
testPrctileMat(3,:) = prctile(nsv.NSlayer3,testPrctiles);
testPrctileMat(4,:) = prctile(nsv.NSlayer4,testPrctiles);
nsv.prctileMat = testPrctileMat;
if ~isempty(normPrctiles)
    numberExceedNormPrctiles = zeros(4,bbp+1);
    exvec = zeros(bbp+1,1);
    exvec(1) = length(nsv.NSlayer1);
    for kk = 1:bbp
        I1 = find(nsv.NSlayer1  >= normPrctiles(1,kk));
        exvec(1+kk) = length(I1);
    end;
    numberExceedNormPrctiles(1,:) = exvec';
    exvec = zeros(bbp+1,1);
    exvec(1) = length(nsv.NSlayer2);
    for kk = 1:bbp
        I1 = find(nsv.NSlayer2  >= normPrctiles(2,kk));
        exvec(1+kk) = length(I1);
    end;
    numberExceedNormPrctiles(2,:) = exvec';
    exvec = zeros(bbp+1,1);
    exvec(1) = length(nsv.NSlayer3);
    for kk = 1:bbp
        I1 = find(nsv.NSlayer3  >= normPrctiles(3,kk));
        exvec(1+kk) = length(I1);
    end;
    numberExceedNormPrctiles(3,:) = exvec';
    exvec = zeros(bbp+1,1);
    exvec(1) = length(nsv.NSlayer4);
    for kk = 1:bbp
        I1 = find(nsv.NSlayer4  >= normPrctiles(4,kk));
        exvec(1+kk) = length(I1);
    end;
    numberExceedNormPrctiles(4,:) = exvec';
    nsv.numberExceedNormPrctiles = numberExceedNormPrctiles;
end;


