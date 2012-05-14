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

function json( obj, varargin )
%json Outputs the given input as JSON
%   Writes the given output as JSON.

    closeOnExit = 0;
    if (nargin > 1)
        fileID = varargin(1);
        if iscellstr(fileID)
            fileID = char(fileID);
        else
            fileID = cell2mat(fileID);
        end
        if ischar(fileID)
            closeOnExit = 1;
            fileID = fopen(fileID, 'w');
        end
    else
        fileID = 1;
    end

    if (ischar(obj))
        fprintf(fileID, '"%s"', stringescape(obj));
    elseif (isnumeric(obj))
        s = size(obj);
        if s(1) == 1
            if s(2) == 1
                % MATLAB doesn't really distinguish between a
                % single-element matrix and a number, so this is kind of...
                % wrong, but do it anyway.
                printnum(fileID, obj);
            else
                % Print as single array
                fprintf(fileID, '[');
                for index = 1:s(2)
                    if (index > 1)
                        fprintf(fileID, ', ');
                    end
                    printnum(fileID, obj(index));
                end
                fprintf(fileID, ']');
            end
        else
            if s(2) == 1
                % Print as an array of numbers (see note above about how
                % this is conceptually "wrong" but it's more right in this
                % case.
                fprintf(fileID, '[');
                for row = 1:s(1)
                    if (row > 1)
                        fprintf(fileID, ', ');
                    end
                    printnum(fileID, obj(row));
                end
                fprintf(fileID, ']');
            else
                % Print as multiple arrays
                fprintf(fileID, '[');
                for row = 1:s(1)
                    if (row > 1)
                        fprintf(fileID, ', ');
                    end
                    fprintf(fileID, '[');
                    for col = 1:s(2)
                        if (col > 1)
                            fprintf(fileID, ', ');
                        end
                        printnum(fileID, obj(row, col));
                    end
                    fprintf(fileID, ']');
                end
                fprintf(fileID, ']');
            end
        end
    elseif isstruct(obj)
        names = fieldnames(obj);
        fprintf(fileID, '{');
        for index = 1:length(names)
            name = char(names{index});
            if (index > 1)
                fprintf(fileID, ',');
            end
            fprintf(fileID, '"%s":', stringescape(name));
            json(obj.(name), fileID);
        end
        fprintf(fileID, '}');
    else
        fprintf(fileID, 'null');
    end
    if closeOnExit
        fclose(fileID);
    end
end

function res = stringescape(str)
    res = regexprep(str, '([\\"])', '\\$1');
end

function printnum(fileID, num)
    if isinteger(num)
        fprintf(fileID, '%d', num);
    else
        % Does the number "look" like an integer?
        %if round(num) - num 
        fprintf(fileID, '%s', num2str(num, 15));
    end
end