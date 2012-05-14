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

function call_runEyesFirstCC(indexSet)

% FIXME: We really should be passing in individual file names, and not an index
% set.
if ischar(indexSet)
    indexSet = eval(indexSet);
end

% Disable some warnings that make it hard to see what's happening

warning('off', 'SPLINES:CHCKXYWP:NaNs');
warning('off', 'MATLAB:MKDIR:DirectoryExists');

% Data directory to store files:
% YOU WILL NEED TO CHANGE THIS TO MATCH YOUR MACHINE
% (FIXME: This really ought to be a parameter)
eyes_first_data_dir = '/Users/dpotter/EyesFirst/data';%/opt/eyesfirst/data';

% Task to do
%curTask = 'generateStatistics';
%curTask = 'identifyInternalLayers';
% curTask = 'smoothInternalLayers';
curTask = 'GenerateExceedanceClusters';
% For testing only partial slices, use the following:
%curTask = 'initialLayerBdrys';

% taskList = {'dicom2mat','motionCorrect','initialGating','initialLayerBdrys','initialLayerBoundarySmoothing','generateThicknessMaps','generateStatistics','identifyInternalLayers','smoothInternalLayers','CFARProcess','GenerateExceedanceClusters','CalculateMomentsExceedClust'};
 dicomDirs = { eyes_first_data_dir };

% Generate the remaining directories
subsidiaryDirs = { eyes_first_data_dir,...
    strcat(eyes_first_data_dir, filesep, 'storeMats'),...
    strcat(eyes_first_data_dir, filesep, 'storeMotionCorrected'),...
    strcat(eyes_first_data_dir, filesep, 'storeGates'),...
    strcat(eyes_first_data_dir, filesep, 'storeLayerBdrys'),...
    strcat(eyes_first_data_dir, filesep, 'storeStats'),...
    strcat(eyes_first_data_dir, filesep, 'storeCfar'),...
    strcat(eyes_first_data_dir, filesep, 'storeOutput') };
outFileDirs.mat = 'storeMats';
outFileDirs.motionCorrect = 'storeMotionCorrected';
outFileDirs.gate = 'storeGates';
outFileDirs.layers = 'storeLayerBdrys';
outFileDirs.stats = 'storeStats';
outFileDirs.cfar = 'storeCfar';
outFileDirs.output = 'storeOutput';
smoothingPar.xdeg = 2;
smoothingPar.zdeg = 2;
gatingPar.edf = 5;
gatingPar.ndil = 4;
gatingPar.sliceRange = []; % if empty will do all
initialLayerPar.ksf = 3;
initialLayerPar.dfstd = [6 3];
initialLayerPar.Nlayers = 3;
initialLayerPar.maxBdrySlope = [5 1 1];
initialLayerPar.maxInterLayerDist = [400 75];
initialLayerPar.minInterLayerDist = [50 10];
initialLayerPar.sliceRange = []; % if set to [], will do all slices
initialLayerPar.layerFig = figure;
initialLayerPar.costFig = figure;
internalLayerPar = [];
layerSmoothingPar.plotFlag = 1;
layerSmoothingPar.imfig = figure;
layerSmoothingPar.bdryfig = figure;
layerSmoothingPar.smoothxy = 0;
layerSmoothingPar.smoothFac = 0.05;
layerSmoothingPar.NsmoothIter = 2;
thicknessPar.useCosCorrect = 1; % if value is 1, then corrects for the slope of the bottom layer
thicknessPar.smoothFlag = 1; % if value is 1, then uses the smoothed layers to calculate thickness
statPar.kurtosisFlag = 1;
statPar.skewnessFlag = 1;
internalLayerPar.ksf = 100;
internalLayerPar.stdv2d = [6 3]; 
internalLayerPar.dfmf = 4;
internalLayerPar.Nlayers = 3;
internalLayerPar.sliceFig = figure;

intLayerSmoothingPar.plotFlag = 1;
intLayerSmoothingPar.imfig = figure;
intLayerSmoothingPar.bdryfig = figure;
intLayerSmoothingPar.smoothxy = 0;
intLayerSmoothingPar.smoothFac = 0.05;
intLayerSmoothingPar.NsmoothIter = 2;
intLayerSmoothingPar.NintLayers = 3;
cfarPar.gapj = 7;
cfarPar.gapk = 3;
cfarPar.TWi = 3;
cfarPar.TWj = 3;
cfarPar.TWk = 3;
cfarClusterPar.initThresh = 4;  % cfar threshold value required to initiate a cluster
cfarClusterPar.growThresh = 3; % cfar threshold value required to grow a cluster
cfarClusterPar.d1 = 1; % defines neighborhood for growing a cluster as cp +/- d1 in dimension 1 and similarly in the other two dimensions.  
                       % Thus, with d1 = d2 = d3 = 1, the neighborhood of a
                       % point in the cube is the 27 point neighborhood.
                       % This corresponds to a connected cluster. Setting
                       % d1i > 1 allows for gaps of size di-1 in dimension
                       % i.
cfarClusterPar.d2 = 1;
cfarClusterPar.d3 = 1;


%indexSet = []; % indexSet is used to control which of the dicom files in the fileArray are processed.  If set to []; then all are.  An alternative
%way to run the program is to loop over the files and complete all steps of the processing 
%on each one before going on to the next.  This can be achieved by looping over indexSets 

for ii = 1:length(subsidiaryDirs)
    mkdir(subsidiaryDirs{ii});
end;
fileArrayFileIn = [];
fileArrayFileOut = 'fileArray1';
runEyesFirstCC(curTask,dicomDirs,subsidiaryDirs,fileArrayFileIn,fileArrayFileOut,internalLayerPar,initialLayerPar,gatingPar,smoothingPar,intLayerSmoothingPar,layerSmoothingPar,thicknessPar,statPar,cfarPar,cfarClusterPar,outFileDirs,indexSet)

end

% taskList = {'dicom2mat','motionCorrect','initialGating','initialLayerBdrys','initialLayerBoundarySmoothing','generateThicknessMaps','generateStatistics','identifyInternalLayers','smoothInternalLayers','CFARProcess','GenerateExceedanceClusters','CalculateMomentsExceedClust'};
