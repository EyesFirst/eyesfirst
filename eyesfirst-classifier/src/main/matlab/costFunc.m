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

function [cc,gg] = costFunc(aa,xv,yv,costFuncFlag,C0)
% costFunc(aa,xv,yv,costFuncFlag,C0)
if costFuncFlag == 2
   cc = sum((polyval(aa,xv)-yv).^2);
   dv = 2*(polyval(aa,xv)-yv);
   pmat = repmat(xv,1,length(aa));
  for ii = 1:length(aa)
      pmat(:,ii) = pmat(:,ii).^(length(aa)-ii);
  end;
   gg = dv'*pmat;
elseif costFuncFlag == 1
   cc = sum(abs(polyval(aa,xv)-yv));
elseif costFuncFlag == 0
    cc = sum(log(1+ abs(polyval(aa,xv)-yv)));
elseif costFuncFlag < 0
    cc = sum( 1./(C0+(polyval(aa,xv)-yv).^(costFuncFlag)));
elseif costFuncFlag == 3
    cc1 = sum((polyval(aa,xv)-yv).^2);
    w1 = exp(-cc1);
    cc = sum(w1.*cc1);% sum((2./(1+exp(-((polyval(aa,xv)-yv).^2))))-1);
else
    error('costFuncFlag should be 3, 1, 0, or < 0\n');
end;