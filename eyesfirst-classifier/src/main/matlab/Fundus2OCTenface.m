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

function [ft_OCT,st_OCT] = Fundus2OCTenface(ft_fundus,st_fundus,interpCoordFlag,CCPeak,dimFundus,dimOCT,dimInterpOCT,scFactor)
% see OCTenface2Fundus for a description of the inputs
% ft_fundus is the vector of fast-time coordinates, where fast time
% corresponds to the fast-time dimension of the OCT; similarly st_fundus is
% the vector of slow-time coordinates
% if interpCoordFlag is 1, then the returned oct coordinates are for the
% interpolated OCT image, whereas if interpCoordFlag = 0 they are for the
% original OCT image; CCPeak is the cross correlation peak of the
% interpolated OCT image with the fundus image; dimFundus is the dimension
% of the fundus image [slow-time,fast-time];dimOCT is the dimension of the
% original OCT image [slow-time,fast-time]; dimInterpOCT is the dimension
% of the interpolated OCT image [slow-time,fast-time]; scFactor is the
% [fast-Time,slowTime] scaling translation used to map the interpolated OCT
% image onto the fundus image
% 

dft = 11.7188; % fast-time dimension pixel length
dst = 46.875; % slow-time dimension pixel length
% sf_O2F_st = scFactor(2);
% sf_O2F_ft = scFactor(1);
TRmat = diag(scFactor.^(-1));
aa = length(ft_fundus);
bb = length(st_fundus);
if aa ~= bb
    error('dimension mismatch\n');
else
    ft_fundus = reshape(ft_fundus,1,aa);
    st_fundus = reshape(st_fundus,1,aa);
    CCPeak = reshape(CCPeak,2,1);
end;
if interpCoordFlag == 1 % map from fundus to interpolated OCT 
    trFundus = [ft_fundus;st_fundus] - repmat(CCPeak,1,aa);
    OCTInd = round(TRmat*trFundus); % note that the order of the components is (fast-time (jj),slow-time(ii))
    ft_OCT = OCTInd(1,:);
    st_OCT = OCTInd(2,:);
%     I2 = find(st_OCT > dimInterpOCT(1) | ft_OCT > dimInterpOCT(2) | st_OCT < 1 | ft_OCT < 1 );
%     if ~isempty(I2)
%        ft_OCT(I2) = NaN;
%        st_OCT(I2)  = NaN;
%     end;
    ft_OCT = min(ft_OCT,dimInterpOCT(2));
    st_OCT = min(st_OCT,dimInterpOCT(1));
    ft_OCT = max(1,ft_OCT);
    st_OCT = max(1,st_OCT);
else % map from fundus to original OCT coordinates
    % map from fundus to interpolated OCT 
    trFundus = [ft_fundus;st_fundus] - repmat(CCPeak,1,aa);
    OCTInd_interp = round(TRmat*trFundus); % note that the order of the components is (fast-time (jj),slow-time(ii))
    ft_OCT_interp = OCTInd_interp(1,:);
    st_OCT_interp = OCTInd_interp(2,:);
    ft_OCT = min(round(1+(ft_OCT_interp-1)*(dimOCT(2)-1)/(dimInterpOCT(2)-1)),dimOCT(2));
    st_OCT = min(round(1+(st_OCT_interp-1)*(dimOCT(1)-1)/(dimInterpOCT(1)-1)),dimOCT(1));
    ft_OCT = max(1,ft_OCT);
    st_OCT = max(1,st_OCT);
%     I2 = find(st_OCT > dimOCT(1) | ft_OCT > dimOCT(2) | st_OCT < 1 | ft_OCT < 1 );
%     if ~isempty(I2)
%        ft_OCT(I2) = NaN;
%        st_OCT(I2)  = NaN;
%     end;
end

