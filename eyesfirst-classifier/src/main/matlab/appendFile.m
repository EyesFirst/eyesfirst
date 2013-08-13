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

function [fileArray] = appendFile(fileArray,filePrefix,fileSuffix,fileDir)
% search for base
foundBase = 0;
Nbases = length(fileArray);
ctBases = 1;
% fileTypes = {'.dcm','_mat.mat','_SAA.mat','_gate.mat','_Layers1.mat','_Layers1_smooth2d.mat', '_thicknessMaps.mat','_thicknessStats.mat','_cfar.mat','_cfar_exceedClust.mat','_cfar_clusterFeatures.mat','intLayers.mat','_intLayers_smooth2d.mat','heViz.ps'};
if strcmp(fileSuffix,'.dcm')
    % check for existing ones and append a new one if it is not there, also
    % check for duplicate file names in different directories
    while ctBases <= Nbases && foundBase == 0
        curBaseName = fileArray{ctBases}.base.name;
        curBaseDir = fileArray{ctBases}.base.dir;
        if strcmp(curBaseName,filePrefix) 
            foundBase = 1;
            if ~strcmp(curBaseDir,fileDir)
               fileArray{ctBases}.base.multiple = 1; % indicates that there are multiple copies of this file
            end
        end;
        ctBases = ctBases+1;
    end;
    if foundBase == 0 % create a new entry
        fileArray{Nbases+1}.base.name = filePrefix;
        fileArray{Nbases+1}.base.dir = fileDir;
        fileArray{Nbases+1}.base.multiple = 0;
        fileArray{Nbases+1}.mat.dir = [];  % matlab file
        fileArray{Nbases+1}.mat.name = [];
        fileArray{Nbases+1}.mat.multiples = [];
        fileArray{Nbases+1}.SAA.dir = [];% SAA file
        fileArray{Nbases+1}.SAA.name = [];
        fileArray{Nbases+1}.SAA.multiples = [];
        fileArray{Nbases+1}.gate.dir = []; % gate file
        fileArray{Nbases+1}.gate.name = [];
        fileArray{Nbases+1}.gate.multiples = [];
        fileArray{Nbases+1}.layers1.dir = []; % initial layer file
        fileArray{Nbases+1}.layers1.name = [];
        fileArray{Nbases+1}.layers1.multiples = [];
        fileArray{Nbases+1}.layers1_smooth2d.dir = []; % smoothed initial layer file
        fileArray{Nbases+1}.layers1_smooth2d.name = [];
        fileArray{Nbases+1}.layers1_smooth2d.multiples = [];
        fileArray{Nbases+1}.intLayers.name = [];  % internal layer file
        fileArray{Nbases+1}.intLayers.dir = [];
        fileArray{Nbases+1}.intLayers.multiples = [];
        fileArray{Nbases+1}.intLayers_smooth2d.name  = [];% smoothed internal layer file
        fileArray{Nbases+1}.intLayers_smooth2d.dir = [];
        fileArray{Nbases+1}.intLayers_smooth2d.multiples = [];
        
        
        fileArray{Nbases+1}.intLayers_smooth2d_TO.name  = [];% smoothed internal layer file
        fileArray{Nbases+1}.intLayers_smooth2d_TO.dir = [];
        fileArray{Nbases+1}.intLayers_smooth2d_TO.multiples = [];
        
        fileArray{Nbases+1}.cfar.name  = [];      % cfar file
        fileArray{Nbases+1}.cfar.dir = [];
        fileArray{Nbases+1}.cfar.multiples = [];
        fileArray{Nbases+1}.cfarCluster.name   = []; % cfar cluster file
        fileArray{Nbases+1}.cfarCluster.dir = [];
        fileArray{Nbases+1}.cfarCluster.multiples = [];
        fileArray{Nbases+1}.cfarClusterFeatures.name   = []; % moments of cfar clusters
        fileArray{Nbases+1}.cfarClusterFeatures.dir = [];
        fileArray{Nbases+1}.cfarClusterFeatures.multiples = [];
        fileArray{Nbases+1}.thicknessStats.name  = [];     % thickness statistics
        fileArray{Nbases+1}.thicknessStats.dir = [];
        fileArray{Nbases+1}.thicknessStats.multiples = [];
        fileArray{Nbases+1}.thicknessMaps.name  = [];    % thicknessMaps
        fileArray{Nbases+1}.thicknessMaps.dir = [];
        fileArray{Nbases+1}.thicknessMaps.multiples  = [];
        fileArray{Nbases+1}.heViz.name  = [];    % HE image file
        fileArray{Nbases+1}.heViz.dir = [];
        fileArray{Nbases+1}.heViz.multiples  = [];
    end;
