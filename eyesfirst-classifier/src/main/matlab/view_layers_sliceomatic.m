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

function view_layers_sliceomatic(layers, layer_id)
    % view_layers_sliceomatic Display layer data using Sliceomatic.
    % view_layers_sliceomatic(layers, layer_id) Displays layer data in
    % layers of the id layer_id using <http://www.mathworks.com/matlabcentral/fileexchange/764 Sliceomatic>.
    % Example: view_layers_sliceomatic(stLayerBdrys, 'oimwsla2d')
    sliceomatic(convert_to_volume(layers, layer_id, 0.125));
end