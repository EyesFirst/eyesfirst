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

function fileArray = run_CFARProcess(fileArray,indexSet,outFileDir,cfarPar)
Nfiles = length(indexSet);

for ii = 1:Nfiles
    % check that the cfarFile has not already been created
    if isempty(fileArray{indexSet(ii)}.cfar.name)
        % SAAFile,bdryFile,ksf,thicknessMapFile,statFile
        % get the SAA file 
         curSAAFile = fileArray{indexSet(ii)}.SAA.name;
         curSAAFileDir = fileArray{indexSet(ii)}.SAA.dir;
         curCompSAAFile = [curSAAFileDir,filesep,curSAAFile];
        % get the layers1_smoothed file
         curLayerFile = fileArray{indexSet(ii)}.layers1_smooth2d.name;
         curLayerFileDir = fileArray{indexSet(ii)}.layers1_smooth2d.dir;
         curCompLayerFile = [curLayerFileDir,filesep,curLayerFile];
         if isempty(curLayerFile) || isempty(curSAAFile)
             error('need to create precursor files\n');
         else
            curFileBaseName = fileArray{indexSet(ii)}.base.name;
            curFileBaseDir = fileArray{indexSet(ii)}.base.dir;
            if isempty(outFileDir)
               ofile = [curFileBaseDir,filesep,curFileBaseName,'_cfar.mat'];
               fileArray{indexSet(ii)}.cfar.dir = curFileBaseDir ;
               fileArray{indexSet(ii)}.cfar.name = [curFileBaseName,'_cfar'];
               fileArray{indexSet(ii)}.cfar.multiples = 0;
            else
                ofileDir = [curFileBaseDir,filesep,outFileDir];
                mkdir(ofileDir);
                ofile = [ofileDir,filesep,curFileBaseName,'_cfar.mat'];
                fileArray{indexSet(ii)}.cfar.dir = ofileDir ;
                fileArray{indexSet(ii)}.cfar.name = [curFileBaseName,'_cfar'];
                fileArray{indexSet(ii)}.cfar.multiples = 0;
            end
            [cfarSAA,curx] = runOCTcfar(curCompSAAFile,curCompLayerFile,cfarPar);
            save(ofile,'cfarSAA','curx');
         end;
    end;
end;

% fileArray{ii}.cfar.name       % cfar file
% fileArray{ii}.cfar.dir
% fileArray{ii}.cfar.multiples
% fileArray{ctBases}.cfar.name = [curBaseName,'_cfar'];
