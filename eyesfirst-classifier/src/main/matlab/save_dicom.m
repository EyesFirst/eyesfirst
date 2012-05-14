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

function save_dicom(stLayerBdrys, file, varargin)
% Attempts to save the image as a Dicom image.
% stLayerBdrys - the image to save
% file - the name of the file to save into
% metadata - metadata to use (optional)

% Example:
% load('C:\EyesFirst\data\storeLayerBdrys\Normal_Sample_Layers1_smooth2d.mat');
% save_dicom(stLayerBdrys, 'C:\EyesFirst\data\test.dcm');

wbh = waitbar(0, 'Saving DICOM image...');

try
    % Default fields to set in the metainfo
    metainfo_defaults = struct();
    % Default options
    option_defaults = struct('CompressionMode', 'JPEG2000 lossless', 'CreateMode', 'copy');

    % Fields that should NOT be used from any info passed in (will be deleted)
    metainfo_do_not_use = { 'Width', 'Height', 'FileSize', 'Format',...
        'FormatVersion', 'NumberOfFrames', 'BitDepth', 'ColorType' };
    % Use the defaults at first...
    metainfo = metainfo_defaults;

    if length(varargin) > 1
        error('Too many arguments.');
    elseif length(varargin) == 1
        % Grab the metainfo if it's present
        metainfo = varargin{1};
        if ~isstruct(metainfo)
            error('Metainfo must be a struct');
        end
        % Otherwise, iterate through the defaults and apply any missing ones
        % to them.
        names = fieldnames(metainfo_defaults);
        for I = 1:size(names)
            field = names{I};
            fprintf('Checking field %s\n', field);
            if ~isfield(metainfo, field)
                % Set the field
                fprintf('Setting field %s to %s\n', field, metainfo_defaults.(field));
                metainfo.(field) = metainfo_defaults.(field);
            end
        end
        % Remove fields that are not supposed to be copied
        metainfo = rmfield(metainfo, metainfo_do_not_use);
    end

    %fprintf('Using the following metainfo:\n');
    %disp(metainfo);

    layers = size(stLayerBdrys);
    layers = layers(1);

    if layers < 1
        error('No layers to save!')
    end

    % Look into the first layer to get the image size
    layer_size = size(stLayerBdrys{1}.oimwsla2d);

    % TODO: If we're given an original size (via the metainfo) we should crop
    % the layers to that size.

    fprintf('Saving %d layers sized %dx%d (based on first).\n', layers, layer_size(1), layer_size(2));

    if layers == 1
        % This is simple, just write it immediately
        fprintf('Immediately writing single layer.\n');
        waitbar(0.5, wbh, 'Writing single layer.');
        dicomwrite(uint8(stLayerBdrys{1}.oimwsla2d), file, metainfo);
        return
    end

    % Generate the imagery, first creating a matrix

    temp_image = zeros(layers, 1, layer_size(1), layer_size(2), 'uint8');

    fprintf('Creating layers...     ');

    for layer = 1:layers
        fprintf('\b\b\b\b% 4d', layer);
        waitbar(0.1 + (0.6 * (layer / layers)), wbh, sprintf('Saving DICOM image - Creating layer %d...', layer));
        temp_image(layer,1,:,:) = uint8(stLayerBdrys{layer}.oimwsla2d);
    end

    fprintf('\b\b\b\bdone.\nPermuting images to the form dicomwrite wants...\n');
    waitbar(0.75, wbh, 'Saving DICOM image - Permuting layers...');
    temp_image = permute(temp_image, [3 4 2 1]);
    fprintf('Writing file dicom file "%s".\n', file);

    waitbar(0.8, wbh, 'Saving DICOM image - Writing image...');
    dicomwrite(temp_image, file, option_defaults, metainfo);

    fprintf('Done!\n');
    delete(wbh);
catch err
    delete(wbh);
    rethrow(err);
end

end