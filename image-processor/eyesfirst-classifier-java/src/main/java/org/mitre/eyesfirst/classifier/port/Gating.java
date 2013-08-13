/*
 * Copyright 2013 The MITRE Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.mitre.eyesfirst.classifier.port;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.util.FastMath;

/**
 * The gating algorithm. Note: this class is not thread-safe. (Some state is
 * stored in internal variables.)
 * @author dpotter
 *
 */
public class Gating {

	public Gating() {
		// TODO Auto-generated constructor stub
	}

	public void runGating() {
		
	}

	/**
	 * Run gating on the given image cube using the default edf and ndil.
	 * @param cube
	 */
	public void initialLayerId(ImageCube cube) {
		initialLayerId(cube, 5.0, 4);
	}

	public void initialLayerId(ImageCube cube, double edf, int numberDilations) {
		//double max = cube.findMax();
		SemClassify.Stat[] initcl = new SemClassify.Stat[] {
			new SemClassify.Stat(20, 16),
			new SemClassify.Stat(60, 25)
		};
		double[] initclpd = new double[] { 0.5, 0.5 };
		int maxNumIt = 12;
		// FIXME: This does 255*0.75*SAA/maxval at this point,
		// thereby normalizing the data to a range of 0-191.25 (for some reason).
		// This step needs to be duplicated, but doing this over the entire cube
		// right now seems silly. (Although it may be less silly later.)
		//initBdryIm = (255*sf)*(SAA/maxval);
		final int width = cube.getWidth();
		final int height = cube.getHeight();
		final int depth = 1;//cube.getDepth();
		for (int i = 0; i < depth; i++) {
			System.out.println("Running gating layer " + (i+1) + "/" + depth);
			// Apparently what the MATLAB code does is pull out all values that
			// are greater than 0 and stick them into an array
			byte[] data = findNonZero(cube.getSlice(i), width, height);
			SemClassify semclassify = new SemClassify();
			semclassify.semClassify(data, data, initcl, initclpd, maxNumIt);
			//[clstat,clpd,cclmap,ppmclf,maclmap,stclest,maxclMapInd]=semclassify(curimv2,curimv2,initcl,initclpd,maxNumIt);
			initclpd = semclassify.getClpd();
			initcl = semclassify.getClstat();
			byte[] maxclMapInd = semclassify.getMaclIndMap();
			maxNumIt = 5;
			// imv = zeros(aa*bb,1);
			byte[] imv = new byte[cube.getWidth() * cube.getHeight()];
			// imv(I1) = maxclMapInd;
			for (int j = 0; j < maxclMapInd.length; j++) {
				// The MATLAB code had to map the indices to 0/1. In Java, they
				// already are 0/1, so just copy them directly.
				imv[nonZeroIndices[j]] = maxclMapInd[j];
			}
			// We also can skip the reshape because we just play with indices
			// directly to do that.
			//showDebugImage("Pre-Erosion", imv, width, height);
			// Now erode the image.
			ImageUtil.Kernel sel = new ImageUtil.Kernel(5, 5);
			imv = ImageUtil.binaryErode(imv, width, height, sel);
			//showDebugImage("Eroded", imv, width, height);
			for (int j = 0; j < numberDilations; j++) {
				imv = ImageUtil.binaryDilate(imv, width, height, sel);
			}
			//showDebugImage("Dilated", imv, width, height);
			// generate pixel density map for computation of gradient image
			double[] imv_e_d_20_3 = pixelDensityMap(imv, width, height, 20, 3, PixelDensityOp.UC, 0.5);
			double[] imv_e_d_20_3_fr = filterRows(imv_e_d_20_3, width, height);

			// compute the gradient image, returned in gim, and if g2flag == 1, the derivative of the
			// gradient image in the direction of the gradient, returned in g2im
			// [gim,g2im] = gradientImage2d(imv_e_d_20_3_fr,3,4,'imd_e_d_20_3');
			gradientImage2d(imv_e_d_20_3_fr, width, height, 3, 4);
		}
	}

	private int[] nonZeroIndices;

