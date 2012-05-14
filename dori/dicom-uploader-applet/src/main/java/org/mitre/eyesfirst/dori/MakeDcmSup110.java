/*
 * Copyright 2012 The MITRE Corporation
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
package org.mitre.eyesfirst.dori;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;
import java.util.regex.Pattern;

import org.dcm4che2.data.BasicDicomObject;
import org.dcm4che2.data.DicomElement;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Implementation;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.UID;
import org.dcm4che2.data.VR;
import org.dcm4che2.util.UIDUtils;
import org.mitre.eyesfirst.Uploader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Responsible for the Dicom metadata associated with making a valid Dicom Supplement 110 (OPT).
 *
 * @author David Smiley dsmiley@mitre.org
 */
public class MakeDcmSup110 {
	private static final Logger log = LoggerFactory.getLogger(MakeDcmSup110.class);

	private MakeDcmSup110() {
	}

	private static final String EF_UID_ROOT = "1.2.826.0.1.3680043.8.1302";//the 1 was concatenated onto the issuer.

	private static final Random random = new SecureRandom();

	public static void updateDicomObject(Zeiss2Dcm zeiss2Dcm, DicomObject nds) {
		//--Do a few things up front because some later portions in order depend on them
		String laterality = nds.getString(Tag.Laterality);
		if ("OS".equals(laterality))
			laterality = "L";
		else if ("OD".equals(laterality))
			laterality = "R";

		final float sliceThickness = nds.getFloat(Tag.SpacingBetweenSlices);

		final DicomObject arSequence = getNestedDicomObjectNN(nds, Tag.AnatomicRegionSequence);//see CID 4209
		arSequence.putString(Tag.CodingSchemeDesignator, null, "SRT");
		arSequence.putString(Tag.CodeValue, null, "T-AA610");
		arSequence.putString(Tag.CodeMeaning, null, "Retina");

		//--?
		nds.putBytes(Tag.FileMetaInformationVersion, VR.OB, new byte[]{0, 1}, false);
		nds.putString(Tag.MediaStorageSOPClassUID, null, UID.OphthalmicTomographyImageStorage);//module?

		//--SOP COMMON MODULE ATTRIBUTES C.12.1		M

		final String instanceUid = makeUid(nds);
		System.out.println(instanceUid);

		String oldSeriesUid = Uploader.seriesUidMap.get(nds
				.getString(Tag.SeriesInstanceUID));
		String seriesUid;
		if(Uploader.seriesUidMap.containsKey(oldSeriesUid)) {
			System.out.println("Series UID found");
			seriesUid = Uploader.seriesUidMap.get(oldSeriesUid);
		} else {
			seriesUid = makeUid(nds);
			Uploader.seriesUidMap.put(oldSeriesUid, seriesUid);
		}

		String oldStudyUid = Uploader.studyUidMap.get(nds
				.getString(Tag.StudyInstanceUID));
		String studyUid;
		if (Uploader.studyUidMap.containsKey(oldStudyUid)) {
			System.out.println("Study UID found");
			studyUid = Uploader.studyUidMap.get(oldStudyUid);
		} else {
			studyUid = makeUid(nds);
			Uploader.studyUidMap.put(oldStudyUid, studyUid);
		}

		{
			//record source image sequence so we can trace-back if needed
			DicomElement srcImgArray = nds.get(Tag.SourceImageSequence,VR.SQ);
			if (srcImgArray == null)
				srcImgArray = nds.putSequence(Tag.SourceImageSequence);
			DicomObject srcImgSeq = new BasicDicomObject();
			srcImgSeq.putString(Tag.SOPInstanceUID,null,nds.getString(Tag.SOPInstanceUID));
			srcImgSeq.putString(Tag.SOPInstanceUID,null,nds.getString(Tag.SOPInstanceUID));
			srcImgArray.addDicomObject(srcImgSeq);

			nds.putString(Tag.SOPInstanceUID,null,instanceUid);
			nds.putString(Tag.MediaStorageSOPInstanceUID,null,instanceUid);
			nds.putString(Tag.SeriesInstanceUID, null, seriesUid);
			nds.putString(Tag.StudyInstanceUID, null, studyUid);

		}
		nds.putString(Tag.SOPClassUID, null, UID.OphthalmicTomographyImageStorage);
		//?
		nds.remove(Tag.OperatorIdentificationSequence);

		nds.putString(Tag.ImplementationClassUID,null,Implementation.classUID());
		nds.putString(Tag.ImplementationVersionName,null,Implementation.versionName());

		//--Patient	 	Patient 		C.7.1.1		M
		//Don't mess with patient-id (or issuer), patient-name
		//		final String efId = validateEfId(nds.getString(Tag.PatientID));
		//		if (efId != null) {
		//			nds.putString(Tag.IssuerOfPatientID, null, "eyesfirst.org");
		//			nds.putNull(Tag.PatientName, null);
		//		}

		//--Study	 	General Study	C.7.2.1		M
		nds.putNull(Tag.ReferringPhysicianName, null);
		nds.putNull(Tag.StudyID,null);

		//--Series		General Series	C.7.3.1		M
		nds.putString(Tag.Modality, null, "OPT");
		nds.putInt(Tag.SeriesNumber, null, -1 * ((int) (Math.pow(2, 31) - 1)));//smallest (not Integer.MINVALUE)

		//--Image		Multi-frame Functional Groups		C.7.6.16	M
		nds.putDate(Tag.ContentDate, null, nds.getDate(Tag.AcquisitionDate));
		nds.putDate(Tag.ContentTime, null, nds.getDate(Tag.AcquisitionTime));
		nds.putString(Tag.ConcatenationUID, null, nds.getString(Tag.SOPInstanceUID));
		nds.putString(Tag.SOPInstanceUIDOfConcatenationSource, null, nds.getString(Tag.SOPInstanceUID));//?
		// nds.putInt(Tag.InConcatenationNumber, null, 1);  specified later for C.8.17.X2
		nds.putInt(Tag.InConcatenationTotalNumber, null, 1);

		final DicomObject sfgSeq = getNestedDicomObjectNN(nds, Tag.SharedFunctionalGroupsSequence);
		//--(Table A.XX.4.3-1  OPHTHALMIC TOMOGRAPHY FUNCTIONAL GROUP MACROS)
		//--	Pixel Measures C.7.6.16.2.1 M
		{
			String pair = nds.getString(Tag.PixelSpacing);
			nds.remove(Tag.PixelSpacing);
			int commaIdx = pair.indexOf(',');
			//note; we've flipped them from where we acquired them since the input order is wrong
			float width = Float.parseFloat(pair.substring(0, commaIdx));
			float height = Float.parseFloat(pair.substring(commaIdx + 1));
			DicomObject pmSeq = getNestedDicomObjectNN(sfgSeq, Tag.PixelMeasuresSequence);//putting in shared functional group seq
			pmSeq.putFloats(Tag.PixelSpacing, null,
					new float[]{height, width}
					);
			pmSeq.putFloat(Tag.SliceThickness, null, sliceThickness);
		}
		//--	Pixel Orientation C.7.6.16.2.4 C (required if no ref image)
		{
			DicomObject poSeq = getNestedDicomObjectNN(sfgSeq, Tag.PlaneOrientationSequence);
			//TODO validate this value is correct.
			poSeq.putFloats(Tag.ImageOrientationPatient,null,new float[]{1f,0f,0f,0f,1f,0f});
		}

		//--	Frame Anatomy C.7.6.16.2.8 M
		{
			DicomObject faSeq = getNestedDicomObjectNN(sfgSeq, Tag.FrameAnatomySequence);//C.7.6.16.2.8
			faSeq.putString(Tag.FrameLaterality,null,laterality);
			DicomObject newArSeq = getNestedDicomObjectNN(faSeq, Tag.AnatomicRegionSequence);
			arSequence.copyTo(newArSeq);
			//addSeq(faSeq, Tag.AnatomicRegionSequence, arSequence);//originally CID 4030 but for opthalmic we use 4209
		}

		final DicomElement pffgSeq = nds.putSequence(Tag.PerFrameFunctionalGroupsSequence);
		for (int i = 0; i < zeiss2Dcm.frames; i++) {
			final DicomObject frameDco = new BasicDicomObject();
			pffgSeq.addDicomObject(frameDco);
			//--	Frame Content C.7.6.16.2.2 M – May not be used as a Shared Functional Group.
			getNestedDicomObjectNN(frameDco, Tag.FrameContentSequence);
			//--	Plane Position C.7.6.16.2.3 C (required if no ref image)
			DicomObject ppSeq = getNestedDicomObjectNN(frameDco, Tag.PlanePositionSequence);
			ppSeq.putFloats(Tag.ImagePositionPatient,null,new float[]{0,0,i*sliceThickness});
			//-- no Opthalmic Frame Location Sequence since we have no ref image

		}

		//--Image		Multi-frame Dimension				C.7.6.17 	M
		nds.putSequence(Tag.DimensionOrganizationSequence);//type 2? ftp://medical.nema.org/medical/dicom/final/cp779_ft.pdf
		nds.putSequence(Tag.DimensionIndexSequence);//type 2?
		//--Image		Ophthalmic Tomography Image 		C.8.17.X2	M
		nds.putStrings(Tag.ImageType, null, new String[]{"ORIGINAL", "PRIMARY"});
		nds.putInt(Tag.PixelRepresentation, null, 0);
		nds.putString(Tag.PresentationLUTShape, null, "IDENTITY");
		nds.putString(Tag.BurnedInAnnotation, null, "NO");
		nds.putInt(Tag.ConcatenationFrameOffsetNumber, null, 0);
		nds.putInt(Tag.InConcatenationNumber, null, 1);

		//--Image		Ophthalmic Tomography Acquisition Parameters	C.8.17.X3	M
		nds.putNull(Tag.AxialLengthOfTheEye, null);
		nds.putNull(Tag.HorizontalFieldOfView, null);
		nds.putNull(Tag.RefractiveStateSequence, null);
		nds.putNull(Tag.EmmetropicMagnification, null);
		nds.putNull(Tag.IntraOcularPressure, null);
		nds.putNull(Tag.PupilDilated, null);

		//--Image		Ophthalmic Tomography Parameters	C.8.17.X4	M
		{
			DicomObject adtcSeq = getNestedDicomObjectNN(nds, Tag.AcquisitionDeviceTypeCodeSequence);
			//Baseline Context ID is 4210
			adtcSeq.putString(Tag.CodingSchemeDesignator, null, "SRT");
			adtcSeq.putString(Tag.CodeValue, null, "A-00FBE");
			adtcSeq.putString(Tag.CodeMeaning, null, "Optical Coherence Tomography Scanner");
		}
		nds.putNull(Tag.LightPathFilterTypeStackCodeSequence, null);
		nds.putNull(Tag.DetectorType, null);//TODO which type?
		//--Image		Acquisition Context					C.7.6.14	M
		nds.putSequence(Tag.AcquisitionContextSequence);//type 2?
		//--Image		Ocular Region Imaged				C.8.17.5	M
		//for "atomic region sequence" see top of file
		{
			nds.remove(Tag.Laterality);//use ImageLaterality instead
			nds.putString(Tag.ImageLaterality, null, laterality);
		}

		//--(general)
		final Collection<DicomObject> cgDcos = new ArrayList<DicomObject>();
		findDcmObjsContaining(nds, Tag.ContextGroupExtensionCreatorUID, cgDcos);
		for (DicomObject doi : cgDcos) {
			//			doi.putString(Tag.ContextGroupExtensionFlag,null,"Y");
			//			doi.putString(Tag.ContextGroupLocalVersion,null,"0");
			doi.remove(Tag.ContextGroupExtensionCreatorUID);
			//NOTE: A Zeiss UID would start with 1.2.276.0.75.2
		}
		nds.putNull(Tag.PositionReferenceIndicator, null);
	}

