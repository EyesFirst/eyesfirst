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

function [ ] = save_raw_binary( filename, A )
%UNTITLED2 Summary of this function goes here
%   Detailed explanation goes here
    fileID = fopen(filename, 'w', 'b');
    sizeA = size(A);
    fwrite(fileID, sizeA(2), 'uint');
    fwrite(fileID, sizeA(1), 'uint');
    fwrite(fileID, sizeA(3), 'uint');
    for slice = 1:sizeA(3)
        for row = 1:sizeA(1)
            fwrite(fileID,squeeze(A(row,:,slice)), 'uint8');
        end
    end
    fclose(fileID);
end

