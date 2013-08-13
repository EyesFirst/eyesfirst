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

function fileArray = run_internalLayers(fileArray,indexSet,outFileDir,internalLayerPar)
Nfiles = length(indexSet);


%fileArray{ii}.intLayer.name   % internal layer file
% fileArray{ii}.intLayer.dir
% fileArray{ii}.intLayer.multiples
for ii = 1:Nfiles
    % check that the internalLayerFile has not already been created
    if isempty(fileArray{indexSet(ii)}.intLayers.name)
        % SAAFile,bdryFile,ksf,thicknessMapFile,statFile
        % get the SAA file 
         curSAAFile = fileArray{indexSet(ii)}.SAA.name;
         curSAAFileDir = fileArray{indexSet(ii)}.SAA.dir;
         curCompSAAFile = [curSAAFileDir,filesep,curSAAFile];
        % get the layers1_smoothed file
         curLayerFile = fileArray{indexSet(ii)}.layers1_smooth2d.name;
         curLayerFileDir = fileArray{indexSet(ii)}.layers1_smooth2d.dir;
         curCompLayerFile = [curLayerFileDir,filesep,curLayerFile];
         % get the thicknessMapFile
         curThicknessMapFile = fileArray{indexSet(ii)}.thicknessMaps.name;
         curThicknessMapDir = fileArray{indexSet(ii)}.thicknessMaps.dir;
         curCompThicknessMapFile = [curThicknessMapDir,filesep,curThicknessMapFile];         
         % get the statFile
         curThicknessStatFile = fileArray{indexSet(ii)}.thicknessStats.name;
         curThicknessStatDir = fileArray{indexSet(ii)}.thicknessStats.dir;
         curCompThicknessStatFile = [curThicknessStatDir,filesep,curThicknessStatFile];
         if isempty(curLayerFile) || isempty(curSAAFile) || isempty(curThicknessStatFile)
             error('need to create precursor files\n');
         else
            curFileBaseName = fileArray{indexSet(ii)}.base.name;
            curFileBaseDir = fileArray{indexSet(ii)}.base.dir;
            if isempty(outFileDir)
               ofile = [curFileBaseDir,filesep,curFileBaseName,'_intLayers.mat'];
               fileArray{indexSet(ii)}.intLayers.dir = curFileBaseDir ;
               fileArray{indexSet(ii)}.intLayers.name = [curFileBaseName,'_intLayers'];
               fileArray{indexSet(ii)}.intLayers.multiples = 0;
            else
                ofileDir = [curFileBaseDir,filesep,outFileDir];
                mkdir(ofileDir);
                ofile = [ofileDir,filesep,curFileBaseName,'_intLayers.mat'];
                fileArray{indexSet(ii)}.intLayers.dir = ofileDir ;
                fileArray{indexSet(ii)}.intLayers.name = [curFileBaseName,'_intLayers'];
                fileArray{indexSet(ii)}.intLayers.multiples = 0;
            end
            load([fileArray{indexSet(ii)}.mat.dir, filesep, fileArray{indexSet(ii)}.mat.name], 'pixelDim');
            intLayerBdrys = identifyInternalLayers(curCompSAAFile,curCompLayerFile,curCompThicknessStatFile,internalLayerPar,pixelDim);
            save(ofile,'intLayerBdrys');
         end;
    end;
end;


