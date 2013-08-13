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

function fileArray = run_computeThickness(fileArray,indexSet,outFileDir,thicknessPar)
Nfiles = length(indexSet);
useCorrect = thicknessPar.useCosCorrect; 
smoothFlag = thicknessPar.smoothFlag;
for ii = 1:Nfiles
    %if fileArray{indexSet(ii)}.base.sizeConstraint
        % check that the thicknessMap file has not already been created
        if isempty(fileArray{indexSet(ii)}.thicknessMaps.name)
            % get the layers1_smoothed file
            curLayerFile = fileArray{indexSet(ii)}.layers1_smooth2d.name;
            curLayerFileDir = fileArray{indexSet(ii)}.layers1_smooth2d.dir;
            curCompLayerFile = [curLayerFileDir,filesep,curLayerFile];
            if isempty(curLayerFile)
                error('need to create the smooth layer files first\n');
            else
                % create the output file name
                curFileBaseName = fileArray{indexSet(ii)}.base.name;
                curFileBaseDir = fileArray{indexSet(ii)}.base.dir;
                if isempty(outFileDir)
                    ofile = [curFileBaseDir,filesep,curFileBaseName,'_thicknessMaps.mat'];
                    fileArray{indexSet(ii)}.thicknessMaps.dir = curFileBaseDir ;
                    fileArray{indexSet(ii)}.thicknessMaps.name = [curFileBaseName,'_thicknessMaps'];
                    fileArray{indexSet(ii)}.thicknessMaps.multiples = 0;
                else
                    ofileDir = [curFileBaseDir,filesep,outFileDir];
                    mkdir(ofileDir);
                    ofile = [ofileDir,filesep,curFileBaseName,'_thicknessMaps.mat'];
                    fileArray{indexSet(ii)}.thicknessMaps.dir = ofileDir ;
                    fileArray{indexSet(ii)}.thicknessMaps.name = [curFileBaseName,'_thicknessMaps'];
                    fileArray{indexSet(ii)}.thicknessMaps.multiples = 0;
                end
                [thickMap1,thickMap2,intenseMap1,intenseMap2,intenseMap3,reflectivityMap1,reflectivityMap2] = thicknessIntensityMaps2d_2(curCompLayerFile,useCorrect,smoothFlag);
                save(ofile,'thickMap1','thickMap2','intenseMap1','intenseMap2','intenseMap3','reflectivityMap1','reflectivityMap2');
            end;
        end;
    %end;
end;


