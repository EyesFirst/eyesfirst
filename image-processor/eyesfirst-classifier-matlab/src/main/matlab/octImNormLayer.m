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

function [cfarSAA,curx] = octImNormLayer(SAA,Bdrys,refPar,curx)
%
% normalizes sliceInd

[aa,bb,cc] = size(SAA);
cfarSAA = NaN*ones(size(SAA));
% meanSAA = NaN*ones(size(SAA));
% stdSAA = NaN*ones(size(SAA));
% meanSAA = [];
% stdSAA = [];
% extract fastTime domain

 for kk = 1:cc
     fprintf('STATUS:{"message":"Running CFAR process (layer %d/%d)..."}\n', kk, cc);
     for jj = 1:bb
      %   tic
       iill = floor(Bdrys.floor(jj,kk));
       iiul = ceil(Bdrys.top(jj,kk));
       for ii = iill:iiul
           [refSet1,refSet2,refSet3,refSet4] = refInBoundsLayer(ii,jj,kk,refPar,Bdrys);
          % refData = [];
           if ~isempty(refSet1) 
               Ndata1 = (refSet1.I(:,:,2)-refSet1.I(:,:,1)) + 1;
               totNdata1 = sum(sum(Ndata1));
               refData1 = zeros(totNdata1,1);
               curInd1 = 0;
               for rr = 1:length(refSet1.J)
                   for ss = 1:length(refSet1.K)
                       nndata = Ndata1(rr,ss);
                       if nndata > 0
                          refData1(curInd1+1:curInd1+nndata) = reshape(SAA(refSet1.I(rr,ss,1):refSet1.I(rr,ss,2),refSet1.J(rr),refSet1.K(ss)),nndata,1);
                          curInd1 = curInd1 + nndata;
                       end;
                   end
               end;  
           else
               refData1 = [];
           end;
           
           if ~isempty(refSet2) 
               Ndata2 = (refSet2.I(:,:,2)-refSet2.I(:,:,1)) + 1;
               totNdata2 = sum(sum(Ndata2));
               refData2 = zeros(totNdata2,1);
               curInd2 = 0;
               for rr = 1:length(refSet2.J)
                   for ss = 1:length(refSet2.K)
                       nndata = Ndata2(rr,ss);
                       if nndata > 0
                          refData2(curInd2+1:curInd2+nndata) = reshape(SAA(refSet2.I(rr,ss,1):refSet2.I(rr,ss,2),refSet2.J(rr),refSet2.K(ss)),nndata,1);
                          curInd2 = curInd2 + nndata;
                       end;
                   end
               end;
           else
               refData2 = [];
           end;
           
            if ~isempty(refSet3) 
               Ndata3 = (refSet3.I(:,:,2)-refSet3.I(:,:,1)) + 1;
               totNdata3 = sum(sum(Ndata3));
               refData3 = zeros(totNdata3,1);
               curInd3 = 0;
               for rr = 1:length(refSet3.J)
                   for ss = 1:length(refSet3.K)
                       nndata = Ndata3(rr,ss);
                       if nndata > 0
                          refData3(curInd3+1:curInd3+nndata) = reshape(SAA(refSet3.I(rr,ss,1):refSet3.I(rr,ss,2),refSet3.J(rr),refSet3.K(ss)),nndata,1);
                          curInd3 = curInd3 + nndata;
                       end;
                   end
               end;  
            else
                refData3 = [];
           end;
           if ~isempty(refSet4) 
               Ndata4 = (refSet4.I(:,:,2)-refSet4.I(:,:,1)) + 1;
               totNdata4 = sum(sum(Ndata4));
               refData4 = zeros(totNdata4,1);
               curInd4 = 0;
               for rr = 1:length(refSet4.J)
                   for ss = 1:length(refSet4.K)
                       nndata = Ndata4(rr,ss);
                       if nndata > 0
                          refData4(curInd4+1:curInd4+nndata) = reshape(SAA(refSet4.I(rr,ss,1):refSet4.I(rr,ss,2),refSet4.J(rr),refSet4.K(ss)),nndata,1);
                          curInd4 = curInd4 + nndata;
                       end;
                   end
               end;    
           else
               refData4 = [];
           end;
           refData = [refData1; refData2; refData3; refData4];
           if ~isempty(refData)
               curMean = mean(refData);
               curStd = std(refData);
%                meanSAA(ii,jj,kk) = curMean;
%                stdSAA(ii,jj,kk) = curStd;
               cfarSAA(ii,jj,kk) = (SAA(ii,jj,kk)-curMean)/curStd;
           else
%                meanSAA(ii,jj,kk) = 0;
%                stdSAA(ii,jj,kk) = 0;
               cfarSAA(ii,jj,kk) = -25;
           end;
      end;
   %   toc
    end;
end
% %fprintf('made it\n');
% ofile = [ofileBase,'_cfar'];
% save(ofile,'cfarSAA','meanSAA','stdSAA','curx');

                 
                 
            
        

