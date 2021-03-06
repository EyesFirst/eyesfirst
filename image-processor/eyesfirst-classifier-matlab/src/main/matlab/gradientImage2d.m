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

function [gim,g2im] = gradientImage2d(im,sd,mf,ofilebase)
% [gim,g2im] = gradientImage(im)
% if g1flag == 1, computes the gradient image, returned in gim, and if g2flag == 1, the derivative of the
% gradient image in the direction of the gradient, returned in
% g2im
    %
    g1flag = 1;
    g2flag = 1;
n1 = 1;
n2 = [];
%n3 = [];
sd1 = sd;
sd2 = sd;
%sd3 = sd;
% extent1 = 6;
% extent2 = 6;
% extent3 = 6;
extent1 = mf*sd;
extent2 = mf*sd;
extent3 = mf*sd;
imx = gauskerndiff2d(im,n1,n2,sd1,sd2,extent1,extent2);
n1 = [];
n2 = 1;
n3 = [];
imy = gauskerndiff2d(im,n1,n2,sd1,sd2,extent1,extent2);
% n1 = [];
% n2 = [];
% n3 = 1;
% imz = gauskerndiff(im,n1,n2,n3,sd1,sd2,sd3,extent1,extent2,extent3);
if g1flag == 1
   gim = (imx.^2 + imy.^2 ).^(0.5);
else
    gim = [];
end;
if g2flag == 1
    n1 = 2;
    n2 = [];

    imxx = gauskerndiff2d(im,n1,n2,sd1,sd2,extent1,extent2);
    n1 = [];
    n2 = 2;

    imyy = gauskerndiff2d(im,n1,n2,sd1,sd2,extent1,extent2);
%     n1 = [];
%     n2 = [];
%     n3 = 2;
%     imzz = gauskerndiff(im,n1,n2,n3,sd1,sd2,sd3,extent1,extent2,extent3);
    n1 = 1;
    n2 = 1;

    imxy = gauskerndiff2d(im,n1,n2,sd1,sd2,extent1,extent2);
%     n1 = 1;
%     n2 = [];
%     n3 = 1;
%     imxz = gauskerndiff(im,n1,n2,n3,sd1,sd2,sd3,extent1,extent2,extent3);
%     n1 = [];
%     n2 = 1;
%     n3 = 1;
%     imyz = gauskerndiff(im,n1,n2,n3,sd1,sd2,sd3,extent1,extent2,extent3);
   % g2im = ((imx.^2).*imxx + 2*(imx.*imy.*imxy) + 2*(imx.*imz.*imxz) + 2*(imy.*imz.*imyz) + (imy.^2).*imyy + (imz.^2).*imzz)./(gim.^2);
    g2im = ((imx.^2).*imxx + 2*(imx.*imy.*imxy)  + (imy.^2).*imyy)./(gim.^2);
end;
% DPOTTER: Commented out unneeded MAT file generation
%ofilebase2 = [ofilebase,'_sd',int2str(sd)];
%cofile = [ofilebase2,'_dx'];
%save(cofile,'imx')
%cofile = [ofilebase2,'_dy'];
%save(cofile,'imy')
% cofile = [ofilebase2,'_dz'];
% save(cofile,'imz')
%cofile = [ofilebase2,'_dxx'];
%save(cofile,'imxx')
%cofile = [ofilebase2,'_dyy'];
%save(cofile,'imyy')
%cofile = [ofilebase2,'_dxy'];
%save(cofile,'imxy')
% cofile = [ofilebase2,'_dzz'];
% save(cofile,'imzz')
%cofile = [ofilebase2,'_g1'];
%save(cofile,'gim')
%cofile = [ofilebase2,'_g2'];
%save(cofile,'g2im')
   