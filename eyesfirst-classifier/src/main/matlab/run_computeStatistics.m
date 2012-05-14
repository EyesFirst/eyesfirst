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

function fileArray = run_computeStatistics(fileArray,indexSet,outFileDir,statPar)
Nfiles = length(indexSet);
for ii = 1:Nfiles
   %if fileArray{indexSet(ii)}.base.sizeConstraint
        % check that the statistics file has not already been created
        if isempty(fileArray{indexSet(ii)}.thicknessStats.name)
            % get the thicknessMaps file
            curThicknessMapFile = fileArray{indexSet(ii)}.thicknessMaps.name;
            curThicknessMapDir = fileArray{indexSet(ii)}.thicknessMaps.dir;
            curCompThicknessMapFile = [curThicknessMapDir,filesep,curThicknessMapFile];
            if isempty(curThicknessMapFile)
                error('need to create the thicknessMap files first\n');
            else
                % create the output file name
                curFileBaseName = fileArray{indexSet(ii)}.base.name;
                curFileBaseDir = fileArray{indexSet(ii)}.base.dir;
                if isempty(outFileDir)
                    ofile = [curFileBaseDir,filesep,curFileBaseName,'_thicknessStats.mat'];
                    fileArray{indexSet(ii)}.thicknessStats.dir = curFileBaseDir;
                    fileArray{indexSet(ii)}.thicknessStats.name = [curFileBaseName,'_thicknessStats'];
                    fileArray{indexSet(ii)}.thicknessStats.multiples = 0;
                else
                    ofileDir = [curFileBaseDir,filesep,outFileDir];
                    mkdir(ofileDir);
                    ofile = [ofileDir,filesep,curFileBaseName,'_thicknessStats.mat'];
                    fileArray{indexSet(ii)}.thicknessStats.dir = ofileDir;
                    fileArray{indexSet(ii)}.thicknessStats.name = [curFileBaseName,'_thicknessStats'];
                    fileArray{indexSet(ii)}.thicknessStats.multiples = 0;
                end
                thicknessFeatures  =  calculateThicknessStats(curCompThicknessMapFile,statPar);
                save(ofile,'thicknessFeatures');
            end;
        end;
    %end;
end;


