% Copyright 2013 The MITRE Corporation
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

function fileArray = runEvaluateClusters(fileArray,indexSet,outFileDir,cfPar)
Nfiles = length(indexSet);
for ii = 1:Nfiles
    %if fileArray{indexSet(ii)}.base.sizeConstraint
    % check that the clusterFeature file has not already been created
    if isempty(fileArray{indexSet(ii)}.cfarClusterFeatures.name)
        % get the required files
        
        curcfarFile = fileArray{indexSet(ii)}.cfar.name;
        curcfarDir = fileArray{indexSet(ii)}.cfar.dir;
        curCompCfarFile = [curcfarDir,filesep,curcfarFile];
        
        
        curSAAFile = fileArray{indexSet(ii)}.SAA.name;
        curSAADir = fileArray{indexSet(ii)}.SAA.dir;
        curCompSAAFile = [curSAADir,filesep,curSAAFile];
        
        curcfarClusterFile = fileArray{indexSet(ii)}.cfarCluster.name;
        curcfarClusterDir = fileArray{indexSet(ii)}.cfarCluster.dir;
        curCompCfarClusterFile = [curcfarClusterDir,filesep,curcfarClusterFile];
        
        curIntboundaryFile = fileArray{indexSet(ii)}.intLayers_smooth2d.name;
        curIntboundaryDir = fileArray{indexSet(ii)}.intLayers_smooth2d.dir;
        curCompIntBoundaryFile = [curIntboundaryDir,filesep,curIntboundaryFile];
        
        
        curExtboundaryFile = fileArray{indexSet(ii)}.layers1_smooth2d.name;
        curExtboundaryDir = fileArray{indexSet(ii)}.layers1_smooth2d.dir;
        curCompExtBoundaryFile = [curExtboundaryDir,filesep,curExtboundaryFile];
        
        curMapFile = fileArray{indexSet(ii)}.thicknessMaps.name;
        curMapDir = fileArray{indexSet(ii)}.thicknessMaps.dir;
        curCompMapFile = [curMapDir,filesep,curMapFile];
        
        if isempty(curcfarClusterFile)
            error('need to create the cfarCluster file first\n');
        else
            % create the output file name
            curFileBaseName = fileArray{indexSet(ii)}.base.name;
            curFileBaseDir = fileArray{indexSet(ii)}.base.dir;
            if isempty(outFileDir)
                cfofile = [curFileBaseDir,filesep,curFileBaseName,'_cfar_clusterFeatures.mat'];
                heVizOfile = [curFileBaseDir,filesep,curFileBaseName,'_heViz.ps'];
                
                fileArray{indexSet(ii)}.cfarClusterFeatures.dir = curFileBaseDir;
                fileArray{indexSet(ii)}.cfarClusterFeatures.name = [curFileBaseName,'_cfar_clusterFeatures'];
                fileArray{indexSet(ii)}.cfarClusterFeatures.multiples = 0;
                
                fileArray{indexSet(ii)}.heViz.dir = curFileBaseDir;
                fileArray{indexSet(ii)}.heViz.name = [curFileBaseName,'_heViz'];
                fileArray{indexSet(ii)}.heViz.multiples = 0;
            else
                cfofileDir = [curFileBaseDir,filesep,outFileDir];
                mkdir(cfofileDir);
                cfofile = [cfofileDir,filesep,curFileBaseName,'_cfar_clusterFeatures.mat'];
                fileArray{indexSet(ii)}.cfarClusterFeatures.dir = cfofileDir;
                fileArray{indexSet(ii)}.cfarClusterFeatures.name = [curFileBaseName,'_cfar_clusterFeatures'];
                fileArray{indexSet(ii)}.cfarClusterFeatures.multiples = 0;
                
                heVizOfile = [cfofileDir,filesep,curFileBaseName,'_heViz.ps'];
                fileArray{indexSet(ii)}.heViz.dir = cfofileDir;
                fileArray{indexSet(ii)}.heViz.name = [curFileBaseName,'_heViz'];
                fileArray{indexSet(ii)}.heViz.multiples = 0;
            end
            load([fileArray{indexSet(ii)}.mat.dir, filesep, fileArray{indexSet(ii)}.mat.name], 'pixelDim');
            [cf,nsv,pv,cv,stCounts,cpd,cpfa,candidateHE]  =  evaluateClusters(curCompCfarFile,curCompCfarClusterFile,curCompIntBoundaryFile,cfPar,pixelDim);
            save(cfofile,'cf','nsv','pv','cv','stCounts','cpd','cpfa','candidateHE');
            % No need to do this during regular processing:
            % put the plot command right here!!!!!!!!!!!!!!!!
            viewHardExudate(curCompSAAFile,curCompCfarFile,cfofile,curCompExtBoundaryFile,curCompIntBoundaryFile,curCompMapFile,heVizOfile)
        end;
    end;
end;

end