	private byte[] findNonZero(byte[] slice, int width, int height) {
		// Count the number of entries we have
		int count = 0;
		for (int i = 0; i < slice.length; i++) {
			if (slice[i] != 0)
				count++;
		}
		byte[] result = new byte[count];
		nonZeroIndices = new int[count];
		// It looks like the MATLAB code goes by row, then column, so start
		// by looping through columns and then rows
		int ri = 0;
		for (int c = 0; c < width; c++) {
			for (int r = 0; r < height; r++) {
				int index = r * width + c;
				byte b = slice[index];
				if (b != 0) {
					result[ri] = b;
					nonZeroIndices[ri] = index;
					ri++;
				}
			}
		}
		/*
		for (int i = 0, ri = 0; i < slice.length; i++) {
			if (slice[i] != 0) {
				result[ri] = slice[i];
				ri++;
			}
		}
		*/
		return result;
	}
/*
 * function  [polyGate,SAATB] =  initialLayerId_4b_v2(SAA,ofile,edf,ndil,sliceInd)
[aa,bb,cc] = size(SAA);
maxNumLayers = 10;
minmax = 1.0e-4;
minLowerGateVal = 24;
imageGate = zeros(2,bb,cc);
initLayers = zeros(maxNumLayers,6,bb,cc);
maxval = max(max(max(SAA)));
sf = .75;
plotflag = 0;
initBdryIm = (255*sf)*(SAA/maxval);
initclpd = [0.5 0.5]';
maxNumIt = 12;
initcl{1}.mean = 20;
initcl{1}.covm = 16;
initcl{2}.mean = 60;
initcl{2}.covm = 25;
plotFlag0 = 0;
% figh1 = figure;
% figh2 = figure;
% figTB1 = figure;
% figTBOR = figure;
% fighTBim = figure;
% figDens = figure;
% figMorph = figure;
% figSem = figure;
%gateInd = zeros(2,bb,cc);
polyGate = cell(cc,1);
SAATB = zeros(size(SAA));
maxjumpThresh = 50;
highStatePixFracThresh = 0.1;
nullGateVal = 5;
%for ii = 1:cc
    
%sliceInd provides start and end indices of slices to be processed
for ii = sliceInd(1):sliceInd(2)
    fprintf('STATUS:{"message":"Running gating (layer %d/%d)..."}\n', ii, sliceInd(2));
    curim = squeeze(SAA(:,:,ii));
    curimv = curim(:);
    I1 = find(curimv > 0);
    curimv2 = curimv(I1)';
    
    %Run sem classification algorithm to fit gaussian mixture model
    [clstat,clpd,cclmap,ppmclf,maclmap,stclest,maxclMapInd]=semclassify(curimv2,curimv2,initcl,initclpd,maxNumIt);
    initclpd = clpd;
    initcl = clstat;
    maxNumIt = 5;
    imv = zeros(aa*bb,1);
    imv(I1) = maxclMapInd;
    imv_bin = imv;
    I1 = find(imv == 1);
    imv_bin(I1) = zeros(size(I1));
    I2 = find(imv == 2);
    imv_bin(I2) = ones(size(I2));
    imv_bin = reshape(imv_bin,aa,bb);
    %figure(figSem);imagesc(imv_bin);title('intensity classification')
    
    %define structured element for erosion and dilation
    sel = strel('rectangle',[edf,edf]);
    
    %apply image erosion to reduce noise in vitreous
    imv_e_d = imerode(imv_bin,sel);
    %figure(figMorph);imagesc(imv_e_d);title('image erosion');
 
    for ll = 1:ndil
       %apply binary image dialation to reduce noise in retina produced
       %during erosion
       imv_e_d = imdilate(imv_e_d,sel);
    end;
    %imv_e_d = imdilate(imv_e_d1,sel);
    %figure(figMorph);imagesc(imv_e_d);title('image dilation');
  
    %generate pixel density map for computation of gradient image
    imv_e_d_20_3 = pixelDensityMap(imv_e_d,20,3,'UC',.5);
    imv_e_d_20_3_fr = filterRows(imv_e_d_20_3);
    %figure;imagesc(imv_e_d_20_3_fr)
    %figure(figDens);imagesc(imv_e_d_20_3_fr); title('pixel density');
  
    %compute the gradient image, returned in gim, and if g2flag == 1, the derivative of the
    %gradient image in the direction of the gradient, returned in g2im
    [gim,g2im] = gradientImage2d(imv_e_d_20_3_fr,3,4,'imd_e_d_20_3');
    FirstNonEmptyNonNullGate = 0;
    LastEmptyGate = 0;
    for jj = 1:bb
        %if jj == 330
        %     fprintf('jj = \n',jj)
        %end;
        curvec2 = g2im(:,jj);
        curvec1 = gim(:,jj);
        if max(max(curvec2)) > minmax && max(max(curvec1)) > minmax 
            mt2 = 0.1*max(curvec2);
            mt1 = 0.1*max(curvec1);
            ppg = spaps([1:aa],curvec1,mt1);
            % pp2 = spaps([1:aa],curvec2,mt2);
            %find maxima in gradient as bounds of relevant region of an A
            %scan
           gate = extractGate3(ppg,plotFlag0,[]);
            if ~isempty(gate) && min(gate) > nullGateVal
               imageGate(:,jj,ii) = gate';
               if FirstNonEmptyNonNullGate == 0
                   FirstNonEmptyNonNullGate = jj;
                   % fill in the first empties
                   if FirstNonEmptyNonNullGate > 1
                      imageGate(:,1:FirstNonEmptyNonNullGate-1,ii) = repmat(gate',1,FirstNonEmptyNonNullGate-1);
                   end;
               end
            else
                if FirstNonEmptyNonNullGate ~= 0
                    imageGate(:,jj,ii) = imageGate(:,jj-1,ii);
                end;
            end
%             figure(figh1);
%             hold off
        else
            if FirstNonEmptyNonNullGate ~= 0
                imageGate(:,jj,ii) = imageGate(:,jj-1,ii);
            end;
        end;
%           cv = imv_e3_d3_20_3(:,jj);
%           mt1 = 0.1*max(cv);
%           pp1 = spaps([1:aa],cv,mt1);
%           pp1val = fnval(pp1,[1:aa]);
%           Ipo = find(pp1val > 0);
%           if length(Ipo) >= 2
%              gateInd(:,jj,ii) = [Ipo(1) Ipo(end)]';
%              initBdryIm([Ipo(1) Ipo(end)],jj,ii) = 255*ones(2,1);
%           end;  
    end
    if plotflag == 1
        figure(figh2)
        imagesc(squeeze(initBdryIm(:,:,ii)));colorbar;title(['initial layers slice ',int2str(ii)]);
    end;
    curGate = squeeze(imageGate(:,:,ii));
    curGateLower = curGate(1,:);
    curGateUpper = curGate(2,:);
    
    LGNN = find(curGateLower > 0);
    %extract jumps between adjacent gates
    lowerGateJumps = curGateLower(LGNN(1:end-1))-curGateLower(LGNN(2:end));
    IUJa = find(lowerGateJumps <= -maxjumpThresh); % jump up
    IDJa = find(lowerGateJumps >= maxjumpThresh);  % jump down
    [IUJ,IDJ] = sandwich(IUJa,IDJa);
    
    %if there are no jumps, no extrapolation necessary
    if isempty(IUJ) && isempty(IDJ)
        imposeBound = 0;
        extrapLowerGate = curGateLower;
    else %there are jumps, extrapolation is necessary based on jumps
        extrapLowerGate = curGateLower;
        %if there are only upward jumps
        if ~isempty(IUJ) && isempty(IDJ)
            %start low and one jump up
            if IUJ(1) > 1
                curSlope = curGateLower(LGNN(IUJ(1))) - curGateLower(LGNN(IUJ(1)-1));
            else
                curSlope = 0;
            end;
            highStateRuns = [LGNN(IUJ(1)+1) LGNN(end) LGNN(IUJ(1)) curGateLower(LGNN(IUJ(1))) curSlope]; %length(LGNN)-LGNN(IUJ(1))];
            extrapLowerGate(LGNN(IUJ(1)+1):LGNN(end)) = floor(max(minLowerGateVal,min(curGateLower(LGNN(IUJ(1)+1):LGNN(end)),floor(curGateLower(LGNN(IUJ(1))) + curSlope*([LGNN(IUJ(1)+1):LGNN(end)]-LGNN(IUJ(1)))))));
        %if there are only downward jumps
        elseif isempty(IUJ) && ~isempty(IDJ) 
            %start high and one jump down
           if IDJ(1) < LGNN(end)-1
                curSlope = curGateLower(LGNN(IDJ(1)+2)) - curGateLower(LGNN(IDJ(1)+1));
            else
                curSlope = 0;
            end;
            highStateRuns = [LGNN(1) LGNN(IDJ(1)) LGNN(IDJ(1)+1) curGateLower(LGNN(IDJ(1)+1)) curSlope];
            extrapLowerGate([LGNN(1):LGNN(IDJ(1))]) = floor(max(minLowerGateVal,min(curGateLower([LGNN(1):LGNN(IDJ(1))]),curGateLower(LGNN(IDJ(1)+1)) + curSlope*([LGNN(1):LGNN(IDJ(1))]-LGNN(IDJ(1)+1)))));

        %if there are both upward and downward jumps
        elseif ~isempty(IUJ) && ~isempty(IDJ)
            %[IUJ,IDJ] = sandwich(IUJ,IDJ);
            if IDJ(1) < IUJ(1) % first jump is down 
                NhighRuns = 1+length(IUJ);
                FJD = 1; % frst jump down flag
            else
                NhighRuns = length(IUJ);
                FJD = 0;
            end
            highStateRuns = zeros(NhighRuns,5);
            for ll = 1:NhighRuns
                if FJD == 1 
                    if ll == 1
                        if IDJ(1)+2 <= IUJ(1)
                            curSlope = curGateLower(LGNN(IDJ(1)+2)) - curGateLower(LGNN(IDJ(1)+1));
                        else
                            curSlope = 0; 
                        end;
                        highStateRuns(1,1:5) = [LGNN(1) LGNN(IDJ(1)) LGNN(IDJ(1)+1) curGateLower(LGNN(IDJ(1)+1)) curSlope];
                        extrapLowerGate(LGNN(1):LGNN(IDJ(1))) = floor(max(minLowerGateVal,min(curGateLower(LGNN(1):LGNN(IDJ(1))),floor(curGateLower(LGNN(IDJ(1))+1) + curSlope*([LGNN(1):LGNN(IDJ(1))]-LGNN(IDJ(1)+1))))));
                        % highStateRuns(1,2) = IDJ(1)-1;
                    else
                        highStateRuns(ll,1) = LGNN(IUJ(ll-1)+1);% IDJ(1)-1];
                        % determine run length 
                        njdi = find(IDJ >  IUJ(ll-1),1,'first');
                        njd = IDJ(njdi);
                        if ~isempty(njd)
                            highStateRuns(ll,2) = LGNN(IUJ(ll-1)) + njd-IUJ(ll-1);
                            curSlope = (curGateLower(LGNN(njd+1))-curGateLower(LGNN(IUJ(ll-1))))/(njd-IUJ(ll-1)+2);
                            highStateRuns(ll,[3:5]) = [LGNN((IUJ(ll-1))), curGateLower((IUJ(ll-1))), curSlope];
                            extrapLowerGate(LGNN(IUJ(ll-1))+1:LGNN(njd)) = floor(max(minLowerGateVal,floor(curGateLower(LGNN(IUJ(ll-1))) + curSlope*([LGNN(IUJ(ll-1))+1:LGNN(njd)]-LGNN(IUJ(ll-1)))))) ;
                        else
                            if IUJ(ll-1)-(IDJ(ll-1)+1) > 0
                                curSlope = curGateLower(LGNN(IUJ(ll-1))) - curGateLower(LGNN(IUJ(ll-1)-1));
                            else
                               curSlope = 0;
                            end
                            highStateRuns(ll,2:5) = [LGNN(end) LGNN(IUJ(ll-1)) curGateLower(LGNN(IUJ(ll-1))) curSlope];%
                            extrapLowerGate([LGNN(IUJ(ll-1))+1:LGNN(end)]) = floor(max(minLowerGateVal,min(curGateLower(LGNN(IUJ(ll-1))+1:LGNN(end)),floor(curGateLower(LGNN(IUJ(ll-1))) + curSlope*([LGNN(IUJ(ll-1))+1:LGNN(end)]-LGNN(IUJ(ll-1)))))));
                        end;
                    end;
                else
                    highStateRuns(ll,1) = LGNN(IUJ(ll))+1;
                     % determine run length 
                    njdi = find(IDJ >  IUJ(ll),1,'first');
                    njd = IDJ(njdi);
                    if ~isempty(njd)
                       % highStateRuns(ll,2) = LGNN(IUJ(ll)) + njd-IUJ(ll);
                       curSlope = (curGateLower(LGNN(njd+1)) - curGateLower(LGNN(IUJ(ll))))/(LGNN(njd+1) - LGNN(IUJ(ll)));
                       highStateRuns(ll,2:5) = [LGNN(njd), LGNN(njd+1), curGateLower(LGNN(IUJ(ll))), curSlope];
                       extrapLowerGate([LGNN(IUJ(ll))+1:LGNN(njd)]) = floor(min(curGateLower([LGNN(IUJ(ll))+1:LGNN(njd)]),floor(curGateLower(LGNN(IUJ(ll))) + curSlope*([LGNN(IUJ(ll))+1:LGNN(njd)]-LGNN(IUJ(ll))))));
                    else   
                        if IUJ(ll)- (IDJ(ll-1)+1) > 0
                            curSlope = curGateLower(LGNN(IUJ(ll))) - curGateLower(LGNN(IUJ(ll)-1));
                        else
                           curSlope = 0;
                        end
                        highStateRuns(ll,2:5) = [LGNN(end), LGNN(IUJ(ll)), curGateLower(LGNN(IUJ(ll))), curSlope]; %
                        extrapLowerGate([LGNN(IUJ(ll))+1:LGNN(end)]) = floor(max(minLowerGateVal,min(curGateLower([LGNN(IUJ(ll))+1:LGNN(end)]),floor(curGateLower(LGNN(IUJ(ll))) + curSlope*([LGNN(IUJ(ll))+1:LGNN(end)]-LGNN(IUJ(ll)))))));
                    end;
                end;
            end;
        end;
        imposeBound = 1;
%         highStatePix = sum((highStateRuns(:,2)-highStateRuns(:,1)+1));
%         if highStatePix/length(LGNN) >= highStatePixFracThresh
%             imposeBound = 1;
%             % interpolate/extrapolate across highState jumps
%             
%         else
%             imposeBound = 0;
%         end
    end;
%         if maxlowerjump > maxjumpThresh
%             imposeBound = 1;
%         else
%             imposeBound = 0;
%         end;
    % [curTB,curPolyGate] = initialTopBottom_v2(curim,curGate,figTB1,figTBOR,fighTBim);
    if imposeBound == 1
        curGate(1,:) = extrapLowerGate;
    end;
    % imposeBound2 = 1;
    %run spline fit against extrapolated gates
    [curTB,curPolyGate] = initialTopBottom_v3_nf(curim,curGate,imposeBound);
    SAATB(:,:,ii) = curTB;
    polyGate{ii}.lower = curPolyGate.lower;
    polyGate{ii}.upper = curPolyGate.upper;
    polyGate{ii}.extrapLower = curPolyGate.extrapLower;
    polyGate{ii}.extrapUpper = curPolyGate.extrapUpper;

    
end;
 save(ofile,'imageGate','initBdryIm','initLayers','SAATB','polyGate');
  

 */

