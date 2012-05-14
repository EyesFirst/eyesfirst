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

function fileArray = run_smoothInternalLayers(fileArray,indexSet,outFileDir,intLayerSmoothingPar)
Nfiles = length(indexSet);


%fileArray{ii}.intLayer.name   % internal layer file
% fileArray{ii}.intLayer.dir
% fileArray{ii}.intLayer.multiples
for ii = 1:Nfiles
    % check that the smoothInternalLayerFile has not already been created
    if isempty(fileArray{indexSet(ii)}.intLayers_smooth2d.name)
        % SAAFile,bdryFile,ksf,thicknessMapFile,statFile
        % get the SAA file 
         curSAAFile = fileArray{indexSet(ii)}.SAA.name;
         curSAAFileDir = fileArray{indexSet(ii)}.SAA.dir;
         curCompSAAFile = [curSAAFileDir,filesep,curSAAFile];
        % get the internal layerfile
         curIntLayerFile = fileArray{indexSet(ii)}.intLayers.name;  % internal layer file
         curIntLayerFileDir = fileArray{indexSet(ii)}.intLayers.dir;
         curCompIntLayerFile = [curIntLayerFileDir,filesep,curIntLayerFile];
         if isempty(curIntLayerFile) || isempty(curSAAFile) 
             error('need to create precursor files\n');
         else
            curFileBaseName = fileArray{indexSet(ii)}.base.name;
            curFileBaseDir = fileArray{indexSet(ii)}.base.dir;
            if isempty(outFileDir)
               ofile = [curFileBaseDir,filesep,curFileBaseName,'_intLayers_smooth2d.mat'];
               fileArray{indexSet(ii)}.intLayers_smooth2d.dir = curFileBaseDir ;
               fileArray{indexSet(ii)}.intLayers_smooth2d.name = [curFileBaseName,'_intLayers_smooth2d'];
               fileArray{indexSet(ii)}.intLayers_smooth2d.multiples = 0;
            else
                ofileDir = [curFileBaseDir,filesep,outFileDir];
                mkdir(ofileDir);
                ofile = [ofileDir,filesep,curFileBaseName,'_intLayers_smooth2d.mat'];
                fileArray{indexSet(ii)}.intLayers_smooth2d.dir = ofileDir ;
                fileArray{indexSet(ii)}.intLayers_smooth2d.name = [curFileBaseName,'_intLayers_smooth2d'];
                fileArray{indexSet(ii)}.intLayers_smooth2d.multiples = 0;
            end
            intLayerBdrys_smooth2d = smoothBoundariesIntLayers(curCompIntLayerFile,curCompSAAFile,intLayerSmoothingPar);
            save(ofile,'intLayerBdrys_smooth2d');
         end;
    end;
end;


