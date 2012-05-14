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

function fileArray = run_sliceShift(fileArray,indexSet,outFileDir,smoothingPar)
Nfiles = length(indexSet);
xdeg = smoothingPar.xdeg;
zdeg = smoothingPar.zdeg;
for ii = 1:Nfiles
    % check size constraint
    %if fileArray{indexSet(ii)}.base.sizeConstraint
    % check that the SAA file has not already been created
        if isempty(fileArray{indexSet(ii)}.SAA.name)
            % get the _mat file
             curMatFile = fileArray{indexSet(ii)}.mat.name;
             curMatFileDir = fileArray{indexSet(ii)}.mat.dir;
             curCompMatFile = [curMatFileDir,filesep,curMatFile];
             if isempty(curMatFile) || isempty(curCompMatFile)
                 error('need to create the mat files first\n');
             else
                load(curCompMatFile)
                % create the output file name
                curFileBaseName = fileArray{indexSet(ii)}.base.name;
                curFileBaseDir = fileArray{indexSet(ii)}.base.dir;
                if isempty(outFileDir)
                   ofile = [curFileBaseDir,filesep,curFileBaseName,'_SAA.mat'];
                   fileArray{indexSet(ii)}.SAA.dir = curFileBaseDir ;
                   fileArray{indexSet(ii)}.SAA.name = [curFileBaseName,'_SAA'];
                   fileArray{indexSet(ii)}.SAA.multiples = 0;
                else
                    ofileDir = [curFileBaseDir,filesep,outFileDir];
                    mkdir(ofileDir);
                    ofile = [ofileDir,filesep,curFileBaseName,'_SAA.mat'];
                    fileArray{indexSet(ii)}.SAA.dir = ofileDir ;
                    fileArray{indexSet(ii)}.SAA.name = [curFileBaseName,'_SAA'];
                    fileArray{indexSet(ii)}.SAA.multiples = 0;
                end
                SAA = slice_shift(A,xdeg,zdeg,ofile);
             end;
        end;
    %end;
end;


