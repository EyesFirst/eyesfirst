% Copyright 2013 The MITRE Corporation
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

function [norm2gim,eigValRatioIm,g2im,kim] = gradientImageDecomposition(cim)
mf = 4; % defines the extent of the smoothing filters as +/- mf*std
strucTensorSigma = [1,1];%[10 10];%Gaussian smoothing parameters used in the smoothing of the structure tensor
divergenceSigma =  [3 3];% gaussian smoothing parameter used in the calculation of the divergence
gradsigma1 = [3,3];% gaussian smoothing parameters
grad2sigma = [3,3];% [3,3]
% gradsigma = difPar.gradsigma;
% strucTensorSigma = difPar.strucTensorSigma;
% mf = difPar.mf;
[aa,bb] = size(cim);
extent = mf*strucTensorSigma;
plotFlag = 0;
[~,gradSmoothKernelx] = gaussKernel(strucTensorSigma(1),extent(1),plotFlag);
[~,gradSmoothKernely] = gaussKernel(strucTensorSigma(2),extent(2),plotFlag);
gim = gradientImageVector2d_wzp_sqim(cim,gradsigma1,mf);
% calculate structure tensor in a-b-c fom
strucTensor = reshape([gim(:,:,1).^2 gim(:,:,1).*gim(:,:,2) gim(:,:,2).^2],aa,bb,3);
% smooth each of the three (a,b,c) images using strucTensorSigma
smoothStrucTensor = zeros(size(strucTensor));
for kk = 1:3
    smoothStrucTensor(:,:,kk) = squeeze(imageSmooth2d(squeeze(strucTensor(:,:,kk)),gradSmoothKernelx,gradSmoothKernely));
end
% Form eigenvalue and principle eigenrotation images
a = squeeze(smoothStrucTensor(:,:,1));
b = squeeze(smoothStrucTensor(:,:,2));
c = squeeze(smoothStrucTensor(:,:,3));
lam1 = (a+c+sqrt((a-c).^2 +4*(b.^2)))/2;
lam2 = (a+c-sqrt((a-c).^2 +4*(b.^2)))/2;
princAngle = atan2((c-a) + sqrt((a-c).^2+4*(b.^2)),2*b);
% Form the eigenvalue images of the diffusion tensor using the
% construct the EED eigenvalues
norm2gim=squeeze(gim(:,:,1)).^2 + squeeze(gim(:,:,2)).^2;
eigValRatioIm = lam2./lam1;
[n2gim,g2im,kim] = gradientImage2d_wrd_kim_wzp_sq(cim,gradsigma1,grad2sigma,mf);
end

