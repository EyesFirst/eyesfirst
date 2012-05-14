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

function [ ] = generate_output_files( original_file, out_prefix, layer_boundary_data, thickness_map )
%generate_output_files Generates output files.
%   Generates the various output files.
%
%   original_file - the path to the original DCM file, necessary for
%      copying existing metadata to the new file.
%   out_prefix - the output prefix (including the path) to use for
%      generating names.
%   layer_boundary_data - the layer boundary data used to create the
%      imagery for the new file.
%   thickness_map - the thickness map to generate

    metadata = dicominfo(original_file);
    save_dicom(layer_boundary_data, [ out_prefix, '_processed.dcm' ], metadata);
    save_thickness_map(thickness_map, [ out_prefix, '_synthesized_fundus.png' ]);

end

