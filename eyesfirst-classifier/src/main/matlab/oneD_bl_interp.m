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

function outseq = oneD_bl_interp(inseq,n_interp,p_flag,fig_num);
%        outseq = oneD_bl_interp(inseq,n_interp,p_flag,fig_num);
% inseq = input sequence
% n_interp = interpolation factor where:
% n_interp = 2 interpolates 1 new data value between each original data point
% n_interp = 3 interpolates 2 new data values between each original data point
% n_interp = 4 interpolates 3 new data values between each original data point
% p_flag = 1 plot original and interpolated data in figure fig_num

[nrows,ncols] = size(inseq);

if ncols == 1
    inseq = inseq.';
end

seq = inseq;
trans = fft(seq);
n_samp = length(trans);

if rem(n_samp,2) == 0
    % Even number of samples
    temp_trans = fftshift(trans);
    temp_trans(1) = temp_trans(1)/2;
    temp_trans(length(temp_trans)+1) = temp_trans(1);
    m = (n_samp*n_interp - (n_samp + 1));
    k = (m + 1)/2;
    padded_trans = [zeros(1,k) temp_trans zeros(1,k-1)];
    zpad = n_interp*ifft(ifftshift(padded_trans));
end


if rem(n_samp,2) == 1
    % odd number of samples
    if rem(n_interp,2) == 0
        % Even interpolation factor
        k = ((n_interp - 1)*n_samp + 1)/2;
        padded_trans = [ zeros(1,k) fftshift(trans) zeros(1,(k - 1))];
        zpad = n_interp*ifft(ifftshift(padded_trans));
    else
        % Odd interpolation factor
        k = n_samp*(n_interp - 1)/2;
        padded_trans = [ zeros(1,k) fftshift(trans) zeros(1,k)];
        zpad = n_interp*ifft(ifftshift(padded_trans));
    end
end

if p_flag == 1
    figure(fig_num),plot((0:(n_samp - 1)),seq,'b.',(0:(n_samp*n_interp - 1))/n_interp,zpad,'go')
end

outseq = zpad;

if ncols == 1
    outseq = outseq.';
end
