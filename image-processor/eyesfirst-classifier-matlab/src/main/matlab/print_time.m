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

function [ ] = print_time( timestamp )
%print_time Print a timestamp
%   Timestamp is a time in days as if it were produced by now, so to time a
%   function record now prior to running it, then run it, then call
%   print_time(now - previous_now)
    
    hours = floor(timestamp * 24);
    minutes = floor((timestamp * 24 - hours) * 60);
    seconds = ((timestamp * 24 - hours) * 60 - minutes) * 60;
    fprintf('%d:%02d:', hours, minutes);
    if (seconds < 10)
        fprintf('0');
    end
    fprintf('%f', seconds);
end

