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

function cclp=clconddist(dm,curcl,curclpd);
% function cclp=clconddist(dm,curcl,curclpd);
% computes the conditional distribution of each class for each datum
%
ncl =length(curclpd);
[nbds,nobs]=size(dm);
condlikemat=zeros(nobs,ncl);
for ii =1:ncl
    curclmean=curcl{ii}.mean;
    curclcovm=curcl{ii}.covm;
    pobsgcl=clcondpd(dm,curclmean,curclcovm);
    condlikemat(:,ii)=pobsgcl';
end;
prodprobmat=condlikemat.*repmat(curclpd',nobs,1);
pobs=sum(prodprobmat,2);
cclp=prodprobmat./(repmat(pobs,1,ncl));
ISN=isnan(cclp);
nanrow=max(ISN,[],2);
INR=find(nanrow==1);
LINR=length(INR);
cclp(INR,:)=(1/ncl)*ones(LINR,ncl);
