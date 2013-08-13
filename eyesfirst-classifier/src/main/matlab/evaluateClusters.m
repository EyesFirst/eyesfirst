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

function [cf,nsv,pv,cv,stCounts,cpd,cpfa,candidateHE,HEscore] = evaluateClusters(cfarFile,clusterFile,boundaryFile,cfPar,pixelDim)
statsFile = cfPar.statsFile;
problemFiles = [];

if ~isempty(statsFile)
   load(statsFile);
else
    stats = [];
end
splineFile = cfPar.splineFile;
load(splineFile);
pdvpfa = ppval([0:.01:1],pp_Pd_Vs_Pfa);

normPrctFile = cfPar.normPrctFile;
load(normPrctFile);
pvec = .01*[0 25 50 75 80 90 95 99 99.5 99.9];
pvec3 = pvec([1,  7]);
% plsym = {'rx','k*'};
layerScoreThresh = [[2 3 4]' normPrctiles([2 3 4],8)]; % threshhold is the 99th percentile

load(clusterFile)
load(cfarFile);
sizeCFAR = size(cfarSAA);
load(boundaryFile);
cf = clusterFeatures(cfarSAA,exceedClusters,intLayerBdrys_smooth2d,stats1,pixelDim); % uses proportionate position in layer 2 as a feature
% save(outFileList{ii},'cf');
nsv = extractNormalScoreFeature_gf(cf,normPrctiles);
[pv2,cv2] = genPvalues(nsv.numberExceedNormPrctiles(2,[1,9]),pvec3); % note that first column is the count; 9th column is the 99th percentiloe
[pv3,cv3] = genPvalues(nsv.numberExceedNormPrctiles(3,[1, 9]),pvec3);
[pv4,cv4] = genPvalues(nsv.numberExceedNormPrctiles(4,[1, 9]),pvec3);
stCounts2 = nsv.numberExceedNormPrctiles(2,[1,9])';% number of clusters in layer 2 and number exceeding 99 percentile
stCounts3 = nsv.numberExceedNormPrctiles(3,[1,9])';% number of clusters in layer 3 and number exceeding 99 percentile
stCounts4 = nsv.numberExceedNormPrctiles(4,[1,9])';% number of clusters in layer 4 and number exceeding 99 percentile
stCounts = [stCounts2(2), stCounts3(2), stCounts4(2)];
maxCount = max(stCounts);
pv = [pv2 pv3 pv4];
[minpv,minpvInd] = min(pv);
cv = [cv2 cv3 cv4];
minpvCt = cv(minpvInd);
cpfa = ppval(pp_Pfa_Vs_Score,maxCount);
cpfam0 = max(0,cpfa);
cpd = ppval(pp_Pd_Vs_Score,maxCount);
%%
% Add following lines 08062012 David Stein
if cpfa < 0
    cpd = ppval(pp_Pd_Vs_Pfa,0);
    cpfa = 0;
end
%%
%%
% current Call prior to 8/6/12
% candidateHE = extractClusters_cf(cf,layerScoreThresh);
% change to fix shift David Stein
axialBuffer = 1;
fastBuffer = 1;
sliceBuffer = 0;
candidateHE = extractClusters_cf(cf,layerScoreThresh,intLayerBdrys_smooth2d,axialBuffer,fastBuffer,sliceBuffer,sizeCFAR);
%%
HEscore = maxCount;

