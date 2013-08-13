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

function imageThickMap(fileDir,statFile,mapFile,ofile,pixelDim)
% creates an image of the thickness map superimposes the polar sectors
% and the mean values of each sector.  The output image is stored in ofile.
stats = load([fileDir,filesep,statFile]);
maps = load([fileDir,filesep,mapFile]);

thickMapAve = (maps.thickMap1+maps.thickMap2)/2;
% figh = figure;
% imagesc(thickMapAve);
cp = stats.thicknessFeatures.cp;

boundaryCurves.radii = [500 1500 3000];
boundaryCurves.rays = [pi/4 3*pi/4 -3*pi/4, -pi/4];
imageDim = size(thickMapAve);
di = pixelDim.slowTime; % microns per pixel slow time 
dj = pixelDim.fastTime; % microns per pixel fast time
bdryIm = polarSectorBoundaries(cp,boundaryCurves,imageDim,di,dj);
[I,J] = find(bdryIm);
maxTMA = max(max(thickMapAve));
for kk = 1:length(I)
   thickMapAve(I(kk),J(kk)) = maxTMA;
end;
figure;
imagesc(thickMapAve);
set(gca, 'XTick', []);
set(gca, 'YTick', []);
axis square;
% Remove the figure borders:
set(gca,'units','pixels'); % set the axes units to pixels
x = get(gca,'position'); % get the position of the axes
% We want this to be square, so:
w = max(x(3:4));
set(gcf,'units','pixels'); % set the figure units to pixels
y = get(gcf,'position'); % get the figure position
set(gcf,'position',[y(1) y(2) w w]); % set the position of the figure to the length and width of the axes
set(gca,'units','normalized','position',[0 0 1 1]); % set the axes units to pixels
set(gcf,'PaperUnits','inches','PaperPosition',[0 0 4 4])
 %text(250,50,'ABC','color','w','fontsize',18)
Nregions = numel(stats.thicknessFeatures.stats);

% Font size to use
fontSize = 18;

for ii = 1:Nregions-1
    meanI = round(mean(stats.thicknessFeatures.stats{ii}.I));
    meanJ = round(mean(stats.thicknessFeatures.stats{ii}.J));
    meanVal = int2str(round(mean(stats.thicknessFeatures.stats{ii}.meanThickAve)));
    % Draw a little text shadow to make things easier to see
    shadow = text(meanJ,meanI,meanVal,'color','k','fontsize',fontSize,'HorizontalAlignment','center','VerticalAlignment','middle');
    % Then draw the actual text
    mainText = text(meanJ,meanI,meanVal,'color','w','fontsize',fontSize,'HorizontalAlignment','center','VerticalAlignment','middle');
    % Move the shadow based on the position of the main text
    set(mainText, 'units', 'pixels');
    p = get(mainText, 'position');
    p = p + [2, -2, 0];
    set(shadow, 'units', 'pixels');
    set(shadow, 'position', p);
end;
print(gcf, ofile, '-dpng', '-r100');
