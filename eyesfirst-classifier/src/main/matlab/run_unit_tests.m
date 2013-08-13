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

function [ ] = run_unit_tests( )
%run_unit_tests run unit tests
%   At present, this basically does nothing more than check to make sure
%   all the MATLAB stuff is present (and wasn't excluded due to a license
%   checkout failure).
%   Fun fact: it appears that compiling this file as a root file makes the
%   build *actually fail* if the licenses are missing. Amazing!

% toolbox/matlab/mcc.enc - I'm assuming if this runs at all, that's
% present.

% toolbox/images/mcc.enc
% Create a structuring element to verify the image toolkit is here
strel('disk', 4, 4);
    
% toolbox/curvefit/mcc.enc
% Try and create fitoptions in order to do... something.
fitoptions;

% toolbox/optim/mcc.enc
% Again, create something simple to just verify the toolkit is there.
optimset;

% toolbox/stats/mcc.enc
% This is copied out of the example documentation and should be enough to,
% again, verify the toolkit exists.
mu = [1 2;-3 -5];
sigma = cat(3,[2 0;0 .5],[1 0;0 1]);
p = ones(1,2)/2;
gmdistribution(mu,sigma,p);

% I guess I'm just going to hope the above is enough and I can ignore the
% following, since they seem to be related:

% toolbox/shared/imageslib/mcc.enc
% toolbox/shared/optimlib/mcc.enc
% toolbox/shared/spcuilib/mcc.enc

end

