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

function [nxv,nyv,I0] = removeOutLier_nf(xv,yv,pfd,signFlag,Nstd,errTolType)
% function [roxv,royv] = removeOutLier(xv,yv,ep,signFlag)
% remove outliers from xv
% repeat until the fit is stable
estErrTol = 5;
maxNumIter = 2;
cxv = xv;
cyv = yv;
cp = polyfit(cxv,cyv,pfd);
pcyv = polyval(cp,cxv);
I0 = 1;
numIter = 0;
estErr = 2*estErrTol;
while estErr > estErrTol && numIter < maxNumIter
    numIter = numIter + 1;
    stde = sqrt(mean((pcyv-cyv).^2));
    if signFlag == 1 % remove outlier that exceed pyv by more than the tolerance
        dv = cyv-pcyv;
    elseif signFlag == -1 % remove outlier that are below pyv by more than the tolerance
        dv = pcyv-cyv;
    else
        dv = abs(pcyv-cyv);
    end;
    I0 = find(dv >= stde*Nstd);
    if ~isempty(I0)
        I1 = find(dv < stde*Nstd);
    else
        [maxdv,I0] = max(dv);
        I1 = find(dv < maxdv);
    end;
    nxv = cxv(I1);
    nyv = cyv(I1);
    np = polyfit(nxv,nyv,pfd);
    pnyv = polyval(np,nxv);
    if errTolType == 2
        delxv = nxv(2:end)-nxv(1:end-1);
        delyv = (pnyv-pcyv(I1)).^2;
        delyvave = (delyv(2:end)+delyv(1:end-1))/2;
        estErr = sqrt(sum(delyvave.*delxv));
    elseif errTolType == 1
        estErr = max(abs(pnyv-pcyv(I1)));
    end
%     figure(figh);
%     plot(cxv,pcyv);
%     hold on
%     plot(cxv(I0),cyv(I0),'r*');
%     plot(nxv,pnyv);
    cxv = nxv;
    cyv = nyv;
    pcyv = pnyv;
end

