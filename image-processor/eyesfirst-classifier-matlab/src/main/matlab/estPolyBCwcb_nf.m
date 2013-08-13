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

function [x,fval,exitflag,output] = estPolyBCwcb_nf(xv,yv,pdeg,boundFlag,costFuncFlag,cLB,cUB)
% function x = estPolyBC(xv,yv,pdeg,boundFlag,costFuncFlag)
% estimates the best fitting polynomial of degree pdeg.  If boundFlag == 0
% then no constraint is imposed on the fit.  If boundFlag = -1 then pp(xv)
% <= yv, and if boundFlag == 1, then pp(xv) >= yv.  
% costFuncFlag should be 2, 1, 0, or -2: flagvalues correspond to
% 2: mean squared error
% 1: L1 norm
% 0: sum(log(1+abs error))
% -2: sum( 1./(C0+(polyval(aa,xv)-yv).^(-2));
% if costfuncFlag == n the derivative of the cost function is 1/abs(pp(xv)-yv)^n
% xv = [-1 0 1 2]';
% yv = [1 0 1 3]';
% pdeg = 2;
% boundFlag = -1;
% costFuncFlag = 2;
ss = .1;
% costFuncFlag = 1;
C0 = 0;
options = optimset('fmincon');
options = optimset(options,'algorithm','interior-point');
options = optimset(options,'gradobj','on');
options = optimset(options,'derivativeCheck','off');
options = optimset(options,'diagnostics','off');
options = optimset(options,'gradConstr','on');
p0 = polyfit(xv,yv,pdeg);
f = @(x)costFunc(x,xv,yv,costFuncFlag,C0);
%[ddd,ggg] = costFunc(p0,xv,yv,costFuncFlag,C0);
%feval(f,p0);
g = @(x)constFunc(x,xv,yv,boundFlag);
%feval(g,p0);
% lb = -Inf;
% ub = Inf;
[x,fval,exitflag,output]= fmincon(f,p0,[],[],[],[],cLB,cUB,g,options);
% figure(figh);
% plotsym = [plotcolor,'*'];
% plot(xv,yv,plotsym);
% hold on;
% ssxv = [xv(1):ss:xv(end)];
% fv = polyval(x,ssxv);
% plot(ssxv,fv,'k');