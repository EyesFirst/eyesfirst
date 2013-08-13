% Copyright 2013 The MITRE Corporation
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

function [] = export_candidate_hes_json( candidateHE, varargin )
%export_candidate_hes Export candidate hard exudates as JSON.
%   Detailed explanation goes here

    length = size(candidateHE, 2);
    closeOnExit = 0;
    if (nargin > 1)
        fileID = varargin(1);
        if iscellstr(fileID)
            fileID = char(fileID);
        end
        if ischar(fileID)
            closeOnExit = 1;
            fileID = fopen(fileID, 'w');
        end
    else
        fileID = 1;
    end
    
    fprintf(fileID, '{"hardExudates":\n[\n');
    for index = 1:length
        if index > 1
            fprintf(fileID, ',\n');
        end
        fprintf(fileID, '{\n"maxCfarValue":%f,\n', candidateHE(index).maxCfarValue);
        fprintf(fileID, '"normalScore":%f,\n', candidateHE(index).normalScore);
        fprintf(fileID, '"layer":%f,\n', candidateHE(index).layer);
        % Center and radius are empty matrixes in the sample I have, so
        % skip them for now.
        %fprintf(fileID, '"center":%d,\n', candidateHE(index).center);
        %fprintf(fileID, '"radius":%d,\n', candidateHE(index).radius);
        fprintf(fileID, '"numVoxels":%f,\n', candidateHE(index).numVoxels);
        writejsonarray(fileID, 'boundingBoxMinCorner', candidateHE(index).boundingBoxMinCorner);
        fprintf(fileID, ',\n');
        writejsonarray(fileID, 'boundingBoxWidth', candidateHE(index).boundingBoxWidth);
        fprintf(fileID, ',\n"layerProportion":%f,\n', candidateHE(index).layerProportion);
        writejsonarray(fileID, 'ellipseCenter', candidateHE(index).ellipseCenter);
        fprintf(fileID, '\n}');
    end

    fprintf(fileID, '\n]\n}\n');
    if closeOnExit
        fclose(fileID);
    end
end

function writejsonarray(fileID, field, value)
    fprintf(fileID, '"%s":[', field);
    len = size(value);
    for index = 1:len
        if (index > 1)
            fprintf(fileID, ',');
        end
        fprintf(fileID, '%d', value(index));
    end
    fprintf(fileID, ']');
end