else
    while ctBases <= Nbases && foundBase == 0
        curBaseName = fileArray{ctBases}.base.name;
        if strcmp(curBaseName,filePrefix)
            foundBase = 1;
            if strcmp(fileSuffix,'_mat')
                %check to see if already assigned
                if isempty(fileArray{ctBases}.mat.name)
                    fileArray{ctBases}.mat.name = [curBaseName,'_mat'];
                    fileArray{ctBases}.mat.dir = fileDir;
                    fileArray{ctBases}.mat.multiples = 0;
                else
                    if ~strcmp(fileArray{ctBases}.mat.dir,fileDir)
                        fileArray{ctBases}.mat.multiples = 1;
                    end;
                end;
            elseif strcmp(fileSuffix,'_SAA')
                %check to see if already assigned
                if isempty(fileArray{ctBases}.SAA.name)
                    fileArray{ctBases}.SAA.name = [curBaseName,'_SAA'];
                    fileArray{ctBases}.SAA.dir = fileDir;
                    fileArray{ctBases}.SAA.multiples = 0;
                else
                    if ~strcmp(fileArray{ctBases}.SAA.dir,fileDir)
                        fileArray{ctBases}.SAA.multiples = 1;
                    end;
                end;
            elseif strcmp(fileSuffix,'_gate')
                %check to see if already assigned
                if isempty(fileArray{ctBases}.gate.name)
                    fileArray{ctBases}.gate.name = [curBaseName,'_gate'];
                    fileArray{ctBases}.gate.dir = fileDir;
                    fileArray{ctBases}.gate.multiples = 0;
                else
                    if ~strcmp(fileArray{ctBases}.gate.dir,fileDir)
                        fileArray{ctBases}.gate.multiples = 1;
                    end;
                end;
            elseif strcmp(fileSuffix,'_Layers1')
                %check to see if already assigned
                if isempty(fileArray{ctBases}.layers1.name)
                    fileArray{ctBases}.layers1.name = [curBaseName,'_Layers1'];
                    fileArray{ctBases}.layers1.dir = fileDir;
                    fileArray{ctBases}.layers1.multiples = 0;
                else
                    if ~strcmp(fileArray{ctBases}.layers1.dir,fileDir)
                        fileArray{ctBases}.layers1.multiples = 1;
                    end;
                end;
            elseif strcmp(fileSuffix,'_Layers1_smooth2d')
                %check to see if already assigned
                if isempty(fileArray{ctBases}.layers1_smooth2d.name)
                    fileArray{ctBases}.layers1_smooth2d.name = [curBaseName,'_Layers1_smooth2d'];
                    fileArray{ctBases}.layers1_smooth2d.dir = fileDir;
                    fileArray{ctBases}.layers1_smooth2d.multiples = 0;
                else
                    if ~strcmp(fileArray{ctBases}.layers1_smooth2d.dir,fileDir)
                        fileArray{ctBases}.layers1_smooth2d.multiples = 1;
                    end;
                end;
            elseif strcmp(fileSuffix,'_thicknessMaps')
                %check to see if already assigned
                if isempty(fileArray{ctBases}.thicknessMaps.name)
                    fileArray{ctBases}.thicknessMaps.name = [curBaseName,'_thicknessMaps'];
                    fileArray{ctBases}.thicknessMaps.dir = fileDir;
                    fileArray{ctBases}.thicknessMaps.multiples = 0;
                else
                    if ~strcmp(fileArray{ctBases}.thicknessMaps.dir,fileDir)
                        fileArray{ctBases}.thicknessMaps.multiples = 1;
                    end;
                end;
            elseif strcmp(fileSuffix,'_thicknessStats')
                %check to see if already assigned
                if isempty(fileArray{ctBases}.thicknessStats.name)
                    fileArray{ctBases}.thicknessStats.name = [curBaseName,'_thicknessStats'];
                    fileArray{ctBases}.thicknessStats.dir = fileDir;
                    fileArray{ctBases}.thicknessStats.multiples = 0;
                else
                    if ~strcmp(fileArray{ctBases}.thicknessStats.dir,fileDir)
                        fileArray{ctBases}.thicknessStats.multiples = 1;
                    end;
                end;
            elseif strcmp(fileSuffix,'_cfar')
                %check to see if already assigned
                if isempty(fileArray{ctBases}.cfar.name)
                    fileArray{ctBases}.cfar.name = [curBaseName,'_cfar'];
                    fileArray{ctBases}.cfar.dir = fileDir;
                    fileArray{ctBases}.cfar.multiples = 0;
                else
                    if ~strcmp(fileArray{ctBases}.cfar.dir,fileDir)
                        fileArray{ctBases}.cfar.multiples = 1;
                    end;
                end;
            elseif strcmp(fileSuffix,'_cfar_exceedClust')
                %check to see if already assigned
                if isempty(fileArray{ctBases}.cfarCluster.name)
                    fileArray{ctBases}.cfarCluster.name = [curBaseName,'_cfar_exceedClust'];
                    fileArray{ctBases}.cfarCluster.dir = fileDir;
                    fileArray{ctBases}.cfarCluster.multiples = 0;
                else
                    if ~strcmp(fileArray{ctBases}.cfarCluster.dir,fileDir)
                        fileArray{ctBases}.cfarCluster.multiples = 1;
                    end;
                end;
            elseif strcmp(fileSuffix,'_cfar_clusterFeatures.mat')
                %check to see if already assigned
                if isempty(fileArray{ctBases}.cfarClusterFeatures.name)
                    fileArray{ctBases}.cfarClusterFeatures.name = [curBaseName,'_cfar_clusterFeatures'];
                    fileArray{ctBases}.cfarClusterFeatures.dir = fileDir;
                    fileArray{ctBases}.cfarClusterFeatures.multiples = 0;
                else
                    if ~strcmp(fileArray{ctBases}.cfarClusterFeatures.dir,fileDir)
                        fileArray{ctBases}.cfarClusterFeatures.multiples = 1;
                    end;
                end;
            elseif strcmp(fileSuffix,'_intLayers')
                %check to see if already assigned
                if isempty(fileArray{ctBases}.intLayers.name)
                    fileArray{ctBases}.intLayers.name = [curBaseName,'_intLayers'];
                    fileArray{ctBases}.intLayers.dir = fileDir;
                    fileArray{ctBases}.intLayers.multiples = 0;
                else
                    if ~strcmp(fileArray{ctBases}.intLayers.dir,fileDir)
                        fileArray{ctBases}.intLayers.multiples = 1;
                    end;
                end;
                
            elseif strcmp(fileSuffix,'_intLayers_smooth2d')
                %check to see if already assigned
                if isempty(fileArray{ctBases}.intLayers_smooth2d.name)
                    fileArray{ctBases}.intLayers_smooth2d.name = [curBaseName,'_intLayers_smooth2d'];
                    fileArray{ctBases}.intLayers_smooth2d.dir = fileDir;
                    fileArray{ctBases}.intLayers_smooth2d.multiples = 0;
                else
                    if ~strcmp(fileArray{ctBases}.intLayer_smooth2d.dir,fileDir)
                        fileArray{ctBases}.intLayers_smooth2d.multiples = 1;
                    end;
                end;
            end;
        end;
        ctBases = ctBases+1;
    end;
            