	private static enum PixelDensityOp {
		UC,
		CC
	}

	private double[] pixelDensityMap(byte[] clmat, int width, int height, int hdim, int vdim, PixelDensityOp opat, double thresh) {
		double[] pdm = new double[width * height];
		int[] N1sMat = new int[width * height];
		if ((hdim & 1) == 0) {
			hdim++;
		}
		if ((vdim & 1) == 0) {
			vdim++;
		}
		for (int i = 0; i < height; i++) {
			int minI = 0, maxI = 0;
			switch (opat) {
			case UC:
				minI = i;
				maxI = Math.min(i + hdim - 1 + 1, height);
				break;
			case CC:
				minI = Math.max(i - (hdim-1)/2, 0);
				maxI = Math.min(i + (hdim / 2) + 1, height);
				break;
			}
			for (int j = 0; j < width; j++) {
				int minJ = Math.max(j - ((vdim - 1) / 2), 0);
				int maxJ = Math.min(j + vdim / 2 + 1, width);
				int N1s = 0;
				for (int y = minI; y < maxI; y++) {
					for (int x = minJ; x < maxJ; x++) {
						N1s += clmat[y * width + x];
					}
				}
				double frac1 = ((double)(N1s)) / ((double)((maxI - minI) * (maxJ - minJ)));
				N1sMat[i * width + j] = N1s;
				if (frac1 >= thresh) {
					pdm[i * width + j] = frac1;
				}
			}
		}
		return pdm;
	}

