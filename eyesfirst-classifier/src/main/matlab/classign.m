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

function ccla=classign(cclp);
%ccla=classign(dm,cclp); determines class assignment based on class conditional distribution
% dm is the nbandsXnobs matrix of observations
% cclp is the nobs Xncl matrix of class conditional probabilities
[nobs,ncl]=size(cclp);
randv=rand(nobs,1);
ccld=cumsum(cclp,2);
ccld=ccld-repmat(randv,1,ncl);
ccla=zeros(nobs,1);
for ii =1:nobs
    I1=find(ccld(ii,:) >=0);
    ccla(ii)=I1(1);
end;
