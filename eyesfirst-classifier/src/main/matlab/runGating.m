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

function fileArray = runGating(fileArray,indexSet,outFileDir,gatingPar)
Nfiles = length(indexSet);
edf = gatingPar.edf;
ndil = gatingPar.ndil;
sliceRange = gatingPar.sliceRange;
for ii = 1:Nfiles
    %if fileArray{indexSet(ii)}.base.sizeConstraint
        % check that the gate file has not already been created
        if isempty(fileArray{indexSet(ii)}.gate.name)
            % get the _SAA file
            curSAAFile = fileArray{indexSet(ii)}.SAA.name;
            curSAAFileDir = fileArray{indexSet(ii)}.SAA.dir;
            curCompSAAFile = [curSAAFileDir,filesep,curSAAFile];
            if isempty(curSAAFile) || isempty(curCompSAAFile)
                error('need to create the SAA files first\n');
            else
                load(curCompSAAFile)
                % create the output file name
                curFileBaseName = fileArray{indexSet(ii)}.base.name;
                curFileBaseDir = fileArray{indexSet(ii)}.base.dir;
                if isempty(outFileDir)
                    ofile = [curFileBaseDir,filesep,curFileBaseName,'_gate.mat'];
                    fileArray{indexSet(ii)}.gate.dir = curFileBaseDir ;
                    fileArray{indexSet(ii)}.gate.name = [curFileBaseName,'_gate'];
                    fileArray{indexSet(ii)}.gate.multiples = 0;
                else
                    ofileDir = [curFileBaseDir,filesep,outFileDir];
                    mkdir(ofileDir);
                    ofile = [ofileDir,filesep,curFileBaseName,'_gate.mat'];
                    fileArray{indexSet(ii)}.gate.dir = ofileDir ;
                    fileArray{indexSet(ii)}.gate.name = [curFileBaseName,'_gate'];
                    fileArray{indexSet(ii)}.gate.multiples = 0;
                end
                if isempty(sliceRange)
                    [aa,bb,cc] = size(SAA);
                    sliceRange = [1 cc];
                end;
                [polyGate,SAATB] = initialLayerId(SAA,ofile,edf,ndil,sliceRange);       
            end;
        end;
    %end
end;


end