	private double[] filterRows(double[] image, int width, int height) {
		double[] sumImIn = new double[height];
		for (int i = 0, offset = 0; i < height; i++) {
			double sum = 0;
			for (int j = 0; j < width; j++) {
				sum += image[offset];
				offset++;
			}
			sumImIn[i] = sum;
		}
		List<SupportInterval> supInt = supportIntervals(sumImIn);
		int nInts = supInt.size();
		if (nInts == 1) {
			return image;
		}
		double maxwt = supInt.get(0).weight;
		int maxind = 0;
		for (int i = 1; i < nInts; i++) {
			double w = supInt.get(i).weight;
			if (w > maxwt) {
				maxwt = w;
				maxind = i;
			}
		}
		if (maxind == 0) {
			return image;
		} else {
			throw new UnsupportedOperationException("Still need to port this!");
		}
		//return null;
		/*
		 * function imout = filterRows(imin);
[aa,bb] = size(imin);
sumImIn = sum(imin,2);
supInt = supportIntervals([1:aa],sumImIn);
Nints = length(supInt);
weightFac = .05;
rowFilt = ones(aa,bb);
if Nints == 1
   imout = imin;
else
    supvals = zeros(Nints,1);
    for ii = 1:Nints
        supvals(ii) = supInt{ii}.weight;
    end;
    [maxwt,maxind] = max(supvals);
    if maxind == 1
        imout = imin;
    else
        I1 = find(supvals <= maxwt*weightFac);
        I2 = find(I1 < maxind,1,'last');
        if ~isempty(I2)
           lastZeroRow = supInt{I2}.interval(end);
           rowFilt(1:lastZeroRow,:) = zeros(lastZeroRow,bb);
           imout = imin.*rowFilt;
        else
            imout = imin;
        end;
    end;
end;
		 */
	}

