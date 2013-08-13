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

function [upcl,uppd]=semupdate(ccla,dm,numcl);
% function [upcl,uppd]=semupdate(ccla,dm);
% ccla is the vector of current class assignments for each datum
% dm is the (nbdsXnobs) matrix of observations
% numcl is the number of classes
[nbds,nobs]=size(dm);
clct = 0;
multfac = 10;
muppd=zeros(numcl,1);
for ii = 1:numcl
    I1=find(ccla == ii);
    if length(I1)>multfac*nbds;
        clct = clct+1;
        cldm=dm(:,I1);
        upcl{clct}.mean=mean(cldm,2);
        upcl{clct}.covm=cov(cldm');
        muppd(clct)=length(I1)/nobs;
    end;
end;
uppd=muppd(1:clct);
        
        
        
