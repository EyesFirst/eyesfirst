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

function volume = convert_to_volume(layers, layer_id, varargin)
    % Attempts to convert the given layers to a 3D volume matrix, such that
    % they may be viewed via one of the MATLAB visualizers.

    p = inputParser;
    
    p.addRequired('layers');
    p.addRequired('layer_id', @ischar);
    p.addOptional('scale', 1.0, @(x) isnumeric(x) && x > 0);
    p.addParamValue('WaitBar', 0, @ishandle);
    p.addParamValue('WaitStart', 0.0, @(x) isnumeric(x) && x > 0 && x <= 1.0);
    p.addParamValue('WaitTotal', 1.0, @(x) isnumeric(x) && x > 0 && x <= 1.0);

    p.parse(layers, layer_id, varargin{:});

    num_layers = size(layers);
    num_layers = num_layers(1);

    if num_layers < 1
        % This is probaly the wrong thing to do, but, whatever.
        volume = zeros(0,0,0);
        return
    end

    % Set defaults
    scale = p.Results.scale;
    wbh = p.Results.WaitBar;
    wb_start = p.Results.WaitStart;
    wb_total = p.Results.WaitTotal;

    % Look into the first layer to get the image size
    layer_size = size(layers{1}.(layer_id));

    width = ceil(layer_size(1) * scale);
    height = ceil(layer_size(2) * scale);

    fprintf('Creating volume with %d layers sized %dx%d (based on first layer).\n', num_layers, width, height);

    % Generate the volume, first creating a matrix

    volume = zeros(num_layers, width, height, 'uint8');

    fprintf('Copying layers...         ');

    for layer = 1:num_layers
        fprintf('\b\b\b\b\b\b\b\b\b% 4d/% 4d', layer, num_layers);
        image = uint8(layers{layer}.(layer_id));
        if scale ~= 1.0
            image = imresize(image, scale);
        end
        image_size = size(image);
        if (image_size(1) ~= width) || (image_size(2) ~= height)
            error('Image size mismatch at layer %d (expected %dx%d, got %dx%d)', layer, width, height, image_size(1), image_size(2));
        end
        volume(layer,:,:) = image;
        if wbh
            waitbar((layer/num_layers) * wb_total * 0.75 + wb_start, wbh);
        end
    end
    
    fprintf('\b\b\b\b\b\b\b\b\b done.\nPermuting layers...');

    volume = permute(volume, [3 2 1]);    

    if wbh
        waitbar(wb_total + wb_start, wbh);
    end

    fprintf(' done.\n');
end