	/** Incorporate CIRRUS's UID into our own which can be useful in troubleshooting. */
	private static String makeUid(DicomObject nds) {
		String randPart = ""+Math.abs(random.nextLong());

		StringBuilder uidBld = new StringBuilder(64);
		uidBld.append(EF_UID_ROOT).append('.').append(randPart);
		String uid = uidBld.toString();
		UIDUtils.verifyUID(uid);
		return uid;
	}

	static Pattern EFIDPTN = Pattern.compile("EF[0-9A-F]{8}[0-9]"); //ex: EF7BBC7D243

	private static String validateEfId(String efid) {
		if (efid == null)
			return null;
		if (!EFIDPTN.matcher(efid).matches()) {
			log.warn("Patient id did not match EFID pattern: "+efid);
		}
		return efid;
	}

	private static DicomObject getNestedDicomObjectNN(DicomObject frameDco, int tag) {
		DicomObject dco = frameDco.getNestedDicomObject(tag);
		if (dco != null)
			return dco;
		DicomElement pmSeq = frameDco.putSequence(tag,1);
		return pmSeq.addDicomObject(new BasicDicomObject());
	}

	private static void findDcmObjsContaining(DicomObject dco, final int tag, final Collection<DicomObject> collect) {
		if (dco == null)
			return;
		if (dco.get(tag) != null)
			collect.add(dco);
		dco.accept(new DicomObject.Visitor() {
			@Override
			public boolean visit(DicomElement e) {
				if (e.hasDicomObjects())
					findDcmObjsContaining(e.getDicomObject(), tag, collect);
				return true;
			}
		});
	}

}
