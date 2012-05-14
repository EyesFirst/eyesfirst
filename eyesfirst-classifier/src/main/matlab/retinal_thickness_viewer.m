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

% Given a data directory, displays a list of all data that can be displayed
% and then opens it via retinal_thickness_viewer.

function RTV = retinal_thickness_viewer(data_dir)
    if nargin == 0
        % FIXME: Load defaults from a single file
        data_dir = 'C:\EyesFirst\data';
    end
    RTV = RetinalThicknessViewer(data_dir);
end
