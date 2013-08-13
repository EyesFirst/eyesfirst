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

function [ft_fundus,st_fundus] = OCTenface2Fundus(ft_oct,st_oct,interpCoordFlag,CCPeak,dimFundus,dimOCT,dimInterpOCT,scFactor) 
% (ft_oct,st_oct) are the fast-time slow-time coordinates of a point in an
% OCT image; 
% if interpCoordFlag == 1, then the coordinates are from the interpolated OCT image, otherwise, they are from the original image
% CCPeak is the (fast-time,slow-time) cross correlation peak of
% the scaled-interpolated OCT image on the fundus image.
%scFactor is the (fast-time,slow-time) scaling factor mapping from the OCT
%to the fundus image assuming that the OCT image has been interpolated 
dft = 11.7188; % fast-time dimension pixel length
dst = 46.875; % slow-time dimension pixel length
% sf_O2F_st = scFactor(2);
% sf_O2F_ft = scFactor(1);
TRmat = diag(scFactor);
%if tensorMode == 0 % the indices are given explicitly; else if tensorMode == 1, the set of indices is the tensor product of the coordinate vectors
aaFO = length(ft_oct);
bbFO = length(st_oct);
if aaFO ~= bbFO
    error('dimension mismatch\n');
else
    ft_oct = reshape(ft_oct,1,aaFO);
    st_oct = reshape(st_oct,1,aaFO);
    CCPeak = reshape(CCPeak,2,1);
end;
if interpCoordFlag == 1 % coordinates are for interpolated image
    fundusInd = round(repmat(CCPeak,1,aaFO)+TRmat*[ft_oct;st_oct]); % note that the order of the components is (fast-time (jj),slow-time(ii))
    ft_fundus = fundusInd(1,:);
    st_fundus = fundusInd(2,:);
   % I2 = find(ft_fundus > dimFundus(2) | st_fundus > dimFundus(1));
    ft_fundus = min(ft_fundus,dimFundus(2));
    ft_fundus = max(ft_fundus,1);
    st_fundus = min(st_fundus,dimFundus(1));
    st_fundus = max(st_fundus,1);
%     if ~isempty(I2)
%        ft_fundus(I2) = NaN;
%        st_fundus(I2)  = NaN;
%     end;
else
    % resample slow time to unify slow and fast time sampling
    if ~isempty(dimInterpOCT) && ~isempty(dimOCT) % reindex by assuming a uniform resampling
        aa = dimOCT(1);
        bb = dimOCT(2);
        aa_interp = dimInterpOCT(1);
        bb_interp = dimInterpOCT(2);
        if aa < bb % slow time is first component
            lft_oct = bb; %length-fast-time
            lst_oct = aa; % length-slow-time
            lst_oct_interp = aa_interp; % assumes that the fast-time, slow-time dimensions are in the same order
            lft_oct_interp = bb_interp;
            % checks that fast time dimensions in the interpolated image
            % and the original image are the same
            if lft_oct ~= lft_oct_interp
                error('check dimensions\n')
            end
            st_oct_interp = round(((lst_oct_interp-1)/(lst_oct-1))*(st_oct-1)+1);
            ft_oct_interp = ft_oct; % no resampling in the fast-time direction
        else % slow time is second component
            lft_oct = aa; %length-fast-time
            lst_oct = bb; % length-slow-time
            lst_oct_interp = bb_interp; % assumes that the fast-time, slow-time dimensions are in the same order
            lft_oct_interp = aa_interp;
            % checks that fast time dimensions in the interpolated image
            % and the original image are the same
            if lft_oct ~= lft_oct_interp
                error('check dimensions\n')
            end
            st_oct_interp = round(((lst_oct_interp-1)/(lst_oct-1))*(st_oct-1)+1);
            ft_oct_interp = ft_oct; % no resampling in the fast-time direction
        end
    else % dimensions of interpolated image not provided; assume that interpolation is done to render the pixels of the interpolated image a square
        st_oct_interp = round(1+ (dst/dft)*(st_oct-1));
        ft_oct_interp = ft_oct;
    end
    % identify corresponding fundus indexing
    fundusInd = round(repmat(CCPeak,1,aaFO)+TRmat*[ft_oct_interp;st_oct_interp]); % note that the order of the components is (fast-time (jj),slow-time(ii))
    ft_fundus = fundusInd(1,:);
    st_fundus = fundusInd(2,:);
%     I2 = find(ft_fundus > dimFundus(2) | st_fundus > dimFundus(1));
%     if ~isempty(I2)
%        ft_fundus(I2) = NaN;
%        st_fundus(I2)  = NaN;
%     end;
    ft_fundus = min(ft_fundus,dimFundus(2));
    ft_fundus = max(ft_fundus,1);
    st_fundus = min(st_fundus,dimFundus(1));
    st_fundus = max(st_fundus,1);
end
%else
    



end

