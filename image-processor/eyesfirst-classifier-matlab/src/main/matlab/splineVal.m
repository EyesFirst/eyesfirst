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

function [fval,gval,shval] = splineVal(x,sf,sg,sh)
% x is a vector of positions; sf is a spline, sg is the spline gradient of
% sf and sh is the spline hessian of sf.
[m,k] = size(x);
fval = fnval(sf,x);
gval = fnval(sg,x);
shval = reshape(fnval(sh,x),[m,m,k]);

end

