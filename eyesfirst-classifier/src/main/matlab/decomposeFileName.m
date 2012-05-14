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

function [filePrefix,fileSuffix] = decomposeFileName(fileName,fileTypes)
fileTypes = {'.dcm','_mat.mat','_SAA.mat','_gate.mat','_Layers1.mat','_Layers1_smooth2d.mat', '_thicknessMaps.mat','_thicknessStats.mat','_cfar.mat','_cfar_exceedClust.mat','_cfar_exceedClust_moments.mat','_intLayers.mat','_intLayers_smooth2d.mat','_intLayers_smooth2d_TO.mat'};

Ntypes = length(fileTypes);
fileTypeInd = zeros(Ntypes,1);
idType = 0;
typeInd = 0;
for ii = 1:Ntypes
    if idType == 0
       typePos = strfind(fileName,fileTypes{ii});
       if ~isempty(typePos)
          fileTypeInd(ii) = typePos;
          idType = 1;
          typeInd = ii;
       end;
    end;
end;
if idType == 0
    filePrefix = [];
    fileSuffix = [];
else
    filePrefix = fileName(1:typePos-1);
    if typeInd == 1
        fileSuffix = '.dcm';
    else
        fileSuffixWmat = fileTypes{typeInd};
        matPos = strfind(fileSuffixWmat,'.mat');
        fileSuffix = fileSuffixWmat(1:matPos-1);
    end;
end;
   

