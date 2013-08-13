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

function [ jointAnomStat, performancePoint ] = computeDMEscore(curThicknessStatFile,curThicknessStatDir,statFile,splineFile)
% jointAnomStat = computeDMEScore(curThicknessStatFile,curThicknessStatDir,statFile)
% computes the nine field joint anomaly statistic based on first four
% statistical moents per field.  Statistics from normal patients are
% contained in [statDir,statFile]; thicknessMap and thickness statistic
% files are contained in curThicknessStatDir

load(splineFile);
load(statFile);
Nfields = 9;
NstatsPerField = 4;
minThick = 25;
statMat = zeros(Nfields,NstatsPerField);
curCompThicknessStatFile = [curThicknessStatDir,filesep,curThicknessStatFile];
statpos = strfind(curCompThicknessStatFile,'Stats');
curCompThicknessMapFile = [curCompThicknessStatFile(1:statpos(end)-1),'Maps.mat'];
load(curCompThicknessStatFile)
tmf = load(curCompThicknessMapFile);
for jj = 1:Nfields
    curThick = thicknessFeatures.stats{jj}.thickAve;
    curI = thicknessFeatures.stats{jj}.I;
    curJ = thicknessFeatures.stats{jj}.J;
    LI = length(curI);
    fieldIntensity = zeros(LI,1);
    for kk = 1:LI
        fieldIntensity(kk) = tmf.intenseMap1(curI(kk),curJ(kk));
    end;
    if length(fieldIntensity) == length(curThick)
        Iok = find(curThick >= minThick & fieldIntensity > 0);
    else
        Iok = find(curThick >= minThick);
    end;
    statMat(jj,1) = mean(curThick(Iok));    % thicknessFeatures.stats{jj}.meanThickAve;
    statMat(jj,2) = std(curThick(Iok));     % thicknessFeatures.stats{jj}.stdThickAve;
    statMat(jj,3) = skewness(curThick(Iok));   % thicknessFeatures.stats{jj}.skewnessAve;
    statMat(jj,4) = kurtosis(curThick(Iok));   % thicknessFeatures.stats{jj}.kurtosisAve;
end
jointStatVec = statMat(:)'; % CHECK THAT THIS IS DIMENSIONED PROPERLY
cjsm = (jointStatVec - jointStats9.mean)';
jointAnomStat = sum(cjsm.*(jointStats9.covm\cjsm));

% convert to equivalent performance points (PFA,PD)
% (Specificity,Sensitivity) based on test data from 2011.

cpfa = ppval(pp_Pfa_Vs_Score,jointAnomStat);
if cpfa >= 0
   cpd = ppval(pp_Pd_Vs_Score,jointAnomStat);
else
    cpfa = 0;
    cpd = ppval(pp_Pd_Vs_Pfa,0);
end
performancePoint.pfa = cpfa;
performancePoint.pd = cpd;
performancePoint.specificity = 1-cpfa;
performancePoint.sensitivity = cpd;
performancePoint.jointAnomStat = jointAnomStat;

end