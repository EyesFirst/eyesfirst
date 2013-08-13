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

function [ X, metadata ] = convert_pgms_to_dicom( pgm_dir, varargin )
%convert_pgms_to_dicom Convert a directory of PGM files to a DICOM file
%   Returns the same X that dicomread would produce, and a set of metadata
%   to give to dicomwrite.
%   To write the final DICOM, use a command like:
%   dicomwrite(X, 'dicom_file.dcm', 'SOPClassUID', '1.2.840.10008.5.1.4.1.1.77.1.5.4', 'SharedFunctionalGroupsSequence', metadata.SharedFunctionalGroupsSequence, 'CreateMode', 'copy');
%   It will warn you that 'SOPClassUID' is ignored in this case, but it's
%   wrong, it is not.
%   Note that this doesn't do a complete conversion to DICOM, quite a few
%   fields are missing. But it does enough to allow the EyesFirst
%   classifier to load the image and start processing it.

    p = inputParser;
    addRequired(p, 'pgm_dir', @ischar);
    addOptional(p, 'width', 6.0, @isnumeric);
    addOptional(p, 'height', 6.0, @isnumeric);
    addOptional(p, 'depth', 2.0, @isnumeric);
    parse(p, pgm_dir, varargin{:});
    widthMM = p.Results.width;
    heightMM = p.Results.height;
    depthMM = p.Results.depth;
    % Grab all the PGMs in that directory
    files = dir(pgm_dir);
    layerFiles = cell(1, 0);
    for idx = 1:length(files)
        name = files(idx).name;
        if length(name) > 4 && strcmp(name(end-3:end), '.pgm')
            nidx = length(layerFiles) + 1;
            layerFiles(nidx) = cellstr(name);
        end
    end
    if length(layerFiles) < 1
        error('convert_pgms_to_dicom:no_files', 'Unable to locate any files');
    end
    % Sort so that they're in order...
    layerFiles = sort(layerFiles);
    % Grab the first layer - all layers must be the same size.
    layer = imread(char(strcat(pgm_dir, filesep, layerFiles(1))));
    width = size(layer, 2);
    height = size(layer, 1);
    % And create the cube (which is really 4-D because the 3rd index would
    % be the color values, but this is grayscale):
    X = zeros(height, width, 1, length(layerFiles), class(layer));
    fprintf('Found %d files, %dx%d, using physical size of %fmm x %fmm x %fmm.\n', length(layerFiles), width, height, widthMM, heightMM, depthMM);
    % Insert the first layer to the cube:
    X(:,:,1,1) = layer;
    % And start building a cube.
    for idx = 2:length(layerFiles)
        % TODO: Check that the read file matches
        X(:,:,1,idx) = imread(char(strcat(pgm_dir, filesep, layerFiles(idx))));
    end
    % Now that we have the cube, we can convert to DICOM. First off,
    % create our metadata
    pms = struct();
    pms.SliceThickness = heightMM / length(layerFiles);
    pms.PixelSpacing = [ depthMM / height ; widthMM / width ];
    metadata = struct();
    metadata.SharedFunctionalGroupsSequence = struct();
    metadata.SharedFunctionalGroupsSequence.Item_1 = struct();
    metadata.SharedFunctionalGroupsSequence.Item_1.PixelMeasuresSequence = struct();
    metadata.SharedFunctionalGroupsSequence.Item_1.PixelMeasuresSequence.Item_1 = pms;
end
