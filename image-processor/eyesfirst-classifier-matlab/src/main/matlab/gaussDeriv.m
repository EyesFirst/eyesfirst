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

function [xv,gd] = gaussDeriv(order,stdev,extent,plotFlag)
% gd is the Gaussian derivative calculated from
% [-stdev*extent,stdev*extent]; of order order.
% 
ss = 1;
% xv = [-extent*stdev:ss:extent*stdev];
xv = [-extent:ss:extent];
nxv = xv/(stdev*sqrt(2));
gf = exp(-(nxv.^2))/(stdev*sqrt(2*pi));
hf = hermite(nxv,order,0);
gd = ((-1/(stdev*sqrt(2)))^order)*(gf.*hf);
% gd = ((-1/(stdev*sqrt(2)))^order)*(hf);
if plotFlag == 1
    figure
    plot(xv,gd)
end;