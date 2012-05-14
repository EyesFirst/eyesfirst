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

function fim = gauskerndiff(im,n1,n2,n3,sd1,sd2,sd3,extent1,extent2,extent3)
% difim is the derivative image of im computed using a separable Gaussian
% kernel of order n1, n2, and n3 and standard deviations sd1, sd2, and sd3 in dimensions 1, 2, and 3,
% respectively.
%
% make filters
%extent = 10; % pixel units
plotFlag = 1;

[aa,bb,cc] = size(im);
if ~isempty(n1)
    im = reshape(im,aa,bb*cc);
    [xv1,fc1] = gaussDeriv(n1,sd1,extent1,plotFlag);
    fim = sepConv(im,fc1);
    fim = reshape(fim,[aa,bb,cc]);
else
    fim = im;
end;
fim = shiftdim(fim,1);
if ~isempty(n2)
   [xv2,fc2] = gaussDeriv(n2,sd2,extent2,plotFlag);
   fim = reshape(fim,bb,aa*cc);
   fim = sepConv(fim,fc2);
   fim = reshape(fim,[bb,cc,aa]);
end;
fim = shiftdim(fim,1);
if ~isempty(n3)
    [xv3,fc3] = gaussDeriv(n3,sd3,extent3,plotFlag);
    fim = reshape(fim,cc,aa*bb);
    fim = sepConv(fim,fc3);
    fim = reshape(fim,[cc,aa,bb]);
end;
fim = shiftdim(fim,1);

