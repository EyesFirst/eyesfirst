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

function [ ] = save_thickness_map( map, filename )
%SAVE_THICKNESS_MAP Saves a thickness map as an image file
%   Saves a thicknessmap as an image file.
%   map - the actual map to save
%   filename - the name of the file to save

    % We need to normalize the map.
    max_value = max(max(map));
    map = map / max_value;
    imwrite(map, filename);

end

