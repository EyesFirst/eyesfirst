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
       save(outFile,'A');
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
    end;
end;
