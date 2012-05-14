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

function RSQ=quadraticSurfaceApprox(cp,Hval,x,y);

% cp = thicknessFeatures.cp;
% Hval = thickFeatures.Hessian;
cpx=cp(1);
cpy=cp(2);
% [aa,bb] = size(thicknessFeatures.maps.meanMapAve);
% NanBuf = 20;
% x = [1:aa]; % note that x is slow time
Lx = length(x);
%y = [NanBuf+1:bb-NanBuf];
Ly = length(y);
xc = x-cpx;
yc = y-cpy;
[X,Y]= meshgrid(xc,yc);
A = zeros(Ly,Lx,2);
A(:,:,1) = X;
A(:,:,2) = Y;
SA = shiftdim(A,2);
SA = reshape(SA,2,Lx*Ly);
% SAC = SA-[cpx;cpy];
Q = sum(SA.*((Hval/2)*SA));
RSQ = reshape(Q,Ly,Lx);
%center points
%cp = zeros(1,2);



end

