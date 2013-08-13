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

function [ fileArray ] = run_generateOutputs(fileArray, indexSet, outFileDir)
%run_generateOutputs Generate output DICOM file and various other
%artifacts.
%   Generates the various outputs.
    Nfiles = length(indexSet);
    for ii = 1:Nfiles
        fileIn = [ fileArray{ii}.base.dir, filesep, fileArray{ii}.base.name, '.dcm' ];
        fileOut = [ fileArray{ii}.base.dir, filesep, outFileDir, filesep, fileArray{ii}.base.name ];
        layers = [ fileArray{ii}.layers1_smooth2d.dir, filesep, fileArray{ii}.layers1_smooth2d.name, '.mat' ];
        load(layers);
        generate_output_files(fileIn, fileOut, stLayerBdrys);
        matFile = [ fileArray{ii}.mat.dir, filesep, fileArray{ii}.mat.name, '.mat' ];
        load(matFile, 'pixelDim');
        imageThickMap(fileArray{ii}.thicknessMaps.dir,...
            fileArray{ii}.thicknessStats.name,...
            fileArray{ii}.thicknessMaps.name,...
            [ fileOut, '_thickness_map.png' ],...
            pixelDim );
        % FIXME: Probably a better way to handle this then within this
        % function, but for now...
        [ score, performancePoint ] = computeDMEscore(fileArray{ii}.thicknessStats.name,...
            fileArray{ii}.thicknessStats.dir,...
            'trainingStatsHighQuality.mat',...
            'thicknessPerformanceSplines.mat');
        fd = fopen([ fileOut, '_results.json' ], 'w');
        fprintf(fd, '{"abnormalThickness":');
        json(performancePoint, fd);
        load([fileArray{ii}.cfarClusterFeatures.dir, filesep, fileArray{ii}.base.name, '_cfar_clusterFeatures.mat']);
        fprintf(fd, ',"hardExudates":{"pfa":%f,"pd":%f,"hardExudates":[', cpfa, cpd);
        numExudates = length(candidateHE);
        for j = 1:numExudates
            if (j > 1)
                fprintf(fd, ',');
            end
            json(candidateHE(j), fd);
        end
        fprintf(fd, ']}}');
        fclose(fd);
        saaFile = [ fileArray{ii}.SAA.dir, filesep, fileArray{ii}.SAA.name, '.mat' ];
        layerBdryFile = [ fileArray{ii}.layers1_smooth2d.dir, filesep, fileArray{ii}.layers1_smooth2d.name, '.mat' ];
        intLayerBdryFile = [ fileArray{ii}.intLayers_smooth2d.dir, filesep, fileArray{ii}.intLayers_smooth2d.name, '.mat' ];
        mapFile = [ fileArray{ii}.thicknessMaps.dir, filesep, fileArray{ii}.thicknessMaps.name, '.mat' ];
        createPsuedoFundusImages(saaFile, layerBdryFile, intLayerBdryFile, mapFile, [ fileOut, '_synthesized_fundus' ]);
    end;

end

