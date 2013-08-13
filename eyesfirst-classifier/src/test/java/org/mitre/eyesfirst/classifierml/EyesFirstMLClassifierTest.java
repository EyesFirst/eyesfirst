package org.mitre.eyesfirst.classifierml;

import static org.junit.Assert.*;
import static org.junit.Assume.*;

import com.mathworks.toolbox.javabuilder.MWException;

import org.junit.Test;

public class EyesFirstMLClassifierTest {
	@Test
	public void testEyesFirstClassifier() {
		try {
			EyesFirstMLClassifier classifier = new EyesFirstMLClassifier();
			classifier.run_unit_tests();
			classifier.dispose();
		} catch(LinkageError e) {
			System.err.println("Error starting MATLAB: " + e);
			System.err.println("Unable to run the test - unable to initialize MATLAB!");
			System.err.println("The MATLAB environment was likely not set up correctly.");
			System.err.println("java.library.path=" + System.getProperty("java.library.path"));
			assumeNoException(e);
		} catch(MWException e) {
			fail("MWException running unit test: " + e);
		}
	}
}
