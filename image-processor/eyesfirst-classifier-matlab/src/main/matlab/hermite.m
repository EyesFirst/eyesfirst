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

function hv = hermite(domain,degree,plotFlag)
% returns the value of the hermite polynomial of degree degree on domain
% domain.

if degree == 0
    hp = [1];
elseif degree == 1
    hp = [2 0];
elseif degree == 2
    hp = [4 0 -2];
elseif degree == 3
    hp = [8 0 -12 0];
elseif degree == 4
    hp = [16 0 -48 0 12];
elseif degree == 5
    hp = [32 0 -160 0 120 0];
elseif degree == 6
    hp = [64 0 -480 0 720 0 -120];
elseif degree == 7
    hp = [128 0 -1344 0 3360 0 -1680 0];
else
    error('degree must be a non negative integer less than or equal to 7\n');
end;
hv = polyval(hp,domain);
if plotFlag == 1
    figure;
    plot(domain,hv);
    title(['Hermite ploynomial of degree ',int2str(degree)]);
end;
        
        