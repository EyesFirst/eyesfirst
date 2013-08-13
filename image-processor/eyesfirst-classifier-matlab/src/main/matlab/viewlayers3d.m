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

function handle = viewlayers3d(layers, varargin)
    % Display layer data using a 3D visualization.
    % Example:
    %   viewlayers3d(stLayerBdrys)
    %   viewlayers3d(stLayerBdrys, 0.25)

    % Optional arg defaults
    layer_id = 'oimwsla2d';
    scale = 0.125;
    if nargin >= 2
        if ~isempty(varargin{1}) && ischar(varargin{1})
            layer_id = varargin{1};
        end
    end
    if nargin >= 3
        if varargin{2} > 0
            scale = varargin{2};
        end
    end

    volume_data = convert_to_volume(layers, layer_id, scale);
    handle = vol3d('cdata', volume_data, 'texture', '3d');
    % Tweak the figure:
    view(3);
    % Make it a cube:
    vsize = size(volume_data);
    % Swap x any y (for ... some reason)
    t = vsize(1);
    vsize(1) = vsize(2);
    vsize(2) = t;
    daspect(vsize / min(vsize));
    vol3d(handle);
    % Set an alphamap to make it possible to see inside:
    alphamap('rampup');
    alphamap(.06 .* alphamap);
    % Show the camera toolbar
    cameratoolbar('Show');
    % Set the projection to "perspective"
    camproj('perspective');
    % Turn rotate 3d on
    rotate3d on;
end