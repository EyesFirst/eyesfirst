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

function [cf,cfeq,gf,gfeq] = constFunc(aa,xv,yv,boundFlag)
cfeq = [];
gfeq = [];
pv = polyval(aa,xv);
if boundFlag == 1 % upperbound
    cf = yv-pv;
    gf = repmat(xv',length(aa),1);
    for ii = 1:length(aa);
        gf(ii,:) = -(gf(ii,:).^(length(aa)-ii));
    end;
elseif boundFlag == -1 % lower bound
    cf = pv-yv;
        gf = repmat(xv',length(aa),1);
    for ii = 1:length(aa);
        gf(ii,:) = gf(ii,:).^(length(aa)-ii);
    end;
end;
