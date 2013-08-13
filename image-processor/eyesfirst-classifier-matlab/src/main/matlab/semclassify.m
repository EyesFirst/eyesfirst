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

function [clstat,clpd,cclmap,ppmclf,maclmap,stclest,maclIndMap]=semclassify(dm,dmwt,iclstat,iclpd,maxnumit);
% function [clstat,clmap,ppcclf]=semclassify(dm,dmwt,iclstat,maxnumit);
% input  dm: data matrix
%        dmwt: data matrix with targets
%        iclstat:  initial class statistics
%        iclpd:    initial class probability distribution
%        maxnumit;  maximum number of iterations used by the sem update equations
% output  clstat:  sem estimate of class statistics
%         clmap:   sem class map
%         ppcclf:  pure pixel class conditional likelihood function
%         maclmap:  maximum (aposteriori ?)  class map
[semstat,sempd,stclest]=semestimate(dm,iclstat,iclpd,maxnumit);
[ppmclf,cclmap]=purepixmclf(dmwt,semstat,sempd);
[maclmap,maclIndMap]=max(cclmap,[],2);
clstat=semstat;
clpd=sempd;
% semclmap=clconddist(simdmat,semstat,sempd);
% gtclmap=clconddist(simdmat,clstat,clpd);

