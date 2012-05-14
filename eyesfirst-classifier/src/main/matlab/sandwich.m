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

function [IUJs,IDJs] = sandwich(IUJ,IDJ)
% IUJ and IDJ are indices of the larget indexSet
% On return, IUJs and IDJs are subsequences of IUJ and IDJ that are
% sandwiched so that no two indices or IUJ or IDj are adjacent.  This is
% achieved by replacing a run of consecutive IDJs with the largest value and a run
% of consecutive IUJs with the smallest value
IUJs = zeros(size(IUJ));
IDJs = zeros(size(IDJ));
if ~isempty(IUJ) || ~isempty(IDJ)
    if isempty(IUJ) 
        IDJs = max(IDJ);
    elseif isempty(IDJ)
        IUJs = min(IUJ);
    else
        lastTerm = 0;
        IDJind = 1;
        % UindexCt = 1;
        loopCt = 1;
        dPoint = 1;
        uPoint = 1;
        Ndj = length(IDJ);
        Nuj = length(IUJ);
        if IUJ(uPoint) < IDJ(dPoint)
           IUJs(1) = IUJ(1);
           while dPoint <= Ndj && uPoint <= Nuj
              loopCt = loopCt+1;
              IUJind = find(IUJ > IDJ(dPoint), 1,'first');
              if ~isempty(IUJind)
                 uPoint = IUJind;
                 IUJval = IUJ(IUJind);
                 IUJs(loopCt) = IUJval;
                 [IDJind] = find(IDJ < IUJval,1,'last'); 
                 dPoint = IDJind+1;
                 IDJval = IDJ(IDJind);
                 IDJs(loopCt-1) = IDJval;
              else
                  uPoint = Nuj+1;
                  IDJval = max(IDJ);  
                  IDJs(loopCt-1) = IDJval;
              end
           end;
           IUJsind = find(IUJs~=0);
           IUJs = IUJs(IUJsind);
           IDJsind = find(IDJs~=0);
           IDJs = IDJs(IDJsind);
        else
            % find the largest D below the smallest U 
            D1ind = find(IDJ < IUJ(1),1,'last');
            FDval = IDJ(D1ind);
            if D1ind < length(IDJ)
               IDJ2 = IDJ(D1ind+1:end);
               [IUJs,IDJs] = sandwich(IUJ,IDJ2);
               IDJs = [FDval IDJs];
            else
                IDJs = FDval;
                [IUJsind] = find(IUJ > IDJs, 1,'first');
                IUJs = IUJ(IUJsind);
            end;
        end;
    end;
end;

              
             
    

end

