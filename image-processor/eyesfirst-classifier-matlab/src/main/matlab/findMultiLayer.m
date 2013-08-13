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

function [csRelTop,csRelIm,BdryRelTop,BdryRelIm] = findMultiLayer(cf,imfloor,imtop,masv,maxRangeCf,Nlayers,maxInterLayerDist,minInterLayerDist,ofile);
% function fl = findLayer(im,cf);
% uses graph cut method to identify layer of image, im, between imfloor and
% imtop.
% cf is the pixel cost function and mas is the maximum absolute value of
% the slope (in pixels). note that mas can be a vector 
% maxInterLayerDist and minInterLayerDist are lengthX(Nlayer-1) matrices
% such that max(min)InterLayerDist(ii,jj) = maximum (minimum)distance between layer ii
% and layer ii+1 at xvalue jj.

[aa,bb] = size(cf);
basePixInd = zeros(bb,Nlayers);
topPixInd = zeros(bb,Nlayers);

ctPix = 0;
imfloor2 = zeros(bb,Nlayers);
imtop2 = zeros(bb,Nlayers);
for ii = 1:bb
    for jj = 1:Nlayers
        topPixInd(ii,jj) = ctPix+1;
      %  imfloor2(ii,jj) = imfloor(ii) - (Nlayers-jj)*minInterLayerDist(ii,jj);
        if jj > 1
            imtop2(ii,jj) = imtop(ii)+ sum(minInterLayerDist(ii,1:jj-1));
        elseif jj == 1
            imtop2(ii,jj) = imtop(ii);
        end;
        if jj == Nlayers
            imfloor2(ii,jj) = imfloor(ii);
        else
            imfloor2(ii,jj) = imfloor(ii)- sum(minInterLayerDist(ii,jj:Nlayers-1));
        end;
%         curImTop = imtop(ii)-(jj-1)*minInterLayerDist(ii);
%         curImFloor = imfloor(ii) - (Nlayers-jj)*minInterLayerDist(ii);
        ctPix = ctPix + imfloor2(ii,jj)-imtop2(ii,jj) + 1;
        basePixInd(ii,jj) = ctPix;
    end;
end;
wf = zeros(ctPix,1);
fi = 0;
li = 0;

% cf must consist of integer values
cf = round((cf/max(max(abs(cf))))*maxRangeCf);
% normalize so that base has value -1
% lastLayerBase = basePixInd(:,Nlayers);

ctEdges = 0;
edgeMat = zeros(ctPix,5);  % contains the end of each edge eminating from the given vertex
for ii = 1:bb
    for jj = 1:Nlayers
        mas = masv(jj);
        fi = li+1; % first index
        li = fi + imfloor2(ii,jj)-imtop2(ii,jj); % last index
        if imfloor2(ii,jj) < imfloor(ii)
          ccfe = cf(imtop2(ii,jj):imfloor2(ii,jj)+1,ii);
          floorCost = cf(imfloor(ii),ii);
          ccfe = ccfe-(floorCost+1); % set floor to a value of -1;
          wcf = ccfe(1:end-1)-ccfe(2:end);
        else
          ccf = cf(imtop2(ii,jj):imfloor2(ii,jj),ii);
          floorCost = cf(imfloor(ii),ii);
          ccf = ccf-(floorCost+1); % set floor to a value of -1;
          wcf = [ccf(1:end-1);0]-[ccf(2:end);1];
        end;
        wf(fi:li) = wcf;
        NpixCur = li-fi+1;
        locPixInd = 1:NpixCur;
        % downward edges
        edgeMat(fi:li-1,1) = [fi+1:1:li]'; 
        ctEdges = ctEdges +(li-fi);
        % rightward edges
        if ii ~= bb
            rightwardPixTop = topPixInd(ii+1,jj);
            rightwardPixBase = basePixInd(ii+1,jj);
            rightPix = rightwardPixTop:rightwardPixBase;
            NpixRight = length(rightPix);
            rightEdgeTermLocInd = min(ceil(locPixInd*(NpixRight/NpixCur)+ mas),NpixRight);
            rightEdgeTermLocInd = max(rightEdgeTermLocInd, ones(size(rightEdgeTermLocInd)));
            rightEdgeTermInd = rightPix(rightEdgeTermLocInd);
            edgeMat(fi:li,2) =  rightEdgeTermInd';   % rightward pointing edge
            ctEdges = ctEdges +(li-fi)+1;
        end;
        % leftward edges
        if ii ~= 1
            leftwardPixTop = topPixInd(ii-1,jj);
            leftwardPixBase = basePixInd(ii-1,jj);
            leftPix = leftwardPixTop:leftwardPixBase;
            Npixleft = length(leftPix);
            leftEdgeTermLocInd = min(ceil(locPixInd*(Npixleft/NpixCur)+ mas),Npixleft);
            leftEdgeTermInd = leftPix(leftEdgeTermLocInd);
            edgeMat(fi:li,3) =  leftEdgeTermInd';   % leftward pointing edge
            ctEdges = ctEdges +(li-fi)+1;
        end;
        % next layer edges
        if jj ~= Nlayers
            nextLayerPixTop = topPixInd(ii,jj+1);
            nextLayerPixBase = basePixInd(ii,jj+1);
            nextLayerPix = nextLayerPixTop:nextLayerPixBase;
            NpixnextLayer = length(nextLayerPix);
            maxildist = maxInterLayerDist(ii,jj);
            minildist = minInterLayerDist(ii,jj);
            nextLayerEdgeTermLocInd = min(ceil(locPixInd*(NpixnextLayer/NpixCur)+ maxildist-minildist),NpixnextLayer);
            nextLayerEdgeTermInd = nextLayerPix(nextLayerEdgeTermLocInd);
            edgeMat(fi:li,4) =  nextLayerEdgeTermInd';   % next layer pointing edge
            ctEdges = ctEdges +(li-fi)+1;
        end;
        % previous layer edges
        if jj ~= 1
            previousLayerPixTop = topPixInd(ii,jj-1);
            previousLayerPixBase = basePixInd(ii,jj-1);
            previousLayerPix = previousLayerPixTop:previousLayerPixBase;
            NpixpreviousLayer = length(previousLayerPix);
           % minildist = minInterLayerDist(ii,jj-1);
            previousLayerEdgeTermLocInd = max(ceil(locPixInd*(NpixpreviousLayer/NpixCur)),1);
            previousLayerEdgeTermInd = previousLayerPix(previousLayerEdgeTermLocInd);
            edgeMat(fi:li,5) =  previousLayerEdgeTermInd';   % previous layer pointing edge
            ctEdges = ctEdges +(li-fi)+1;
        end;
    end;
