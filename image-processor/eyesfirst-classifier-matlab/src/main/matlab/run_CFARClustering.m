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

function fileArray = run_CFARClustering(fileArray,indexSet,outFileDir,cfarClusterPar)
Nfiles = length(indexSet);

for ii = 1:Nfiles
    % check that the cfarFile has not already been created
    if isempty(fileArray{indexSet(ii)}.cfarCluster.name)
        % SAAFile,bdryFile,ksf,thicknessMapFile,statFile
        % get the cfar file 
         curCFARFile = fileArray{indexSet(ii)}.cfar.name;
         curCFARFileDir = fileArray{indexSet(ii)}.cfar.dir;
         curCompCFARFile = [curCFARFileDir,filesep,curCFARFile];
         if isempty(curCFARFile)
             error('need to create precursor files\n');
         else
            curFileBaseName = fileArray{indexSet(ii)}.base.name;
            curFileBaseDir = fileArray{indexSet(ii)}.base.dir;
            if isempty(outFileDir)
               ofile = [curFileBaseDir,filesep,curFileBaseName,'_cfar_exceedClust.mat'];
               fileArray{indexSet(ii)}.cfarCluster.dir = curFileBaseDir;
               fileArray{indexSet(ii)}.cfarCluster.name = [curFileBaseName,'_cfar_exceedClust'];
               fileArray{indexSet(ii)}.cfarCluster.multiples = 0;
            else
                ofileDir = [curFileBaseDir,filesep,outFileDir];
                mkdir(ofileDir);
                ofile = [ofileDir,filesep,curFileBaseName,'_cfar_exceedClust.mat'];
                fileArray{indexSet(ii)}.cfarCluster.dir = ofileDir ;
                fileArray{indexSet(ii)}.cfarCluster.name = [curFileBaseName,'_cfar_exceedClust'];
                fileArray{indexSet(ii)}.cfarCluster.multiples = 0;
            end
            exceedClusters = runClusterCFAR(curCompCFARFile,cfarClusterPar);
            save(ofile,'exceedClusters');
         end;
    end;
end;

% fileArray{ii}.cfar.name       % cfar file
% fileArray{ii}.cfar.dir
% fileArray{ii}.cfar.multiples
% fileArray{ctBases}.cfar.name = [curBaseName,'_cfar'];
