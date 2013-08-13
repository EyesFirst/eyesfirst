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

function [ problemFiles ] = exec_clusterFeatures( clusterFile, cfarFile, boundaryFile, outFile )
%run_clusterFeatures Run the cluster features on the given files.
%   Detailed explanation goes here

statsFile = 'trainingStats_ppl_2.mat';
splineFile = 'HESplineFile.mat';
normPrctFile = 'normalTrainingPercentilesstatInd_1.mat';

problemFiles = [];

load(statsFile);
load(splineFile);

pdvpfa = ppval([0:.01:1],pp_Pd_Vs_Pfa);
figh = figure;
plot([0:.01:1],pdvpfa,'g');
ylabel('probability of detection')
xlabel('probability of false alarm')
hold on;

load(normPrctFile);
pvec = .01*[0 25 50 75 80 90 95 99 99.5 99.9];
pvec3 = pvec([1,  8]);
plsym = {'rx','k*'};

% threshhold is the 99th percentile
layerScoreThresh = [[2 3 4]' normPrctiles([2 3 4],8)];

load(clusterFile)
load(cfarFile);
fprintf('Loading boundary file "%s".\n', boundaryFile);
load(boundaryFile);

% uses proportionate position in layer 2 as a feature
cf = clusterFeatures(cfarSAA,exceedClusters,intLayerBdrys_smooth2d,stats1);

% We resave this file later, so ... let's not save it now
%save(outFile, 'cf');
nsv = extractNormalScoreFeature_gf(cf,normPrctiles);
[pv2,cv2] = genPvalues(nsv.numberExceedNormPrctiles(2,[1,9]),pvec3); % note that first column is the count 9th column is the 99th percentiloe
[pv3,cv3] = genPvalues(nsv.numberExceedNormPrctiles(3,[1, 9]),pvec3);
[pv4,cv4] = genPvalues(nsv.numberExceedNormPrctiles(4,[1, 9]),pvec3);
stCounts2 = nsv.numberExceedNormPrctiles(2,[1,9])';% number of clusters in layer 2 and number exceeding 99.5 percentile
stCounts3 = nsv.numberExceedNormPrctiles(3,[1,9])';% number of clusters in layer 3 and number exceeding 99.5 percentile
stCounts4 = nsv.numberExceedNormPrctiles(4,[1,9])';% number of clusters in layer 4 and number exceeding 99.5 percentile
stCounts = [stCounts2(2), stCounts3(2), stCounts4(2)];
maxCount = max(stCounts);
pv = [pv2 pv3 pv4];
[minpv,minpvInd] = min(pv);
cv = [cv2 cv3 cv4];
minpvCt = cv(minpvInd);
cpfa = ppval(pp_Pfa_Vs_Score,maxCount);
cpfam0 = max(0,cpfa);
cpd = ppval(pp_Pd_Vs_Score,maxCount);
%plot(cpfam0,cpd,plsym{ii},'markerSize',14)
% now extract the above threshold clusters
candidateHE = extractClusters_cf(cf,layerScoreThresh);
save(outFile,'cf','nsv','pv','cv','stCounts','cpd','cpfa','candidateHE');

end