	private static class SupportInterval {
		/**
		 * The start of the interval, inclusive.
		 */
		public int intervalStart;
		/**
		 * The end of the interval, exclusive.
		 */
		public int intervalEnd;
		public double weight;
		@Override
		public String toString() {
			return "SupportInterval [intervalStart=" + intervalStart
					+ ", intervalEnd=" + intervalEnd + ", weight=" + weight
					+ "]";
		}
	}

	/**
	 * Assumes that the function is non-negative and finds the support
	 * intervals, ie intervals on which the funcvalues are greater than zero and
	 * the integral of the function on the intervals
	 * @param values
	 */
	private List<SupportInterval> supportIntervals(double[] values) {
		// MATLAB calls it "eps", Java calls it "ulp" for whatever reason
		double minValue = Math.ulp(1.0);
		int i = 0;
		for (; i < values.length; i++) {
			// First, skip over any values that are less than or equal to
			// minValue
			if (values[i] > minValue) {
				break;
			}
		}
		List<SupportInterval> intervals = new ArrayList<SupportInterval>();
		// From here on out, "break" points are any points where the number of
		// values is greater than the min value. I think.
		SupportInterval currentInterval = new SupportInterval();
		currentInterval.intervalStart = i;
		double weight = 0.0;
		for (; i < values.length; i++) {
			if (values[i] <= minValue) {
				// Interval ends
				currentInterval.intervalEnd = i;
				currentInterval.weight = weight;
				intervals.add(currentInterval);
				// And move ahead to the next non-zero value
				i++;
				for (; i < values.length; i++) {
					if (values[i] > minValue) {
						// And break out of this loop and continue as normal
						break;
					}
				}
				if (i < values.length) {
					currentInterval = new SupportInterval();
					currentInterval.intervalStart = i;
					weight = values[i];
					continue;
				} else {
					// If we've reached the end without finding a new interval,
					// null out the current interval and break so that we don't
					// try and increment the weight with a non-existant value
					currentInterval = null;
					break;
				}
			}
			weight += values[i];
		}
		// It's possible the last interval extended to the very end and was
		// therefore not added.
		if (currentInterval != null) {
			currentInterval.intervalEnd = values.length;
			currentInterval.weight = weight;
			intervals.add(currentInterval);
		}
		return intervals;
	}

