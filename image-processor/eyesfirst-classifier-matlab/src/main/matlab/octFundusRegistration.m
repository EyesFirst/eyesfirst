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

function [offset,scFactor_sr,regCube,O2FrowInd,O2FcolInd] = octFundusRegistration(octIm,origOCTDim,fundusIm,octInterpFlag,scaleFactor_sr,useSmoothFlag)
%
if octInterpFlag == 1
    % octIm has been interpolated to approximately square pixels
    octDim = origOCTDim;
    octDimInterp = size(octIm);
    octIm_interp = octIm;
else
    octIm_interp = interpEnface(octIm);
    octDim = size(octIm);
    octDimInterp = size(octIm_interp);
end
fundusDim = size(fundusIm);
% load oct/fundus scaling factor 
if isempty(scaleFactor_sr)
    scFactorFile = 'scFactor_OCT2Fund_fastTime_slowTime';
    load(scFactorFile)
end
% extract vessels from OCT image
[rowZCmidPoint_O,colZCmidPoint_O,oct_vessel_binary] = extractVessels_filtered(octIm_interp);
%
% smooth oct vessel image
fundusGradientFile = [];%'fundus_gradient_file';
% extract vessels from the fundus image
[rowZCmidPoint_F,colZCmidPoint_F,fundus_vessel_binary] = extractVessels_fundus(fundusIm,fundusGradientFile);
%
if useSmoothFlag == 1
% smooth vessel images
   oct_vb_sm = gauskerndiff2d_wzp(oct_vessel_binary,0,0,3,3,4,4);
   fund_vb_sm = gauskerndiff2d_wzp(fundus_vessel_binary,0,0,3,3,4,4);
   % compute peak of cross correlation
   [offset,regCube,O2FrowInd,O2FcolInd] = corrOffSet(oct_vb_sm,fund_vb_sm,scFactor_sr);
else
    [offset,regCube,O2FrowInd,O2FcolInd] = corrOffSet(oct_vessel_binary,fundus_vessel_binary,scFactor_sr);
end;
crossCorrelationFile = 'OCTFundusCrossCorrPeak'
save('crossCorrelationFile','offset','octDim','octDimInterp','fundusDim','scFactor_sr','regCube','O2FrowInd','O2FcolInd')
end