%         fileArray{ii}.base.dir    % dicom file
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
% fileArray{ii}.intLayer.name   % internal layer file
% fileArray{ii}.intLayer.dir
% fileArray{ii}.intLayer.multiples
% fileArray{ii}.intLayer_smooth2d.name  % smoothed internal layer file
% fileArray{ii}.intLayer_smooth2d.dir
% fileArray{ii}.intLayer_smooth2d.multiples
% fileArray{ii}.cfar.name       % cfar file
% fileArray{ii}.cfar.dir
% fileArray{ii}.cfar.multiples
% fileArray{ii}.cfarCluster.name   % cfar cluster file
% fileArray{ii}.cfarCluster.dir
% fileArray{ii}.cfarCluster.multiples
% fileArray{ii}.cfarClusterFeatures.name   % moments of cfar clusters
% fileArray{ii}.cfarClusterFeatuers.dir
% fileArray{ii}.cfarClusterFeatures.multiples
% fileArray{ii}.thicknessStats.name      % thickness statistics
% fileArray{ii}.thicknessStats.dir
% fileArray{ii}.thicknessStats.multiples
% fileArray{ii}.thicknessMaps.name      % thicknessMaps
% fileArray{ii}.thicknessMaps.dir
% fileArray{ii}.thicknessMaps.multiples 


end

