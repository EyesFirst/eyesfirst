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

function fileArray = createFileArray(dicomDirs,subsidDirs,fileArray)
% dirs is a cell array of directories
% on return fileArray is an updated cell array of file names
% with the following fields
% find the base files in the indicated directories 
% base files end in .dcm
% note that multiples is set to one if there is more than one file of the
% given name

% first identify the .dcm files these are required to create an entry in the fileArray structure
NdcmDir = length(dicomDirs);
curDir = pwd;
%fileTypesAll = {'.dcm','_mat.mat','_SAA.mat','_gate.mat','_Layers1.mat','_Layers1_smooth2d.mat', '_thicknessMaps2d.mat','_stats.mat','_cfar.mat','_cfar_exceedClust.mat','_cfar_exceedClust_moments.mat','_intLayers.mat','_intLayers_smooth2d.mat'};
fileTypesDCM = {'.dcm'};
for ii = 1:NdcmDir  % note that if dcmDir is empty then does not search for additional bases 
    fileDir = dicomDirs{ii}; % complete path name
    cd(fileDir)
    cfiles = dir;
    Nfiles = length(cfiles);
    for jj = 1:Nfiles
        % search for .dcm files
        [filePrefix,fileSuffix] = decomposeFileName(cfiles(jj).name,fileTypesDCM);
        if ~isempty(fileSuffix)
             fileArray = appendFile(fileArray,filePrefix,fileSuffix,fileDir);
        end;
    end;
    cd(curDir);
end;
    
Ndirs = length(subsidDirs);
ctBases = length(fileArray);
curDir = pwd;
for kk = 1:Ndirs
    fileDir = subsidDirs{kk}; % complete path names
    cd(fileDir)
    cfiles = dir;
    Nfiles = length(cfiles);
    for jj = 1:Nfiles
        [filePrefix,fileSuffix] = decomposeFileName(cfiles(jj).name,[]);
        if ~isempty(filePrefix) & ~strcmp(fileSuffix,'.dcm')
           fileArray = appendFile(fileArray,filePrefix,fileSuffix,fileDir);
        end;
    end;
    cd(curDir);
end;
    
    
%     
%     
%     
% fileArray{ii}.base.dir    % dicom file
% fileArray{ii}.base.name
% fileArray{ii}.base.multiples
% fileArray{ii}.mat.dir  % matlab file 
% fileArray{ii}.mat.name
% fileArray{ii}.mat.multiples
% fileArray{ii}.a.dir % SAA file
% fileArray{ii}.a.name
% fileArray{ii}.a.multiples
% fileArray{ii}.gate.dir % gate file
% fileArray{ii}.gate.name
% fileArray{ii}.gate.multiples
% fileArray{ii}.layers1.dir % initial layer file
% fileArray{ii}.layers1.name
% fileArray{ii}.layers1.multiples
% fileArray{ii}.layers1_smooth2d.dir % smoothed initial layer file
% fileArray{ii}.layers1_smooth2d.name
% fileArray{ii}.layers1_smooth2d.multiples
% fileArray{ii}.intLayers.name   % internal layer file
% fileArray{ii}.intLayers.dir
% fileArray{ii}.intLayers.multiples
% fileArray{ii}.intLayers_smooth2d.name  % smoothed internal layer file
% fileArray{ii}.intLayers_smooth2d.dir
% fileArray{ii}.intLayers_smooth2d.multiples
% fileArray{ii}.intLayers_smooth2d_TO.name  % smoothed internal layer file
% fileArray{ii}.intLayers_smooth2d_TO.dir
% fileArray{ii}.intLayers_smooth2d_TO.multiples
% fileArray{ii}.cfar.name       % cfar file
% fileArray{ii}.cfar.dir
% fileArray{ii}.cfar.multiples
% fileArray{ii}.cfarCluster.name   % cfar cluster file
% fileArray{ii}.cfarCluster.dir
% fileArray{ii}.cfarCluster.multiples
% fileArray{ii}.cfarClusterMoments.name   % moments of cfar clusters
% fileArray{ii}.cfarClusterMoments.dir
% fileArray{ii}.cfarClusterMoments.multiples
% fileArray{ii}.thicknessStats.name      % thickness statistics
% fileArray{ii}.thicknessStats.dir
% fileArray{ii}.thicknessStats.multiples
% fileArray{ii}.thicknessMaps.name      % thicknessMaps
% fileArray{ii}.thicknessMaps.dir
% fileArray{ii}.thicknessMaps.multiples 
% 
