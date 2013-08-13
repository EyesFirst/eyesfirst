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

function SAA = slice_shift(A,xdeg,zdeg,ofile)
[aa,bb,cc] = size(A);
if rem(aa,2) ~= 0 || rem(aa,2) ~= 0
    error('cube dimensions must be even\n')
end;
haap1 = (aa/2)+1;
hbbp1 = (bb/2)+1;
for i_y = 1:cc-1
    s1=double(squeeze(A(:,:,i_y)));
    s2=double(squeeze(A(:,:,i_y+1)));
    t1 = fft2(s2);
    t2 = conj(t1);
    t3 = fft2(s1);
    t4 = t3.*t2;
    c12=ifft2(t4);
    c12=fftshift(c12);
    %figure(3),mesh(fftshift(c12))
    [value,index]=max(c12(:));
    [z_shift,x_shift] = ind2sub(size(c12),index);
    z_array(i_y) = z_shift-haap1;
    x_array(i_y) = x_shift-hbbp1;
end

% These were not commented out in the motionCorrect version, but I'm fairly
% sure they're just for debug purposes and don't do anything terribly
% useful anyway.
% figure(5),subplot(211),plot(1:cc-1,z_array,'r.',1:cc-1,z_array,'b'),title('Z shift')
% figure(5),subplot(212),plot(1:cc-1,x_array,'r.',1:cc-1,x_array,'b'),title('X shift')
SAA = shiftAlignImage_v2(A,z_array,x_array,zdeg,xdeg);
if ~isempty(ofile)
   save(ofile,'SAA');
end;