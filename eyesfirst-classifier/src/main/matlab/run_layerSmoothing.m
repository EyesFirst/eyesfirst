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

function fileArray = run_layerSmoothing(fileArray,indexSet,outFileDir,smoothingPar)
Nfiles = length(indexSet);

plotFlag=smoothingPar.plotFlag;
smoothFac=smoothingPar.smoothFac;
smoothxy=smoothingPar.smoothxy;
imfig=smoothingPar.imfig;
NsmoothIter=smoothingPar.NsmoothIter;
% maxBdrySlope = initialLayerPar.maxBdrySlope;
% maxInterLayerDist = initialLayerPar.maxInterLayerDist;
% minInterLayerDist = initialLayerPar.minInterLayerDist;
% sliceRange = initialLayerPar.sliceRange
% costFigH = initialLayerPar.costFig;
% layerFigH = initialLayerPar.layerFig;
% clf(costFigH);
% clf(layerFigH);
% dfstd = initialLayerPar.dfstd;
% ksf = initialLayerPar.ksf;
for ii = 1:Nfiles
    %if fileArray{indexSet(ii)}.base.sizeConstraint
        % check that the layers1 file has not already been created
        if isempty(fileArray{indexSet(ii)}.layers1_smooth2d.name)
            % get the _layers1 file
            curLayerFile = fileArray{indexSet(ii)}.layers1.name;
            curLayerFileDir = fileArray{indexSet(ii)}.layers1.dir;
            curCompLayerFile = [curLayerFileDir,filesep,curLayerFile];
            % get the _SAA file
            curSAAFile = fileArray{indexSet(ii)}.SAA.name;
            curSAAFileDir = fileArray{indexSet(ii)}.SAA.dir;
            curCompSAAFile = [curSAAFileDir,filesep,curSAAFile];
            if isempty(curLayerFile) || isempty(curSAAFile)
                error('need to create the layer and SAA files first\n');
            else
                % create the output file name
                curFileBaseName = fileArray{indexSet(ii)}.base.name;
                curFileBaseDir = fileArray{indexSet(ii)}.base.dir;
                if isempty(outFileDir)
                    ofile = [curFileBaseDir,filesep,curFileBaseName,'_Layers1_smooth2d.mat'];
                    fileArray{indexSet(ii)}.layers1_smooth2d.dir = curFileBaseDir ;
                    fileArray{indexSet(ii)}.layers1_smooth2d.name = [curFileBaseName,'_Layers1_smooth2d'];
                    fileArray{indexSet(ii)}.layers1_smooth2d.multiples = 0;
                else
                    ofileDir = [curFileBaseDir,filesep,outFileDir];
                    mkdir(ofileDir);
                    ofile = [ofileDir,filesep,curFileBaseName,'_Layers1_smooth2d.mat'];
                    fileArray{indexSet(ii)}.layers1_smooth2d.dir = ofileDir ;
                    fileArray{indexSet(ii)}.layers1_smooth2d.name = [curFileBaseName,'_Layers1_smooth2d'];
                    fileArray{indexSet(ii)}.layers1_smooth2d.multiples = 0;
                end
                stLayerBdrys = smoothBoundaries2_v2(curCompLayerFile,curCompSAAFile,plotFlag,smoothFac,smoothxy,imfig,NsmoothIter);
                save(ofile,'stLayerBdrys');
            end;
        end;
    %end;
end;


