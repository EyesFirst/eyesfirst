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

function runEyesFirstCC(curTask,dicomDirs,subsidiaryDirs,fileArrayFileIn,fileArrayFileOut,internalLayerPar,initialLayerPar,gatingPar,smoothingPar,intLayerSmoothingPar,layerSmoothingPar,thicknessPar,statPar,cfarPar,cfarClusterPar,outFileDirs,indexSet)
% curTask is what should be done.  For a list of choices see taskList
% below.  fileArrayFile is the file containing an initial fileArray
%dicomDirs = {}; % a list of directories containing dicom files to be processed
%subsidiaryDirs = []; % directories in which to look for files already created in setting up fileArray
taskList = {'dicom2mat','motionCorrect','initialGating','initialLayerBdrys','initialLayerBoundarySmoothing','generateThicknessMaps','generateStatistics','identifyInternalLayers','smoothInternalLayers','CFARProcess','GenerateExceedanceClusters','CalculateMomentsExceedClust'};
NpossibleTasks = length(taskList);
analysisTasks = zeros(NpossibleTasks,1);
% identify Current Task index
curTaskInd = 0;
for ii = 1:NpossibleTasks
   if strcmp(curTask,taskList{ii})
       curTaskInd = ii;
   end
end;
if curTaskInd == 0
    fprintf('nothing to do\n')
else 
    analysisTasks(1:curTaskInd) = ones(curTaskInd,1);
end;

if isempty(fileArrayFileIn)
    fileArray = [];
    fileArray = createFileArray(dicomDirs,subsidiaryDirs,fileArray);
else
    error('File array file in is not empty, auto-loading file information from a MAT file is currently disabled.');
    %load(fileArrayFile);
end
% indexSet = [];
if isempty(indexSet)  % do all of the files
    NdcmFiles = length(fileArray);
    indexSet = 1:NdcmFiles;
end;

for index=1:length(indexSet)
    currentIndex = indexSet(index);
    try
        fprintf('=== STARTING TO PROCESS %s ===\n', fileArray{currentIndex}.base.name);
        fileArray = eyesFirstCC(fileArray,currentIndex,outFileDirs,analysisTasks,smoothingPar,gatingPar,initialLayerPar,layerSmoothingPar,thicknessPar,statPar,internalLayerPar,intLayerSmoothingPar,cfarPar,cfarClusterPar);
        % Don't bother saving the file array any more; as far as I can tell,
        % it's never loaded anyway
        % save(fileArrayFileOut,'fileArray')
    catch err
        fprintf('An error occurred while processing file "%s":\n%s\n', fileArray{currentIndex}.base.name, getReport(err, 'extended'));
    end
end

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

