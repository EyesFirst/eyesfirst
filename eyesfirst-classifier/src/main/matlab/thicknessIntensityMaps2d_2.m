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

function [thickMap1,thickMap2,intenseMap1,intenseMap2,intenseMap3,reflectivityMap1,reflectivityMap2] = thicknessIntensityMaps2d_2(layerFile,useCosCorrect,smoothFlag)
axialPixRes = 1.9531; % microns

di = 46.875; % microns per pixel slow time 
dj = 11.7188; % microns per pixel fast time
dk = 1.9531; % microns per pixel axial

load(layerFile)
Nslices = length(stLayerBdrys);
if smoothFlag == 0
    Ncols = length(stLayerBdrys{1}.thicknessProf12);
else
    Ncols = length(stLayerBdrys{1}.smoothThickness2dProf12);
end
thickMap1 = zeros(Nslices,Ncols);
thickMap2 = zeros(Nslices,Ncols);
intenseMap1 = zeros(Nslices,Ncols);
intenseMap2 = zeros(Nslices,Ncols);
intenseMap3 = zeros(Nslices,Ncols);
% identify cosine correction terms
cosCorrect = ones(Nslices,1);


for ii = 1:Nslices
    if useCosCorrect == 1
        [aa,bb] = size(stLayerBdrys{ii}.BdryRelIm);
        rl1 = polyfit([1:aa]',stLayerBdrys{ii}.BdryRelIm(:,2),1);
        curCorrect = cos(atan(rl1(1)*(dk/dj)));
    else
        curCorrect = 1;
    end;
    cosCorrect(ii) = curCorrect;
    if smoothFlag == 0
        thickMap1(ii,:) = stLayerBdrys{ii}.thicknessProf12*axialPixRes*curCorrect;
        thickMap2(ii,:) = stLayerBdrys{ii}.thicknessProf13*axialPixRes*curCorrect;
    else
        thickMap1(ii,:) = stLayerBdrys{ii}.smoothThickness2dProf12*axialPixRes*curCorrect;
        thickMap2(ii,:) = stLayerBdrys{ii}.smoothThickness2dProf13*axialPixRes*curCorrect;
    end;
    intenseMap1(ii,:) = stLayerBdrys{ii}.intensityProf12;
    intenseMap2(ii,:) = stLayerBdrys{ii}.intensityProf13;
    if isfield(stLayerBdrys{ii},'intensityProf1B')
       intenseMap3(ii,:) = stLayerBdrys{ii}.intensityProf1B;
    end;

end
reflectivityMap1 = intenseMap1./thickMap1;
reflectivityMap2 = intenseMap2./thickMap2;
% figure;
% imagesc(thickMap1);colorbar;title(['Thickness between layers 1 and 2 (microns)',' ',patientid])
% figure;
% imagesc(thickMap2);colorbar;title(['Thickness between layers 1 and 3 (microns)',' ',patientid])
% figure;
% imagesc(intenseMap1);colorbar;title(['Intensity between layers 1 and 2',' ',patientid])
% figure;
% imagesc(intenseMap2);colorbar;title(['Intensity between layers 1 and 3',' ',patientid])
% figure;
% imagesc(reflectivityMap1);colorbar;title(['Reflectivity between layers 1 and 2',' ',patientid])
% figure;
% imagesc(reflectivityMap2);colorbar;title(['Reflectivity between layers 1 and 3',' ',patientid])