	private void gradientImage2d(double[] image, int width, int height, int sd, int mf) {
		double sd1 = sd;
		double sd2 = sd;
		int extent = mf*sd;

		double[] imx = gausKernDiff2d(image, width, height, 1, -1, sd1,sd2,extent,extent);
		double[] imy = gausKernDiff2d(image, width, height, -1, 1, sd1,sd2,extent,extent);

		//gim = (imx.^2 + imy.^2 ).^(0.5);
		double[] gim = new double[imx.length];
		for (int i = 0; i < gim.length; i++) {
			double x = imx[i];
			double y = imy[i];
			gim[i] = FastMath.sqrt(x * x + y * y);
		}
		DebugUtils.showDebugMatrix("gim", gim, width, height);

		double[] imxx = gausKernDiff2d(image, width, height, 2, -1,sd1,sd2,extent,extent);

		double[] imyy = gausKernDiff2d(image, width, height, -1, 2, sd1,sd2,extent,extent);

		double[] imxy = gausKernDiff2d(image, width, height, 1, 1, sd1,sd2,extent,extent);

		// g2im = ((imx.^2).*imxx + 2*(imx.*imy.*imxy)  + (imy.^2).*imyy)./(gim.^2);
		double[] g2im = new double[gim.length];
		for (int i = 0; i < g2im.length; i++) {
			double x = imx[i];
			double y = imy[i];
			double gSquared = gim[i];
			gSquared *= gSquared;
			g2im[i] = ((x*x) * imxx[i] + 2*(x*y*imxy[i]) + y*y*imyy[i]) / gSquared;
		}
		DebugUtils.showDebugMatrix("g2im", g2im, width, height);
	}
	/*
	 * unction [gim,g2im] = gradientImage2d(im,sd,mf,ofilebase)
% [gim,g2im] = gradientImage(im)
% if g1flag == 1, computes the gradient image, returned in gim, and if g2flag == 1, the derivative of the
% gradient image in the direction of the gradient, returned in
% g2im
    %
    g1flag = 1;
    g2flag = 1;
n1 = 1;
n2 = [];
%n3 = [];
sd1 = sd;
sd2 = sd;
%sd3 = sd;
% extent1 = 6;
% extent2 = 6;
% extent3 = 6;
extent1 = mf*sd;
extent2 = mf*sd;
extent3 = mf*sd;
imx = gauskerndiff2d(im,n1,n2,sd1,sd2,extent1,extent2);
n1 = [];
n2 = 1;
n3 = [];
imy = gauskerndiff2d(im,n1,n2,sd1,sd2,extent1,extent2);
% n1 = [];
% n2 = [];
% n3 = 1;
% imz = gauskerndiff(im,n1,n2,n3,sd1,sd2,sd3,extent1,extent2,extent3);
if g1flag == 1
   gim = (imx.^2 + imy.^2 ).^(0.5);
else
    gim = [];
end;
if g2flag == 1
    n1 = 2;
    n2 = [];

    imxx = gauskerndiff2d(im,n1,n2,sd1,sd2,extent1,extent2);
    n1 = [];
    n2 = 2;

    imyy = gauskerndiff2d(im,n1,n2,sd1,sd2,extent1,extent2);
%     n1 = [];
%     n2 = [];
%     n3 = 2;
%     imzz = gauskerndiff(im,n1,n2,n3,sd1,sd2,sd3,extent1,extent2,extent3);
    n1 = 1;
    n2 = 1;

    imxy = gauskerndiff2d(im,n1,n2,sd1,sd2,extent1,extent2);
%     n1 = 1;
%     n2 = [];
%     n3 = 1;
%     imxz = gauskerndiff(im,n1,n2,n3,sd1,sd2,sd3,extent1,extent2,extent3);
%     n1 = [];
%     n2 = 1;
%     n3 = 1;
%     imyz = gauskerndiff(im,n1,n2,n3,sd1,sd2,sd3,extent1,extent2,extent3);
   % g2im = ((imx.^2).*imxx + 2*(imx.*imy.*imxy) + 2*(imx.*imz.*imxz) + 2*(imy.*imz.*imyz) + (imy.^2).*imyy + (imz.^2).*imzz)./(gim.^2);
    g2im = ((imx.^2).*imxx + 2*(imx.*imy.*imxy)  + (imy.^2).*imyy)./(gim.^2);
end;
% DPOTTER: Commented out unneeded MAT file generation
%ofilebase2 = [ofilebase,'_sd',int2str(sd)];
%cofile = [ofilebase2,'_dx'];
%save(cofile,'imx')
%cofile = [ofilebase2,'_dy'];
%save(cofile,'imy')
% cofile = [ofilebase2,'_dz'];
% save(cofile,'imz')
%cofile = [ofilebase2,'_dxx'];
%save(cofile,'imxx')
%cofile = [ofilebase2,'_dyy'];
%save(cofile,'imyy')
%cofile = [ofilebase2,'_dxy'];
%save(cofile,'imxy')
% cofile = [ofilebase2,'_dzz'];
% save(cofile,'imzz')
%cofile = [ofilebase2,'_g1'];
%save(cofile,'gim')
%cofile = [ofilebase2,'_g2'];
%save(cofile,'g2im')
   
	 */
	/**
	 * For the Java port, negative numbers are used to indicate that an order
	 * should be skipped. Not sure it makes sense, but I'm assuming an order of
	 * less than 0 can't really happen.
	 * @param image
	 * @param width
	 * @param height
	 * @param n1
	 * @param n2
	 * @param sd1
	 * @param sd2
	 * @param extent1
	 * @param extent2
	 * @return
	 */
	private double[] gausKernDiff2d(double[] image, int width, int height, int n1, int n2, double sd1, double sd2, int extent1, int extent2) {
		double[] fim;
		if (n1 < 0) {
			fim = image;
		} else {
			fim = null;
			double[] fc1 = gaussDeriv(n1, sd1, extent1);
			fim = sepConv(image, width, height, fc1);
		}
		if (n2 >= 0) {
			// Shift dimensions, which in this case means effectively swapping
			// row and column
			//DebugUtils.showDebugImage("original", image, width, height);
			double[] shifted = MathUtil.shiftDimensions(image, width, height);
			//DebugUtils.showDebugImage("shifted", shifted, height, width);
			double[] fc2 = gaussDeriv(n2, sd2, extent2);
			fim = sepConv(shifted, height, width, fc2);
			//DebugUtils.showDebugImage("fim", fim, height, width);
			fim = MathUtil.shiftDimensions(fim, height, width);
			//DebugUtils.showDebugImage("shifted fim", fim, width, height);
		}
		return fim;
	}
	/**
	 * gd is the Gaussian derivative calculated from
	 * [-stdev*extent,stdev*extent]; of order order.
	 * @param order
	 * @param stdev
	 * @param extent
	 */
	private double[] gaussDeriv(int order, double stdev, int extent) {
		double[] xv = new double[extent * 2 + 1];
		double[] nxv = new double[xv.length];
		double[] gf = new double[xv.length];
		double stdevSqrt2 = stdev * FastMath.sqrt(2.0);
		double stdevSqrt2Pi = stdev * FastMath.sqrt(2.0 * FastMath.PI);
		for (int i = -extent, idx=0; i <= extent; i++, idx++) {
			xv[idx] = (double) i;
			nxv[idx] = xv[idx] / stdevSqrt2;
			gf[idx] = FastMath.exp(-(nxv[idx] * nxv[idx])) / stdevSqrt2Pi;
		}
		double[] hf = hermite(nxv, order);
		double[] gd = new double[xv.length];
		for (int i = 0; i < gd.length; i++) {
			gd[i] = FastMath.pow(-1.0 / stdevSqrt2, order) * gf[i] * hf[i];
		}
		return gd;
	}

