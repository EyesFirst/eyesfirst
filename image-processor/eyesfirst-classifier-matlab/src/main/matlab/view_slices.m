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

function view_slices(data_cube_in,x_index,y_index,z_index,fignum)
% view_slices(data_cube_in,x_index,y_index,z_index,fignum)
% data_cube_in(z,x,y)
figure(fignum),subplot(222),imagesc(squeeze(data_cube_in(z_index,:,:)).'),colormap(gray),colorbar
xlabel('X')
ylabel('Y')
title(['Slice at Z = ' num2str(z_index)])
figure(fignum),subplot(223),imagesc(squeeze(data_cube_in(:,x_index,:))),colormap(gray),colorbar
xlabel('Y')
ylabel('Z')
title(['Slice at X = ' num2str(x_index)])
figure(fignum),subplot(224),imagesc(squeeze(data_cube_in(:,:,y_index))),colormap(gray),colorbar
xlabel('X')
ylabel('Z')
title(['Slice at Y = ' num2str(y_index)])
figure(fignum),subplot(221),imagesc(squeeze(sum(data_cube_in,1)).'),colormap(gray),colorbar
xlabel('X')
ylabel('Y')
title('Sum for all Z')