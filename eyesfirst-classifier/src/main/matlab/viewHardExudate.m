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

function viewHardExudate(cfarFile,clusterFeatureFile)
% a function to view the candidate hard exudate bounding boxes superimposed
% on the cfar image
% dimension order of candidateHE boxes is (fastTime,slowTime,axial)
% dimension order of cfarSAA is (axial,fastTime,slowTime)
load(cfarFile);
load(clusterFeatureFile);
nHE = numel(candidateHE);
sliceBuffer = 0;
[aa,bb,cc] = size(cfarSAA);
figure;
cfarRange = [-5,8];
maxVal = max(cfarRange);
for ii = 1:nHE
    minVox = candidateHE(ii).boundingBoxMinCorner;
    boxWidth = candidateHE(ii).boundingBoxWidth;
    minAxial = minVox(3);
    minFast = minVox(1);
    minSlow = minVox(2);
    delAxial = boxWidth(3);
    delFast = boxWidth(1);
    delSlow = boxWidth(2);
    sliceRange = max([minSlow-sliceBuffer,1]):min([minSlow+delSlow-1+sliceBuffer,cc]);
    Nslices = length(sliceRange);
    for jj = 1:Nslices;
        curSlice = squeeze(cfarSAA(:,:,sliceRange(jj)));
        % add top edge to curSlice
        I1 = minAxial*ones(1,delFast);
        J1 = minFast:(minFast+delFast-1);
        I2 = minAxial:(minAxial+delAxial-1);
        J2 = (minFast+delFast-1)*ones(1,delAxial);
        I3 = (minAxial+delAxial-1)*ones(1,delFast);
        J3 = J1;
        I4 = I2;
        J4 = minFast*ones(1,delAxial);
        curSlice = imposeSubImage(curSlice,I1,J1,maxVal);
        curSlice = imposeSubImage(curSlice,I2,J2,maxVal);
        curSlice = imposeSubImage(curSlice,I3,J3,maxVal);
        curSlice = imposeSubImage(curSlice,I4,J4,maxVal);
        cmap= colormap;
        imagesc(curSlice,cfarRange);colorbar
        title(['cluster ',int2str(ii),'; slice ',int2str(jj)]);
    end;
end;

end

