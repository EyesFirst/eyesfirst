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

function [gate] = extractGate3(pp1,plflag,figh)
% pp1 is a spline approximation of g1 and pp2 is a spline approximation of
% g2. On return gate is a  vector containing the well identified maxima of
% g1.  A maxima of g1 is well identified if there is a corresponding zero
% of g2 with a negative slope
%

minabsthresh = 0.001;
rootdiftol = 1.0e-2;
% identify maxima of pp1
dpp1 = fnder(pp1,1);
d2pp1 = fnder(pp1,2);
dpp1rootsa = fnzeros(dpp1);
if ~isempty(dpp1rootsa)
    delroot = dpp1rootsa(2,:)-dpp1rootsa(1,:);
    I1 = find(delroot <= rootdiftol);
    dpp1roots = dpp1rootsa(1,I1);
    vd2rootsdpp1 = fnval(d2pp1,dpp1roots);
    I1 = find(vd2rootsdpp1 < 0);
    if ~isempty(I1)
        locmaxpp1a = dpp1roots(I1);
        locmaxvalpp1a = fnval(pp1,dpp1roots(I1));
        I1a = find(locmaxvalpp1a >= minabsthresh);
        if ~isempty(I1a)
            locmaxpp1 = locmaxpp1a(I1a);
            locmaxvalpp1 = locmaxvalpp1a(I1a);
            I2 = find(vd2rootsdpp1 > 0);
            if ~isempty(I2) > 0
                locminpp1 = dpp1roots(I2);
                % locminvalpp1 = fnval(pp1,dpp1roots(I2));
            else
                % locminppa = [];
            %     I2a = find(locminvalpp1a >= minabsthresh);
            %     locmaxpp1 = locminpp1a(I2a);
            %     locmaxvalpp1 = locminvalpp1a(I2a);
            end;

            % identfy gate as smallest local min interval of pp1 that contains the
            % local locmaxpp1.
            [minmaxpp1,~] = min(locmaxpp1);
            [maxmaxpp1,~] = max(locmaxpp1);
            if ~isempty(locminpp1)
                Imm = find(locminpp1-minmaxpp1 < 0);
                if ~isempty(Imm)
                   minbdry = max(locminpp1(Imm));
                else
                    minbdry = minmaxpp1;
                end;
                Imm2 = find(locminpp1-maxmaxpp1 > 0);
                if ~isempty(Imm2)
                   maxbdry = min(locminpp1(Imm2));
                else
                    maxbdry = maxmaxpp1;
                end;
            else
                minbdry = minmaxpp1;
                maxbdry = maxmaxpp1;
            end;
            gate = [minbdry maxbdry];
            if plflag == 1
                if isempty(figh)
                    figure;
                else
                    figure(figh)
                end
                if ~isempty(pp1)
                    fnplt(pp1)
                    if ~isempty(locmaxpp1)
                        hold on;plot(locmaxpp1,locmaxvalpp1,'rx')
                    end;
                end
                plot(gate(1),0,'kx','markersize',16)
                plot(gate(2),0,'kx','markersize',16)
            end;
        else
            gate = [];
        end;
    else
        gate = [];
    end
else
    gate = [];
end;

        
        


