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

function cf = clusterFeatures(cfarSAA,exceedClusters,layerBoundaries,stats)
% calculates features of the clusters
% geometric characterization
%    convexhull of cluster in pixel space
%    volume of cluster 
%    number of voxels
%    absoluteMaxCoordinates
%    cfarMaxCoordinates
%    clusterLayer.cfar (layer of cfar maximum)
%    clusterLayer.absolute (layer of absolute maximum)
% Note that there are six possible layer values: 0-above the ILM should not
% occur; 1-between the ILM and the first interior layer boundary; 2-
% between the first and second interior boundaries; 3- between the second
% and third interior boundaries; 4- between the third interior boundary and
% the RPE (note to self: incorporate anatomically significant names to
% layer boundaries) 
% normalized intensity percentiles: 25, 50, 75 90, 100
% intensity percentiles: 25, 50, 75, 90, 100 
% minimal volume enclosing ellipse
%    center
%    quadratic form
%    eigenvalues
%    volume
% load(statFile);
% for ii = 1:4
%     stats2{ii}.invCovMat = inv(stats2{ii}.covMat);
% end;
cp = [1 1 1]; % origin of pixel coordinate system
dy = 46.875; % microns per pixel slow time 
dx = 11.7188; % microns per pixel fast time
dz = 1.9531; % microns per pixel axial
dmu = [dx dy dz];
psf = dz/2; % point spread function is assumed to be a cube of side length 2*psf
p2d = diag(dmu); % pixel 2 distance conversion matrix
Nclust = length(exceedClusters);
cf = cell(Nclust,1);
locErrTol = 1;
for ii = 1:Nclust
  % colOne = layerBoundaries{ii}.curxaug(1);
   pm = [exceedClusters{ii}.clust(:,3:4) exceedClusters{ii}.clust(:,2)]; % [fast time, slow time, axial] pixel index relCFAR image
   Npix = size(pm,1);
   pmsc = (pm - repmat(cp,Npix,1))*p2d; % spatial coordinates
   pmscev = psfexpand(pmsc,psf); % unique pixels in spatial coordinates expanded by point spread function
   % can now do the triangulation in a uniform way
   dt = DelaunayTri(pmscev);
   [ch,dtvol] = convexHull(dt);
   chu = unique(ch);
   cf{ii}.delaunay = dt;
   cf{ii}.volume = dtvol;
   cf{ii}.convexHull = ch;
   chmat = dt.X(chu,:)';
   [mvqf, mvc] = MinVolEllipse(chmat,.01);
   cf{ii}.ellipse.center = mvc;
   cf{ii}.ellipse.qf = mvqf;
   [U,S,V] = svd(mvqf);
   D = sqrt(diag(S).^(-1));
   cf{ii}.ellipse.volume = pi*prod(D)*(4/3);
   cf{ii}.ellipse.eigenvalues = D;
   cf{ii}.ellipse.eigenvectors = U;
   [FF XF] = freeBoundary(dt);
   cf{ii}.freeBoundary.FF = FF;
   cf{ii}.freeBoundary.XF = XF;
   minPix = min(pm);
   maxPix = max(pm);
   pixThick = maxPix-minPix+ones(1,3);
   I1 = find(pixThick == 1);
   if isempty(I1)
       clustdim = 3;
   elseif length(I1) == 1
       clustdim = 2;
   elseif length(I1) == 2
       clustdim = 1;
   elseif length(I1) == 3
       clustdim = 0;
   end;
   cf{ii}.dim = clustdim;
   cf{ii}.Npix = Npix;
   cf{ii}.pixThick = pixThick;
   cf{ii}.dmu = dmu;
   if clustdim == 0  % cluster is a single point and no spatial parameters

       layerBdryCoord =  [layerBoundaries{pm(2)}.top(pm(1)) layerBoundaries{pm(2)}.smooth2dInterMediateBdrys(pm(1),:) ... 
                                        layerBoundaries{pm(2)}.floor(pm(1))];
                                    
       Ig0 = find(layerBdryCoord - pm(3) > 0);
       if isempty(Ig0)
           if pm(3) - max(layerBdryCoord) <= locErrTol
               clusterLayer = 4;
           else
              error('cluster point is at or beyond the floor')
           end;
       else
           if length(Ig0) == 5
               error('cluster point is above the top')
           else
               clusterLayer = 5-length(Ig0);
           end;
       end;
       cf{ii}.clusterLayer.cfar = clusterLayer;
       cf{ii}.clusterLayer.absolute = clusterLayer;
       cf{ii}.normprctile = cfarSAA(pm(3),pm(1),pm(2));
       curShift = layerBoundaries{pm(2)}.curxaug(1)-1;
       cf{ii}.prctile = []; %SAA(pm(3),pm(1)+curShift,pm(2));   % need to convert back to SAA coordinates 
       cf{ii}.ellipse = [];
   else
       clustCFARValues = zeros(Npix,1);
       clustValues = zeros(Npix,1);
       for rr = 1:Npix % extract the saa and cfarsaa values
           clustCFARValues(rr) = cfarSAA(pm(rr,3),pm(rr,1),pm(rr,2));
           curShift = layerBoundaries{pm(rr,2)}.curxaug(1)-1;
           % clustValues(rr) = SAA(pm(rr,3),pm(rr,1)+curShift,pm(rr,2));
       end;
       cf{ii}.normprctile = prctile(clustCFARValues,[25 50 75 90 100]);
       cf{ii}.prctile = prctile(clustValues,[25 50 75 90 100]);
       % define the cfar layer as the layer containing the maximum of the
       % cfar normalized cluster values
       [mnv,mni] = max(clustCFARValues);
        layerBdryCoord =  [layerBoundaries{pm(mni,2)}.top(pm(mni,1)) layerBoundaries{pm(mni,2)}.smooth2dInterMediateBdrys(pm(mni,1),:) ... 
                                        layerBoundaries{pm(mni,2)}.floor(pm(mni,1))];
       Ig0 = find(layerBdryCoord - pm(mni,3) > 0);
       if isempty(Ig0)
           if pm(mni,3) - max(layerBdryCoord) <= locErrTol
               clusterLayer = 4;
           else
              error('cluster point is at or beyond the floor')
           end;
       else
           if length(Ig0) == 5
               error('cluster point is above the top')
           else
               clusterLayer = 5-length(Ig0);
           end;
       end;
       cf{ii}.clusterLayer.cfar = clusterLayer;
       localRoof = layerBdryCoord(clusterLayer);
       localFloor = layerBdryCoord(clusterLayer+1);
       proportionatePosition = (pm(mni,3)-localRoof)/(localFloor-localRoof);
       cf{ii}.clusterLayer.cfarProportionatePos = proportionatePosition;
       % define the absolute layer as the layer containing the maximum of the
       % cluster values
