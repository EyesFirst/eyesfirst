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

function bw_VEF = gen_fundus(fundusFile)



rgb_fundus =imread(fundusFile,'jpeg');
 
bw_VEF = double(rgb_fundus(:,:,2));
mask = bw_VEF > 0;
max_val = max(bw_VEF(:));
bw_VEF = (max_val - bw_VEF).*mask;

%bw_VEF = imadjust(bw_VEF);
 
figure(1),imagesc(bw_VEF),colormap(gray),colorbar,axis image,hold on
