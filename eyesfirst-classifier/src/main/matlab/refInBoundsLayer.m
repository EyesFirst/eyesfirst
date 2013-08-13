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

function [refSet1,refSet2,refSet3,refSet4] = refInBoundsLayer(ii,jj,kk,refPar,Bdrys)
% Bdrys is a structure such that the retina is confined to  Bdrys.top and
% Bdrys.floor.  The CFAR structure maintains the same proportion between
% top and floor as the test pixel
%

delI = (refPar.TWi-1)/2;
I1 = (ii-delI):(ii+delI);
cfloor = floor(Bdrys.floor(jj,kk));
ctop = ceil(Bdrys.top(jj,kk));
Ifrac = [(I1(1)-cfloor)/(ctop-cfloor) (I1(end)-cfloor)/(ctop-cfloor)];
if Ifrac(1) >= 0 && Ifrac(2) <= 1
    delK = (refPar.TWk-1)/2;
    K1 = kk-refPar.TWk-refPar.gapk:kk+refPar.TWk+refPar.gapk;
    K2 = kk-refPar.TWk-refPar.gapk:kk-refPar.gapk-1;
    K3 = kk+refPar.gapk+1:kk+refPar.TWk+refPar.gapk;
    delJ = (refPar.TWj-1)/2;
    J1 = jj-refPar.TWj-refPar.gapj:jj-1-refPar.gapj;
    J2 = jj-refPar.gapj:jj+refPar.gapj;
    J3 = jj+refPar.gapj+1:jj+refPar.TWj+refPar.gapj;
    if J1(1) > 0 && J3(end) <= refPar.sizeA(2)   && K1(1) > 0 && K1(end) <= refPar.sizeA(3)
        kmat1 = kron((1-Ifrac),floor(Bdrys.floor(J1,K1))) + kron(Ifrac,ceil(Bdrys.top(J1,K1)));
        R1Iind = reshape(kmat1,length(J1),length(K1),length(Ifrac));
        R1Ind(:,:,1) = max(floor(R1Iind(:,:,1)),floor(Bdrys.floor(J1,K1)));
        R1Ind(:,:,2) = min(ceil(R1Iind(:,:,2)),ceil(Bdrys.top(J1,K1)));
        %
        kmat2 = kron((1-Ifrac),floor(Bdrys.floor(J2,K3))) + kron(Ifrac,ceil(Bdrys.top(J2,K3)));
        R2Iind = reshape(kmat2,length(J2),length(K3),length(Ifrac));
        R2Ind(:,:,1) = max(floor(R2Iind(:,:,1)),floor(Bdrys.floor(J2,K3)));
        R2Ind(:,:,2) = min(ceil(R2Iind(:,:,2)),ceil(Bdrys.top(J2,K3)));
        %
        kmat3 = kron((1-Ifrac),floor(Bdrys.floor(J3,K1))) + kron(Ifrac,ceil(Bdrys.top(J3,K1)));
        R3Iind = reshape(kmat3,length(J3),length(K1),length(Ifrac));
        R3Ind(:,:,1) = max(floor(R3Iind(:,:,1)),floor(Bdrys.floor(J3,K1)));
        R3Ind(:,:,2) = min(ceil(R3Iind(:,:,2)),ceil(Bdrys.top(J3,K1)));
        %
        kmat4 = kron((1-Ifrac),floor(Bdrys.floor(J2,K2))) + kron(Ifrac,ceil(Bdrys.top(J2,K2)));
        R4Iind = reshape(kmat4,length(J2),length(K2),length(Ifrac));
        R4Ind(:,:,1) = max(floor(R4Iind(:,:,1)),floor(Bdrys.floor(J2,K2)));
        R4Ind(:,:,2) = min(ceil(R4Iind(:,:,2)),ceil(Bdrys.top(J2,K2)));
        refSet1.I = R1Ind;
        refSet1.J = J1;
        refSet1.K = K1;
        refSet2.I = R2Ind;
        refSet2.J = J2;
        refSet2.K = K3;
        refSet3.I = R3Ind;
        refSet3.J = J3;
        refSet3.K = K1;
        refSet4.I = R4Ind;
        refSet4.J = J2;
        refSet4.K = K2;
    else
        refSet1 = [];
        refSet2 = [];
        refSet3 = [];
        refSet4 = [];
    end
else
    refSet1 = [];
    refSet2 = [];
    refSet3 = [];
    refSet4 = [];
end;

    
