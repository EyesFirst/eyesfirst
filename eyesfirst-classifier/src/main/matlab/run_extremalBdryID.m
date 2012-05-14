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

function fileArray = run_extremalBdryID(fileArray,indexSet,outFileDir,initialLayerPar)
Nfiles = length(indexSet);
Nlayers = initialLayerPar.Nlayers;
maxBdrySlope = initialLayerPar.maxBdrySlope;
maxInterLayerDist = initialLayerPar.maxInterLayerDist;
minInterLayerDist = initialLayerPar.minInterLayerDist;
sliceRange = initialLayerPar.sliceRange
costFigH = initialLayerPar.costFig;
layerFigH = initialLayerPar.layerFig;
clf(costFigH);
clf(layerFigH);
dfstd = initialLayerPar.dfstd;
ksf = initialLayerPar.ksf;
for ii = 1:Nfiles
    %if fileArray{indexSet(ii)}.base.sizeConstraint
        % check that the layers1 file has not already been created
        if isempty(fileArray{indexSet(ii)}.layers1.name)
            % get the _SAA file
            curSAAFile = fileArray{indexSet(ii)}.SAA.name;
            curSAAFileDir = fileArray{indexSet(ii)}.SAA.dir;
            curCompSAAFile = [curSAAFileDir,filesep,curSAAFile];
            % get the gate file
            curGateFile = fileArray{indexSet(ii)}.gate.name;
            curGateFileDir = fileArray{indexSet(ii)}.gate.dir;
            curCompGateFile = [curGateFileDir,filesep,curGateFile];
            if isempty(curSAAFile) || isempty(curGateFile)
                error('need to create the SAA and gate files first\n');
            else
                load(curCompSAAFile)
                load(curCompGateFile);
                % create the output file name
                curFileBaseName = fileArray{indexSet(ii)}.base.name;
                curFileBaseDir = fileArray{indexSet(ii)}.base.dir;
                if isempty(outFileDir)
                    ofile = [curFileBaseDir,filesep,curFileBaseName,'_Layers1.mat'];
                    fileArray{indexSet(ii)}.layers1.dir = curFileBaseDir ;
                    fileArray{indexSet(ii)}.layers1.name = [curFileBaseName,'_Layers1'];
                    fileArray{indexSet(ii)}.layers1.multiples = 0;
                else
                    ofileDir = [curFileBaseDir,filesep,outFileDir];
                    mkdir(ofileDir);
                    ofile = [ofileDir,filesep,curFileBaseName,'_Layers1.mat'];
                    fileArray{indexSet(ii)}.layers1.dir = ofileDir ;
                    fileArray{indexSet(ii)}.layers1.name = [curFileBaseName,'_Layers1'];
                    fileArray{indexSet(ii)}.layers1.multiples = 0;
                end
                if isempty(sliceRange)
                    [aa,bb,lastSlice] = size(SAA);
                    firstSlice = 1;
                else
                    firstSlice = sliceRange(1);
                    lastSlice = sliceRange(2);
                end;
                cc = lastSlice-firstSlice+1;
                fprintf('estimating layers\n')
                stLayerBdrys = cell(cc,1);
                ct2cc = 0;
                for rr = firstSlice:lastSlice
                    fprintf('STATUS:{"message":"Running initial layer identification (%d/%d)..."}\n', rr, lastSlice);
                    ct2cc = ct2cc+1;
                    curim = squeeze(SAA(:,:,rr));
                    imtop = floor(polyGate{rr}.extrapLower);
                    imfloor = ceil(polyGate{rr}.extrapUpper);
                    [BdryRelIm,rowOffSet,colOffSet,oimwla,thicknessProf12,thicknessProf13,intensityProf12,intensityProf13,intensityProf1B] = identifyLayers2D_wrd_kim(curim,imtop,imfloor,Nlayers,maxBdrySlope,maxInterLayerDist,minInterLayerDist,costFigH,layerFigH,dfstd,ksf);
                    stLayerBdrys{ct2cc}.BdryRelIm = BdryRelIm;
                    stLayerBdrys{ct2cc}.rowOffSet = rowOffSet;
                    stLayerBdrys{ct2cc}.colOffSet = colOffSet;
                    stLayerBdrys{ct2cc}.oimwla = oimwla;
                    stLayerBdrys{ct2cc}.thicknessProf12 = thicknessProf12;
                    stLayerBdrys{ct2cc}.thicknessProf13 = thicknessProf13;
                    stLayerBdrys{ct2cc}.intensityProf12 = intensityProf12;
                    stLayerBdrys{ct2cc}.intensityProf13 = intensityProf13;
                    stLayerBdrys{ct2cc}.intensityProf1B = intensityProf1B;
                end;
                save(ofile,'stLayerBdrys')
                clear A SAA  ofile ofileBase  curFile stLayerBdrys polyGate SAATB
            end;
        end;
    %end;
end;