	/**
	 * on return, fim is the convolution of columns of im with the filter f
	 * such that incompletly filtered values are zero
	 * assumes that length(f) = 2*r+1; 
	 * @param image a 2-d matrix
	 * @param f a filter
	 */
	private double[] sepConv(double[] image, int width, int height, double[] f) {
		System.out.println("f=" + Arrays.toString(f));
		double[] zpim = MathUtil.vectorize(image, width, height, width, height + f.length); //Arrays.copyOf(image, image.length + width * f.length);
		System.out.println("zpim.length=" + zpim.length);
		System.out.println("zpim=" + Arrays.toString(Arrays.copyOfRange(zpim, 220, 240)));
		double[] Y = FilterUtils.convolve(zpim, f);
		System.out.println("Y=" + Arrays.toString(Arrays.copyOfRange(Y, 220, 240)));
		double[] fim = new double[image.length];
		int r = (f.length - 1) / 2;
		// Pad the top and bottom with NaN
		Arrays.fill(fim, 0, r * width, Double.NaN);
		Arrays.fill(fim, fim.length - r * width, fim.length, Double.NaN);
		// And copy the rest over from Y
		int index = r * width;
		int lastIndex = fim.length - r * width;
		for (int row = f.length-1; index < lastIndex; row++) {
			for (int c = 0; c < width; c++) {
				fim[index] = Y[c * height + row];
				index++;
			}
		}
		//DebugUtils.showDebugImage("fim", fim, width, height);
		//DebugUtils.showDebugMatrix("fim", fim, width, height);
		return fim;
	}

