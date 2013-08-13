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

function pdm = pixelDensityMap(clmat,hdim,vdim,opat,thresh)
%opat = 'CC';
[aa,bb] = size(clmat);
pdm = zeros(aa,bb);
N1sMat = zeros(aa,bb);
if rem(hdim,2) == 0
    hdim = hdim+1;
end
if rem(vdim,2) == 0
    vdim = vdim+1;
end;
% for ii= 1:aa
%     if strcmpi(opat,'UC')
%        minI = ii;
%        maxI = min(ii+hdim-1,aa);
%     elseif strcmpi(opat,'CC')
%         minI = max(ii-floor((hdim-1)/2),1);
%         maxI = min(ii+ceil((hdim-1)/2),aa);
%     end;
%     I = minI:maxI;
%     for jj = 1:bb
%         minJ = max(jj-floor((vdim-1)/2),1);
%         maxJ = min(jj+ceil((vdim-1)/2),bb);
%         J = minJ:maxJ;
%         samp = clmat(I,J);
%         N1s = sum(sum(samp));
%         frac1 = N1s/(length(I)*length(J));
%         N1sMat(ii,jj) = N1s;
%         if frac1 >= thresh
%            pdm(ii,jj) = frac1;
%         else
%             pdm(ii,jj) = 0;
%         end;
%     end;
% end;
% a loop free version
% create the numerator matrix
bufMat = zeros((aa+hdim-1),(bb+vdim-1));
M1 = bufMat;
I1 = 1:aa;
J1 = 1:bb;
M1(I1,J1) = clmat;
M2 = bufMat;
I2 = hdim:(aa+hdim-1);
J2 = 1:bb;
M2(I2,J2) = clmat;
M3 = bufMat;
I3 = 1:aa;
J3 = vdim:(bb+vdim-1);
M3(I3,J3) = clmat;
M4 = bufMat;
I4 = hdim:(hdim+aa-1);
J4 = vdim:(bb+vdim-1);
M4(I4,J4) = clmat;
csM1_R = cumsum(M1,2);
%csM1_C = cumsum(M1,1);
csM1 = cumsum(csM1_R,1);
csM2_R = cumsum(M2,2);
%csM2_C = cumsum(M2,1);
csM2 = cumsum(csM2_R,1);
csM3_R = cumsum(M3,2);
csM3_C = cumsum(M3,1);
csM3 = cumsum(csM3_R,1);
csM4_R = cumsum(M4,2);
csM4_C = cumsum(M4,1);
csM4 = cumsum(csM4_R,1);
N2sMatB = (csM1-csM2-csM3+csM4) +(csM2_R - csM4_R) + (csM3_C-csM4_C) + M4; % area term, row term, column term, pixel term
% create the denominator.  Note that the numerator only depends upon the
% slice dimensions, so this step can be done once and passed in as a
% parameter.  Requires a bit of recoding though.
denomBase = ones(aa,bb);
bufMat = zeros((aa+hdim-1),(bb+vdim-1));
D1 = bufMat;
I1 = 1:aa;
J1 = 1:bb;
D1(I1,J1) = denomBase;
D2 = bufMat;
I2 = hdim:(aa+hdim-1);
J2 = 1:bb;
D2(I2,J2) = denomBase;
D3 = bufMat;
I3 = 1:aa;
J3 = vdim:(bb+vdim-1);
D3(I3,J3) = denomBase;
D4 = bufMat;
I4 = hdim:(hdim+aa-1);
J4 = vdim:(bb+vdim-1);
D4(I4,J4) = denomBase;
csD1_R = cumsum(D1,2);
%csD1_C = cumsum(D1,1);
csD1 = cumsum(csD1_R,1);
csD2_R = cumsum(D2,2);
%csD2_C = cumsum(D2,1);
csD2 = cumsum(csD2_R,1);
csD3_R = cumsum(D3,2);
csD3_C = cumsum(D3,1);
csD3 = cumsum(csD3_R,1);
csD4_R = cumsum(D4,2);
csD4_C = cumsum(D4,1);
csD4 = cumsum(csD4_R,1);
DMatB = (csD1-csD2-csD3+csD4) +(csD2_R - csD4_R) + (csD3_C-csD4_C) + D4;

    if strcmpi(opat,'UC')
    I5 = hdim:(aa+hdim-1);
    J5 = (1+(vdim-1)/2):(bb+(vdim-1)/2);
    DMat = DMatB(I5,J5);
    N2s = N2sMatB(I5,J5);
    elseif strcmpi(opat,'CC')
    I5 = (1+(hdim-1)/2):(aa+(hdim-1)/2);
    J5 = (1+(vdim-1)/2):(bb+(vdim-1)/2);
    DMat = DMatB(I5,J5);
    N2s = N2sMatB(I5,J5);
end
pdm = N2s./DMat;
ILT = find(pdm < thresh);
pdm(ILT) = zeros(size(ILT));

