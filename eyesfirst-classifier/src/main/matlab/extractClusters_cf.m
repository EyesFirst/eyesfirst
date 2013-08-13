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

function candidateHE = extractClusters_cf(cf,layerScoreThresh,layerBdrys,axialBuffer,fastBuffer,sliceBuffer,sizeCFAR)

Nclust = length(cf);
scoresLayer = zeros(Nclust,2);
for ii = 1:Nclust
    scoresLayer(ii,:) = [cf{ii}.normalScore1 cf{ii}.clusterLayer.cfar];
end;
candidateHE = struct('maxCfarValue',{},'normalScore',{},'layer',{},'center',{},'radius',{},'numVoxels',{},'boundingBoxMinCorner',{},'boundingBoxWidth',{});
coordinateOffset = [1 1 1]';
Nlayers = size(layerScoreThresh,1);
ctHE = 0;
for jj = 1:Nlayers
    curLayer = layerScoreThresh(jj,1);
    curThresh = layerScoreThresh(jj,2);
    I1 = find(scoresLayer(:,2) == curLayer & scoresLayer(:,1) >= curThresh);
    if ~isempty(I1)
        for ii = 1:length(I1)
            % sort in descending order
            curScores = scoresLayer(I1,1);
            [sortScores,I1sort] = sort(curScores,'descend');
            ctHE = ctHE+1;
            candidateHE(ctHE).maxCfarValue = cf{I1(I1sort(ii))}.normprctile(end);
            candidateHE(ctHE).normalScore = cf{I1(I1sort(ii))}.normalScore1;
            candidateHE(ctHE).layer = cf{I1(I1sort(ii))}.clusterLayer.cfar;
            candidateHE(ctHE).layerProportion = cf{I1(I1sort(ii))}.clusterLayer.cfarProportionatePos;
            m2p = inv(diag(cf{I1(I1sort(ii))}.dmu));
            cemi = cf{I1(I1sort(ii))}.ellipse.center; % center of ellipse in microns
            cepi = round(m2p*cemi+coordinateOffset); % center of ellipse in pixels
            candidateHE(ctHE).ellipseCenter = cepi;   % in pixels
            vm = cf{I1(I1sort(ii))}.delaunay.X; % vertices of the triangulation;
            minv = min(vm)';
            maxv = max(vm)';
            % coordinates of bounding box of triangulation in pixels
            minvPix = floor(m2p*minv+coordinateOffset);
            maxvPix = ceil(m2p*maxv+coordinateOffset);
            BBwidth = maxvPix-minvPix;
            candidateHE(ctHE).boundingBoxMinCorner = minvPix;
            candidateHE(ctHE).boundingBoxWidth = BBwidth;
            candidateHE(ctHE).numVoxels = cf{I1(I1sort(ii))}.Npix;
            % changes added 08_06_12 to include a field with shifted
            % indices David Stein
            %%
             minVox = candidateHE(ctHE).boundingBoxMinCorner;
            boxWidth = candidateHE(ctHE).boundingBoxWidth;
            minAxial = minVox(3)-axialBuffer;
            minFast = minVox(1)-fastBuffer;
            minSlow = minVox(2);
            delAxial = boxWidth(3)+axialBuffer;
            delFast = boxWidth(1)+fastBuffer;
            delSlow = boxWidth(2);
            sliceRange = max([minSlow-sliceBuffer,1]):min([minSlow+delSlow-1+sliceBuffer,sizeCFAR(3)]);
            curShift = layerBdrys{sliceRange(1)}.curxaug(1)-1;
            candidateHE(ctHE).boundingBoxMinCornerShift = candidateHE(ctHE).boundingBoxMinCorner;
            candidateHE(ctHE).boundingBoxMinCornerShift(1) = minFast+curShift;
            candidateHE(ctHE).boundingBoxMinCornerShift(2) = minSlow;
            candidateHE(ctHE).boundingBoxMinCornerShift(3) = minAxial;
            % end of changes
    %%
        end;
    end;
end;