end;
% now create the sparse matrix

% 
baseVal = wf(basePixInd(:,Nlayers));
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


I4 = find(edgeMat(:,4) ~= 0);
J4 = edgeMat(I4,4);
CI4 = intArcVal*ones(length(I4),1);
I5 = find(edgeMat(:,5) ~= 0);
J5 = edgeMat(I5,5);
CI5 = intArcVal*ones(length(I5),1);


IndexS = ctPix+1;
IndexT = ctPix+2;
IS = IndexS*ones(Nneg,1);
JS = Ineg;
CS = -wf(Ineg);
IT = Ipos;
JT = IndexT*ones(Npos,1);
CT = wf(Ipos);

Isp = [I1;I2;I3;I4;I5;IS;IT];
Jsp = [J1;J2;J3;J4;J5;JS;JT];
Csp = [CI1;CI2;CI3;CI4;CI5;CS;CT];
graphMat = sparse(Isp,Jsp,Csp,ctVertex,ctVertex,ctEdges);
[flowVal,Cut,R,F] = max_flow(graphMat,IndexS,IndexT);
mcs = find(Cut == 1);
Bdry = zeros(bb,Nlayers);
BdryRelTop = zeros(bb,Nlayers);
BdryRelIm = zeros(bb,Nlayers);
cmcs = mcs;
csRelIm = zeros(aa,bb,Nlayers);
csRelTop = cell(bb,Nlayers);
for hh = 1:Nlayers
    for ii = 1:bb
        curCol = find(cmcs >= topPixInd(ii,hh) & cmcs <= basePixInd(ii,hh));
        Bdry(ii,hh) = min(cmcs(curCol));
        BdryRelTop(ii,hh) = Bdry(ii,hh) - topPixInd(ii,hh) + 1;  % index relative to top pixel
        BdryRelIm(ii,hh) = BdryRelTop(ii,hh) + imtop2(ii,hh) - 1;    % index relative to the image dimensions
        csRowIndRelIm = cmcs(curCol) - topPixInd(ii,hh) + imtop2(ii,hh); % row indices relative to the image
        csRelIm(csRowIndRelIm,ii,hh) = ones(length(csRowIndRelIm),1);
        csRelTop{ii,hh} = cmcs(curCol) - topPixInd(ii,hh) + 1;  % closed surface indices relative to the top
        cmcs(curCol) = zeros(size(curCol));
        II1 = cmcs ~= 0;
        cmcs = cmcs(II1);
    end
end;
if ~isempty(ofile)
    save(ofile);
end;
