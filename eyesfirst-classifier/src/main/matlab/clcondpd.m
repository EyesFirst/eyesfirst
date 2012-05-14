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

function pobsgcl=clcondpd(dm,curclmean,curclcov);
% computes the probability of the observations dm (nbandsXnobs) for given normal class defined
% by mean curclmean and covariance curclcov

[nbds,nobs]=size(dm);
maxcondnum = 10;
[U,D,V]=svd(curclcov);
eigval=diag(D);
condnumv=eigval/eigval(1);
I1=find(log10(condnumv)>=-maxcondnum);
LI1=length(I1);
teigval=eigval;
if LI1<nbds
    teigval(LI1+1:nbds)=eigval(LI1+1)*ones(nbds-LI1,1);
end;
invteigval=teigval.^(-1);
acovinv=U*diag(invteigval)*U';
whitemat=diag(sqrt(invteigval))*U';
wdm=whitemat*(dm-repmat(curclmean,1,nobs));
if nbds > 1
   qf=sum(wdm.^2);
else
    qf = wdm.^2;
end;
srdetcm=sqrt(prod(teigval));
pobsgcl=exp(-qf/2)/(srdetcm*((2*pi)^(nbds/2)));


