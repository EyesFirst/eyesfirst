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

function intLayerBdrys_smooth2d_tr = trimInternalLayers(curCompIntLayerFile)
% function trimInternalLayers(curCompIntLayerFile)
outFile = [curCompIntLayerFile,'_trim'];
load(curCompIntLayerFile)
Nslices = length(intLayerBdrys_smooth2d);
intLayerBdrys_smooth2d_tr = cell(Nslices,1);
for ii = 1:Nslices
    intLayerBdrys_smooth2d_tr{ii}.top = intLayerBdrys_smooth2d{ii}.top;
    intLayerBdrys_smooth2d_tr{ii}.floor = intLayerBdrys_smooth2d{ii}.floor;
    intLayerBdrys_smooth2d_tr{ii}.curxaug = intLayerBdrys_smooth2d{ii}.curxaug;
    intLayerBdrys_smooth2d_tr{ii}.smooth2dInterMediateBdrys = intLayerBdrys_smooth2d{ii}.smooth2dInterMediateBdrys;
end
% save(outfile,'intLayerBdrys_smooth2d_tr');

