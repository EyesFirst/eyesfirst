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

function fileArray = eyesFirstCC(fileArray,indexSet,outFileDirs,analysisTasks,smoothingPar,gatingPar,initialLayerPar,layerSmoothingPar,thicknessPar,statPar,internalLayerPar,intLayerSmoothingPar,cfarPar,cfarClusterPar)

% assume that the initial fileArray has been created outside of this
% program so that indexSet can refer to a list of files known at runtime

fprintf('STATUS:{"message":"Reading DICOM files..."}\n');
if analysisTasks(1) == 1  % read dicom files
    fileArray = readDicomListNormal(fileArray,indexSet,outFileDirs.mat);
end;
fprintf('STATUS:{"message":"Running motion correction..."}\n');
if analysisTasks(2) == 1 % do the motion correction
    fileArray = run_sliceShift(fileArray,indexSet,outFileDirs.motionCorrect,smoothingPar);
end;
fprintf('STATUS:{"message":"Running gating..."}\n');
if analysisTasks(3) == 1 % do the gating
    fileArray = runGating(fileArray,indexSet,outFileDirs.gate,gatingPar);
end;
fprintf('STATUS:{"message":"Running initial layer identification..."}\n');
if analysisTasks(4) == 1 % do the initial layer identification
    fileArray = run_extremalBdryID(fileArray,indexSet,outFileDirs.layers,initialLayerPar);
end;
fprintf('STATUS:{"message":"Running initial layer smoothing..."}\n');
if analysisTasks(5) == 1 % do the initial layer smoothing
    fileArray = run_layerSmoothing(fileArray,indexSet,outFileDirs.layers,layerSmoothingPar);
end;
fprintf('STATUS:{"message":"Generating thickness maps..."}\n');
if analysisTasks(6) == 1 % generate thickness maps
    fileArray = run_computeThickness(fileArray,indexSet,outFileDirs.stats,thicknessPar);
end;
fprintf('STATUS:{"message":"Computing statistics..."}\n');
if analysisTasks(7) == 1 % compute statistics
    fileArray = run_computeStatistics(fileArray,indexSet,outFileDirs.stats,statPar);
end;
fprintf('STATUS:{"message":"Identifying internal layers..."}\n');
if analysisTasks(8) == 1 % 'identifyInternalLayers'
    fileArray = run_internalLayers(fileArray,indexSet,outFileDirs.layers,internalLayerPar);
end;
fprintf('STATUS:{"message":"Smoothing internal layers..."}\n');
if analysisTasks(9) == 1 % 'smoothInternalLayers'
    fileArray = run_smoothInternalLayers(fileArray,indexSet,outFileDirs.layers,intLayerSmoothingPar);
end;
fprintf('STATUS:{"message":"Running CFAR process..."}\n');
if analysisTasks(10) == 1 % 'CFARProcess'
    fileArray = run_CFARProcess(fileArray,indexSet,outFileDirs.cfar,cfarPar);
end;
fprintf('STATUS:{"message":"Running CFAR clustering..."}\n');
if analysisTasks(11) == 1 % 'clusterCFARExceedances'
    fileArray = run_CFARClustering(fileArray,indexSet,outFileDirs.cfar,cfarClusterPar);
end;
fprintf('STATUS:{"message":"Running cluster features classifier..."}\n');

run_clusterFeatures( fileArray, indexSet, outFileDirs.cfar );

fprintf('STATUS:{"message":"Exporting results..."}\n');

% Right now we always run this: generate outputs

fileArray = run_generateOutputs(fileArray,indexSet,outFileDirs.output);

end