	/**
	 * The coefficients used by hermite.
	 * Note: PolynomialFunction in Commons Math expects the coefficients in the
	 * opposite order than MATLAB does.
	 */
	private final double[][] HERMITE_COEFFICIENTS = {
		new double[] { 1.0 },
		new double[] { 0.0, 2.0 },
		new double[] { -2.0, 0.0, 4.0 },
		new double[] { 0, -12, 0, 8 },
		new double[] { 12, 0, -48, 0, 16 },
		new double[] { 0, 120, 0, -160, 0, 32 },
		new double[] { -120, 0, 720, 0, -480, 0, 64 },
		new double[] { 0, -1680, 0, 3360, 0, -1344, 0, 128 }
	};
	/**
	 * FIXME: There also exists PolynomialUtils.createHermitePolynomial, which
	 * can probably be used in place of this.
	 * Returns the value of the hermite polynomial of {@code degree} degree on
	 * {@code domain} domain.
	 * @param domain
	 * @param degree
	 * @return
	 */
	private double[] hermite(double[] domain, int degree) {
		if (degree < 0 || degree >= HERMITE_COEFFICIENTS.length)
			throw new IllegalArgumentException("degree must be a non negative integer less than or equal to 7");
		PolynomialFunction pf = new PolynomialFunction(HERMITE_COEFFICIENTS[degree]);
		double[] result = new double[domain.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = pf.value(domain[i]);
		}
		return result;
	}

	public static void main(String[] args) {
		for (int i = 0; i < args.length; i++) {
			try {
				Gating gating = new Gating();
				gating.initialLayerId(ImageCube.fromRawBinary(new FileInputStream(args[i])));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
