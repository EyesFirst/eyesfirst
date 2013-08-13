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

function fim = sepConv(im,f)
% im is a 2-d matrix and f is a filter
% on return, fim is the convolution of columns of im with the filter f
% such that incompletly filtered values are zero
% assumes that length(f) = 2*r+1; 

[aa,bb] = size(im);
nf = length(f);
r = (nf-1)/2;
zpim = [im;zeros(nf,bb)];
Y = conv(zpim(:),f);
fim = NaN*ones(aa,bb);
Y = reshape(Y(1:(aa+nf)*bb),aa+nf,bb);
Z = Y(1:aa,1:bb);
fim(r+1:aa-r,:) = Z(nf:aa,:);
  