%        [mnv,mni] = max(clustValues);
%         layerBdryCoord =  [layerBoundaries{pm(mni,2)}.top(pm(mni,1)) layerBoundaries{pm(mni,2)}.smooth2dInterMediateBdrys(pm(mni,1),:) ... 
%                                         layerBoundaries{pm(mni,2)}.floor(pm(mni,1))];                             
%        Ig0 = find(layerBdryCoord - pm(mni,3) > 0);
%        if isempty(Ig0)
%            if pm(mni,3) - max(layerBdryCoord) <= locErrTol
%                clusterLayer = 4;
%            else
%               error('cluster point is at or beyond the floor')
%            end;
%        else
%            if length(Ig0) == 5
%                error('cluster point is above the top')
%            else
%                clusterLayer = 5-length(Ig0);
%            end;
%        end;
%        localRoof = layerBdryCoord(clusterLayer);
%        localFloor = layerBdryCoord(clusterLayer+1);
%        proportionatePosition = (pm(mni,3)-localRoof)/(localFloor-localRoof);
%        cf{ii}.clusterLayer.absolute = clusterLayer;
%        cf{ii}.clusterLayer.absoluteProportionatePos = proportionatePosition;
       if ~isempty(stats)
           normalMean = stats{clusterLayer}.mu;
           invNormalCov = stats{clusterLayer}.invCovMat;
           if clusterLayer == 2
              featureVec = [cf{ii}.normprctile(end) cf{ii}.Npix cf{ii}.ellipse.eigenvalues(2)/cf{ii}.ellipse.eigenvalues(3) cf{ii}.ellipse.eigenvalues(1)/cf{ii}.ellipse.eigenvalues(2) cf{ii}.clusterLayer.cfarProportionatePos];
           else
               featureVec = [cf{ii}.normprctile(end) cf{ii}.Npix cf{ii}.ellipse.eigenvalues(2)/cf{ii}.ellipse.eigenvalues(3) cf{ii}.ellipse.eigenvalues(1)/cf{ii}.ellipse.eigenvalues(2)];
           end
           cfv = featureVec-normalMean;
           cf{ii}.normalScore1 = cfv*invNormalCov*cfv';
       else
           cf{ii}.normalScore = [];
       end;
   end
end


end

