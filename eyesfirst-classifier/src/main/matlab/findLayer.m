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

function [csRelTop,csRelIm,BdryRelTop,BdryRelIm] = findLayer(cf,imfloor,imtop,mas,maxRangeCf,ofile)
% function fl = findLayer(im,cf);
% uses graph cut method to identify layer of image, im, between imfloor and
% imtop.
% cf is the pixel cost function and mas is the maximum absolute value of
% the slope (in pixels). note that mas can be a vector 

[aa,bb] = size(cf);
basePixInd = zeros(bb,1);
topPixInd = zeros(bb,1);
if  bb == length(imfloor) && bb == length(imtop) && (bb == length(mas) || length(mas) == 1)
    ctPix = 0;
    for jj = 1:bb
        topPixInd(jj) = ctPix+1;
        ctPix = ctPix + imfloor(jj)-imtop(jj) + 1;
        basePixInd(jj) = ctPix;
    end;
end;
wf = zeros(ctPix,1);
fi = 0;
li = 0;

% cf must consist of integer values
cf = round((cf/max(max(abs(cf))))*maxRangeCf);
ctEdges = 0;
edgeMat = zeros(ctPix,3);  % contains the end of each edge eminating from the given vertex
for ii = 1:bb
    fi = li+1;
    li = fi + imfloor(ii)-imtop(ii);
    ccf = cf(imtop(ii):imfloor(ii),ii);
    ccf = ccf-(ccf(end)+1); % set floor to a value of -1;
    wcf = [ccf(1:end-1);0]-[ccf(2:end);1];
    wf(fi:li) = wcf;
    NpixCur = li-fi+1;
    locPixInd = 1:NpixCur;
    edgeMat(fi:li-1,1) = [fi+1:1:li]'; % downward edges
    ctEdges = ctEdges +(li-fi);
    if ii == 1 
        ctEdges = ctEdges +(li-fi) + 1;
        rightPix = [topPixInd(2):basePixInd(2)];
        NpixRight = length(rightPix);
        rightEdgeTermLocInd = min(ceil(locPixInd*(NpixRight/NpixCur)+ mas),NpixRight);
        rightEdgeTermInd = rightPix(rightEdgeTermLocInd);
        edgeMat(fi:li,2) =  rightEdgeTermInd';   % rightward pointing edge
    elseif ii == bb
        ctEdges = ctEdges + (li-fi) + 1;
        leftPix = [topPixInd(bb-1):basePixInd(bb-1)];
        NpixLeft = length(leftPix);
        leftEdgeTermLocInd = min(ceil(locPixInd*(NpixLeft/NpixCur)+ mas),NpixLeft);
        leftEdgeTermInd = leftPix(leftEdgeTermLocInd);
        edgeMat(fi:li,2) =  leftEdgeTermInd';   % leftward pointing edge
    else
        ctEdges = ctEdges + 2*(li-fi) + 2;
        rightPix = [topPixInd(ii+1):basePixInd(ii+1)];
        NpixRight = length(rightPix);
        rightEdgeTermLocInd = min(ceil(locPixInd*(NpixRight/NpixCur)+ mas),NpixRight);
        rightEdgeTermInd = rightPix(rightEdgeTermLocInd);
        edgeMat(fi:li,2) =  rightEdgeTermInd';   % rightward pointing edge
        leftPix = [topPixInd(ii-1):basePixInd(ii-1)];
        NpixLeft = length(leftPix);
        leftEdgeTermLocInd = min(ceil(locPixInd*(NpixLeft/NpixCur)+ mas),NpixLeft);
        leftEdgeTermInd = leftPix(leftEdgeTermLocInd);
        edgeMat(fi:li,3) =  leftEdgeTermInd';   % leftward pointing edge
    end;
end;
baseVal = wf(basePixInd);
sabv = sum(abs(baseVal));
Ineg = find(wf < 0);
Nneg = length(Ineg);
Ipos = find(wf > 0);
Npos = length(Ipos);
TAneg = -sum(wf(Ineg));
Tpos = sum(wf(Ipos));
intArcVal = TAneg-sabv+1;
ctEdges = ctEdges+Nneg+Npos;
ctVertex = ctPix+2;
I1 = find(edgeMat(:,1) ~= 0);
J1 = edgeMat(I1,1);
CI1 = intArcVal*ones(length(I1),1);
I2 = find(edgeMat(:,2) ~= 0);
J2 = edgeMat(I2,2);
CI2 = intArcVal*ones(length(I2),1);
I3 = find(edgeMat(:,3) ~= 0);
J3 = edgeMat(I3,3);
CI3 = intArcVal*ones(length(I3),1);
IndexS = ctPix+1;
IndexT = ctPix+2;
IS = IndexS*ones(Nneg,1);
JS = Ineg;
CS = -wf(Ineg);
IT = Ipos;
JT = IndexT*ones(Npos,1);
CT = wf(Ipos);

Isp = [I1;I2;I3;IS;IT];
Jsp = [J1;J2;J3;JS;JT];
Csp = [CI1;CI2;CI3;CS;CT];
graphMat = sparse(Isp,Jsp,Csp,ctVertex,ctVertex,ctEdges);
[flowVal,Cut,R,F] = max_flow(graphMat,IndexS,IndexT);
mcs = find(Cut == 1);
Bdry = zeros(1,bb);
BdryRelTop = zeros(1,bb);
BdryRelIm = zeros(1,bb);
cmcs = mcs;
csRelIm = zeros(aa,bb);
csRelTop = cell(bb,1);
for ii = 1:bb
    curCol = find(cmcs >= topPixInd(ii) & cmcs <= basePixInd(ii));
    Bdry(ii) = min(cmcs(curCol));
    BdryRelTop(ii) = Bdry(ii) - topPixInd(ii) + 1;  % index relative to top pixel
    BdryRelIm(ii) = BdryRelTop(ii) + imtop(ii) - 1;    % index relative to the image dimensions
    csRowIndRelIm = cmcs(curCol) - topPixInd(ii) + imtop(ii); % row indices relative to the image
    csRelIm(csRowIndRelIm,ii) = ones(length(csRowIndRelIm),1);
    csRelTop{ii} = cmcs(curCol) - topPixInd(ii) + 1;  % closed surface indices relative to the top
    cmcs(curCol) = zeros(size(curCol));
    II1 = find(cmcs ~= 0);
    cmcs = cmcs(II1);
end
if ~isempty(ofile)
    save(ofile);
end;



            
    

