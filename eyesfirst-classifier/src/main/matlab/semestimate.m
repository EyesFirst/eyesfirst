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

function [semcl,sempd,stcl]=semestimate(dm,initcl,initcpd,maxnumit);
% function semcl=semestimate(dm,initcl);
% applies the SEM algorithm to the data set dm (nbandsXnobs) fitting a Gaussian
% mixture model  to the data.  initcl is the structure of initial classes, initcpd is the
% vector(numclX1) of the initial class probability distribution.
curcl=initcl;
curclpd=initcpd;
numcl=length(initcpd);
if numcl == 1
    stcl = zeros(maxnumit,4);
end
for ii = 1:maxnumit
    cclp=clconddist(dm,curcl,curclpd);
    ccla=classign(cclp);
    [upcl,uppd]=semupdate(ccla,dm,numcl);
    nupclass = length(upcl);
    if nupclass < numcl
        for jj = nupclass+1:numcl
           upcl{jj}.mean = upcl{nupclass}.mean;
           upcl{jj}.covm = upcl{nupclass}.covm;
        end;
        uppd = [uppd; zeros(numcl-nupclass,1)];
    end
    curcl=upcl;
    curclpd=uppd;
    stcl(ii,:) = [curcl{1}.mean curcl{1}.covm curcl{2}.mean curcl{2}.covm];
end;
semcl=curcl;
sempd=curclpd;




