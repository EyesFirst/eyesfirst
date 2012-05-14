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

classdef RetinalThicknessViewer < handle
    properties (SetAccess = private)
        data_dir
        figure_handle
        file_list_handle
        file_names
    end
    methods
        function RTV = RetinalThicknessViewer(data_dir)
            % Create the figure
            RTV.figure_handle = figure('Name', 'Retinal Thickness Viewer', 'Visible', 'off', 'MenuBar', 'none');
            % Create the menu
            file_menu = uimenu(RTV.figure_handle, 'Label', 'File');
            RTV.data_dir = data_dir;
            uimenu(file_menu, 'Label', 'Set data directory...', 'Callback', { @changeDataDir, RTV });
            RTV.file_list_handle = uicontrol(RTV.figure_handle,...
                'Style', 'listbox',...
                'Units', 'Normalized', 'Position', [0 0 1 1],...
                'Callback', {@file_list_callback, RTV});
            set(RTV.figure_handle, 'Visible', 'on');
            setDataDir(RTV, data_dir)
        end
        function setDataDir(RTV, newDataDir)
            % The data directory should contain a directory called
            % "storeLayerBdrys"
            % TODO: Allow one of the "store" directories to be used directly.
            if ~exist(newDataDir, 'dir')
                errordlg(sprintf('%s does not exist (or is not a directory)', newDataDir));
                return
            end
            layer_bdry_dir = [ newDataDir filesep 'storeLayerBdrys' ];
            if ~exist(layer_bdry_dir, 'dir')
                errordlg(sprintf('%s does not contain retinal thickness data', newDataDir));
                return
            end
            RTV.data_dir = newDataDir;
            files = dir([ layer_bdry_dir filesep '*.mat' ]);
            data = cell(length(files), 1);
            RTV.file_names = cell(length(files), 1);
            for I = 1:length(files)
                data{I, 1} = RetinalThicknessViewer.format_name(files(I).name);
                RTV.file_names{I, 1} = files(I).name;
            end
            set(RTV.file_list_handle, 'String', data);
        end
        function changeDataDir(hObj, ~, RTV)
            % Event callback used to ask the user for a new data directory.
            % May be called directly with just the handle, in which case
            % the RTV is invoked directly.
            if nargin == 1
                RTV = hObj;
            end
            dd = uigetdir(RTV.data_dir, 'Select data directory');
            if dd
                setDataDir(RTV, dd);
            end
        end
        function file_list_callback(hObj, ~, RTV)
            if strcmp(get(RTV.figure_handle, 'SelectionType'), 'open')
                % Display the selected strings
                selected = get(hObj, 'Value');
                for I = 1:length(selected)
                    filename = RTV.file_names{selected(I)};
                    fprintf('%d: %s\n', I, filename);
                    wbh = waitbar(0, sprintf('Loading %s...', untex(filename)));
                    try
                        complete_path = [ RTV.data_dir filesep 'storeLayerBdrys' filesep filename];
                        fprintf('Loading %s...\n', complete_path);
                        load(complete_path);
                        close(wbh);
                        if exist('stLayerBdrys', 'var') && iscell(stLayerBdrys) && length(stLayerBdrys) > 1 && isstruct(stLayerBdrys{1}) %#ok<USENS>
                            if isfield(stLayerBdrys{1}, 'oimwsla2d')
                                retinal_thickness_visualizer(stLayerBdrys);
                            elseif isfield(stLayerBdrys{1}, 'oimwla')
                                retinal_thickness_visualizer(stLayerBdrys, 'oimwla');
                            end
                        else
                            errordlg(sprintf('%s did not contain retinal thickness data.', filename));
                        end
                    catch err
                        if ishandle(wbh)
                            close(wbh);
                        end
                        errordlg(sprintf('An error occurred while loading %s: %s', RTV.file_names{selected(I)}, err.message));
                    end
                end
            end
        end
    end
    methods(Access=private, Static=true)
        function res = format_name(str)
            res = str;
            if length(str) > 4
                if strcmpi(str(length(str)-3:length(str)), '.mat')
                    res = str(1:length(str)-4);
                end
            end
        end
    end
end