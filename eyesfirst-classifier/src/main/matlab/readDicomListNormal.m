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

function fileArray = readDicomListNormal(fileArray,indexSet,outFileDir)
% curdir = pwd;
% cd(filedir)
% fileList = dir;
% fileList=fileList(3:end,1);
% Nfiles = length(fileList);
% %ws = ' ';
% %us = '_';
% dicomFileCt = 0;
% patientId = zeros(Nfiles,1);
% fileArray = struct;
Nreads = length(indexSet);
%sizeConstraint = [1024 512 128];
for ii = 1:Nreads
    curFileName = fileArray{indexSet(ii)}.base.name;
    curFileDir  = fileArray{indexSet(ii)}.base.dir; % full path name
    curFile = [curFileDir,filesep,curFileName];
    if isempty(outFileDir)
        outFileDir = curFileDir;
        outFileDirComp = outFileDir;
    else
        mkdir(curFileDir,outFileDir); % outFileDir is a subdirectory of the current directory
        outFileDirComp = [curFileDir,filesep,outFileDir];
    end;
    outFile = [outFileDirComp,filesep,curFileName,'_mat'];
    outFileName = [curFileName,'_mat'];
    if isempty(fileArray{indexSet(ii)}.mat.name)
       curFileWext = [curFile,'.dcm'];
       A = squeeze(dicomread(curFileWext));
       % If the image isn't in the expected size, resize to match
       % FIXME: We shouldn't need to do this
       %[height, width, slices] = size(A);
       %if height ~= 1024 || width ~= 512
       %    % We need to resize
       %    B = zeros(1024, 512, slices);
       %    for idx = 1:slices
       %        B(:,:,idx) = imresize(A(:,:,idx), [ 1024, 512 ]);
       %    end
       %    A = B;
       %end
       % For now, fake this:
       metadata = dicominfo(curFileWext);
       % Try and pull the data out of the DICOM file
       pms = findPixelMeasuresSequence(metadata);
       if ~isempty(pms)
           pixelDim.axial = pms.PixelSpacing(1) * 1000;
           pixelDim.fastTime = pms.PixelSpacing(2) * 1000;
           pixelDim.slowTime = pms.SliceThickness * 1000;
       elseif isfield(metadata, 'Manufacturer')
           if strcmp(metadata.Manufacturer, 'Carl Zeiss Meditec') == 1
               pixelDim.axial = 1.9531;
               pixelDim.fastTime = 11.7188;
               pixelDim.slowTime = 46.875;
           elseif strcmp(metadata.Manufacturer, 'Heidelberg Engineering') == 1
               pixelDim.axial = 3.87;
               pixelDim.fastTime = 5.71;
               pixelDim.slowTime = 61;
           else
               warning('EyesFirst:NoPixelDim', ['Unknown manufacturer "', metadata.Manufacturer, '", using Carl Zeiss dimensions!']);
               pixelDim.axial = 1.9531;
               pixelDim.fastTime = 11.7188;
               pixelDim.slowTime = 46.875;
           end
       else
           warning('EyesFirst:NoPixelDim', 'Unable to locate PixelMeasuresSequence or a manufacturer, using Carl Zeiss dimensions!');
           pixelDim.axial = 1.9531;
           pixelDim.fastTime = 11.7188;
           pixelDim.slowTime = 46.875;
       end
       fprintf('Using pixel dimensions: %e, %e, %e\n', pixelDim.axial, pixelDim.fastTime, pixelDim.slowTime);
       save(outFile,'A','pixelDim');
       [aa,bb,cc] = size(A);
      % if aa == sizeConstraint(1) && bb == sizeConstraint(2) && cc == sizeConstraint(3)
      %     sizeConstraintFlag = 1;
      % else
      %     sizeConstraintFlag = 0;
      % end

       %sizeConstraintFlag = 1;
       fileArray{indexSet(ii)}.mat.name = outFileName;
       fileArray{indexSet(ii)}.mat.dir = outFileDirComp;
       fileArray{indexSet(ii)}.mat.multiples = 0;
       %fileArray{indexSet(ii)}.base.sizeConstraint = sizeConstraintFlag;
    end
end
end

function pms = findPixelMeasuresSequence(metadata)
   % PMS is a child of SharedFunctionalGroupsSequence
   if isfield(metadata, 'SharedFunctionalGroupsSequence')
       % Go through the fields inside this
       sfgs = metadata.SharedFunctionalGroupsSequence;
       fields = fieldnames(sfgs);
       for i=1:numel(fields)
           % Check to see if this item contains PixelMeasuresSequence
           if isfield(sfgs.(fields{i}), 'PixelMeasuresSequence')
               % Found it - just return whatever the first item is
               item = sfgs.(fields{i}).PixelMeasuresSequence;
               itemFields = fieldnames(item);
               pms = item.(itemFields{1});
               return;
           end
       end
   end
   pms = [];
end