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

function [ fileArray ] = run_clusterFeatures( fileArray,indexSet,outFileDir )
    Nfiles = length(indexSet);
    for ii = 1:Nfiles
        clusterFile = [ fileArray{indexSet(ii)}.cfarCluster.dir, filesep, fileArray{indexSet(ii)}.cfarCluster.name ];
        cfarFile = [ fileArray{indexSet(ii)}.cfar.dir, filesep, fileArray{indexSet(ii)}.cfar.name ];
        boundaryFileName = fileArray{indexSet(ii)}.intLayers_smooth2d.name;
        boundaryFileDir = fileArray{indexSet(ii)}.intLayers_smooth2d.dir;
        if isempty(boundaryFileName)
            error('Need to create precursor files (intLayers_smooth2d) first\n');
        end
        boundaryFile = [ boundaryFileDir, filesep, boundaryFileName ];
        outFile = [ fileArray{indexSet(ii)}.base.dir, filesep, outFileDir, filesep, fileArray{indexSet(ii)}.base.name, '_clusterFeatures' ];
        exec_clusterFeatures(clusterFile, cfarFile, boundaryFile, outFile);
    end
end

