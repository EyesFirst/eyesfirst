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

function pmscev = psfexpand(pmsc,psf)
% pmsc is an nxddim matrix of points in R^(ddim);
% psf is the radius of the point spread function modeled here as a cube
% pmsc is interpreted as the centers of point-spread function cubes; 
%on return pmscev contains the unique vertices of the cubes;
%
[aa,bb] = size(pmsc);
expf = 2^bb;
pmscev1 = zeros(aa*expf,bb);
expm2 = psf*[1 1;
            1 -1;
            -1 1;
            -1 -1];
expm3 = [psf*ones(4,1) expm2;-ones(4,1)*psf expm2];
if bb == 2
    expm = expm2;
elseif bb == 3
    expm = expm3;
else
    error('dimension must be 2 or 3\n');
end;
rpmsc = repmat(pmsc,[1,1,expf]);
expm = reshape(expm',[1,bb,expf]);
rexpm2 = repmat(expm,[aa,1,1]);
pmscev1 = rexpm2+rpmsc;
pmscev1 = shiftdim(pmscev1,1);
pmscev1 = reshape(pmscev1,bb,aa*expf)';
pmscev = unique(pmscev1,'rows');

    
            


end

