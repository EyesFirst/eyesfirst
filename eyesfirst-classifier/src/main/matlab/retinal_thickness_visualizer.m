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

% Displays visualization based on the given data.

function [handle] = retinal_thickness_visualizer(layerData, layer_id)

    wbh = waitbar(0, 'Generating slice data...');
try
    % Load the layer data
    if (nargin == 1)
        layer_id = 'oimwsla2d';
    end
    handle.scale = 0.125;
    % Image size millimeters (x,y,z)
    handle.image_size = [ 6 6 2 ];
    % The complete volume is used to generate slice images at full
    % resolution
    handle.complete_volume = convert_to_volume(layerData, layer_id, 1.0, 'WaitBar', wbh, 'WaitTotal', 0.5);
    waitbar(0.5, wbh, 'Generating volume data...');
    volume = convert_to_volume(layerData, layer_id, handle.scale, 'WaitBar', wbh, 'WaitStart', 0.5, 'WaitTotal', 0.25);
    handle.volume = volume;
    % Generate the final figure
    waitbar(0.75, wbh, 'Generating 3D visualization...');

    % Grab the current monitor (primary is always 0?)
    available_space = get(0, 'MonitorPositions');
    available_space = available_space(1,:);

    handle.fh = figure('Name', 'Retinal Thickness Viewer', 'Visible', 'off',...
        'OuterPosition', [ available_space(1), available_space(4)/2, available_space(3)/2, available_space(4)/2 ]);
    % It is apparently not possible to make a set of axes clip so that
    % they don't appear behind other UI elements. Oh well.
    handle.ah = axes('Parent', handle.fh, 'position', [ 0.2 0.2 0.6 0.6 ]);
    handle.v3d = vol3d('cdata', volume, 'texture', '3d', 'Parent', handle.ah);
    % Tweak the figure:
    view(3);
    % Make it a cube:
    vsize = size(volume);
    % Swap x any y (for ... some reason)
    t = vsize(1);
    vsize(1) = vsize(2);
    vsize(2) = t;
    daspect(vsize / min(vsize));
    vol3d(handle.v3d);
    % Set an alphamap to make it possible to see inside:
    alphamap(logspace(0,2,64)/500);
    %alphamap('rampup');
    %alphamap(.06 .* alphamap);
    % Show the camera toolbar
    cameratoolbar(handle.fh, 'Show');
    % Set the projection to "perspective"
    camproj('perspective');
    % Turn rotate 3d on
    rotate3d on;
    % Generate sliders
    % The x slider controls
    handle.volume_size = size(handle.complete_volume);
    handle.x_figure = make_slice_figure('x', handle, available_space);
    handle.x_slider = make_slice_slider('x', handle,...
        handle.volume_size(1), [ 0.1 0.05 0.8 0.05 ]);
    handle.y_figure = make_slice_figure('y', handle, available_space);
    handle.y_slider = make_slice_slider('y', handle,...
        handle.volume_size(2), [ 0.9 0.1 0.05 0.8 ]);
    handle.z_figure = make_slice_figure('z', handle, available_space);
    handle.z_slider = make_slice_slider('z', handle,...
        handle.volume_size(3), [ 0.05 0.1 0.05 0.8 ]);
    handle.colormaps = {'jet','hsv','hot','cool','spring','summer','autumn','winter','gray','bone','copper','pink','lines'};
    handle.colormap_popup = uicontrol(handle.fh,'Style','popupmenu',...
                'String',handle.colormaps,...
                'Units', 'Normalized',...
                'Value',1,'Position',[0.05 0.9 0.25 0.1],...
                'Callback', {@set_colormap, handle});
    set(handle.fh, 'Visible', 'on');
    set(handle.x_figure.figure, 'Visible', 'on');
    set(handle.y_figure.figure, 'Visible', 'on');
    set(handle.z_figure.figure, 'Visible', 'on');
    delete(wbh);
catch err
    delete(wbh);
    rethrow(err);
end

end

function handle = make_slice_figure(type, parent, available_space)
    pos = [ available_space(3), available_space(4), available_space(3), available_space(4) ] / 2;
    switch (type)
        case 'x'
            % Do nothing
        case 'y'
            pos(1) = 1;
            pos(2) = 1;
        case 'z'
            pos(2) = 1;
    end
    handle.figure = figure('Name', ['Slice ' type], 'Visible', 'off',...
        'OuterPosition', pos);
    handle.image = imagesc(get_slice(type, 1, parent), [0 255]);
    colormap('jet');
end

function data = get_slice(type, slice, handle)
    % Extract the slice based on which "type" it is
    % x is x slice (show y,z)
    % y is y slice (show x,z)
    % z is z slice (show x,y)
    switch type
        case 'x'
            if slice >= 1 && slice <= handle.volume_size(1)
                data = squeeze(handle.complete_volume(slice,:,:));
            else
                data = [];
            end
        case 'y'
            if slice >= 1 && slice <= handle.volume_size(2)
                data = squeeze(handle.complete_volume(:,slice,:));
            else
                data = [];
            end
        case 'z'
            if slice >= 1 && slice <= handle.volume_size(2)
                data = squeeze(handle.complete_volume(:,:,slice));
            else
                data = [];
            end
        otherwise
            error(['Unexpected type ' type]);
    end
end

function show_slice(type, slice, handle)
    data = get_slice(type, slice, handle);
    if ~isempty(data)
        ih = handle.([type '_figure']);
        %fprintf('Showing %s slice %d\n', type, slice);
        set(ih.image, 'CData', data);
    else
        fprintf('Ignoring %s slice %d!\n', type, slice);
    end
end

function handle = make_slice_slider(type, parent, max, position)
    handle = uicontrol(parent.fh, 'Style','slider',...
        'Max',max,'Min',0,'Value',0,...
        'SliderStep',[1/max 10/max],...
        'Units', 'Normalized', 'Position', position,...
        'Callback', {@slider_changed, type, parent});
end

function slider_changed(hObj, ~, type, parent)
    slice = floor(get(hObj, 'Value') + 0.5);
    % Display the new slice
    show_slice(type, slice, parent);
end

function set_colormap(hObj, ~, parent)
    index = get(hObj, 'Value');
    cm = parent.colormaps{index};
    colormap(cm);
    cmap = colormap;
    set(parent.x_figure.figure, 'ColorMap', cmap);
    set(parent.y_figure.figure, 'ColorMap', cmap);
    set(parent.z_figure.figure, 'ColorMap', cmap);
end