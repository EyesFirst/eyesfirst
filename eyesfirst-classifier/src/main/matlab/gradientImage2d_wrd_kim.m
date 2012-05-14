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

function [gim,g2im,kim] = gradientImage2d_wrd_kim(im,sdv,mf,ofilebase)
% [gim,g2im] = gradientImage(im)
% if g1flag == 1, computes the gradient image, returned in gim, and if g2flag == 1, the derivative of the
% gradient image in the direction of the gradient, returned in
% g2im
% wrd indicates with respect to distance, i.e., derivatives are converted
% into per micron or per micron^2
% if length sdv == 1 then the standard deviation used in the smoothing
% filter is scaled to have the same magnitude in microns in each direction,
% and if sdv is a two vector the component values are used without scaling
    %
dx = 1.9531; % pixel length (microns) in axial direction
dy = 11.7188; % pixel length (microns) in fast time direction
g1flag = 1;
g2flag = 1;
n1 = 1;
n2 = [];
%n3 = [];
if length(sdv) == 2
    sd1 = sdv(1);
    sd2 = sdv(2);
else
    sd1 = sdv;
    sd2 = sd1*(dx/dy);
end;
%sd3 = sd;
% extent1 = 6;
% extent2 = 6;
% extent3 = 6;
extent1 = mf*sd1;
extent2 = mf*sd2;
%extent3 = mf*sd;
imx = gauskerndiff2d(im,n1,n2,sd1,sd2,extent1,extent2);
imx = imx/dx;
n1 = [];
n2 = 1;
n3 = [];
imy = gauskerndiff2d(im,n1,n2,sd1,sd2,extent1,extent2);
imy = imy/dy;
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
    imxx = imxx/(dx^2);
    n1 = [];
    n2 = 2;

    imyy = gauskerndiff2d(im,n1,n2,sd1,sd2,extent1,extent2);
    imyy = imyy/(dy^2);
%     n1 = [];
%     n2 = [];
%     n3 = 2;
%     imzz = gauskerndiff(im,n1,n2,n3,sd1,sd2,sd3,extent1,extent2,extent3);
    n1 = 1;
    n2 = 1;

    imxy = gauskerndiff2d(im,n1,n2,sd1,sd2,extent1,extent2);
    imxy = imxy/(dy*dx);
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
    

%%%%%%%%%% CALCULATE CURVATURE
    
T1=-imy./sqrt((imx.^2)+(imy.^2));
T2=imx./sqrt((imx.^2)+(imy.^2));  
kim = sqrt((imxy.*T1+imyy.*T2).^2+(imxx.*T1+imxy.*T2).^2);

end;
% ofilebase2 = [ofilebase,'_sd',int2str(sd)];
% cofile = [ofilebase2,'_dx'];
% save(cofile,'imx')
% cofile = [ofilebase2,'_dy'];
% save(cofile,'imy')
% % cofile = [ofilebase2,'_dz'];
% % save(cofile,'imz')
% cofile = [ofilebase2,'_dxx'];
% save(cofile,'imxx')
% cofile = [ofilebase2,'_dyy'];
% save(cofile,'imyy')
% cofile = [ofilebase2,'_dxy'];
% save(cofile,'imxy')
% % cofile = [ofilebase2,'_dzz'];
% % save(cofile,'imzz')
% cofile = [ofilebase2,'_g1'];
% save(cofile,'gim')
% cofile = [ofilebase2,'_g2'];
% save(cofile,'g2im')
   