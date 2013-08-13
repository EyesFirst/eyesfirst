package org.mitre.eyesfirst.util;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Very simple class that blocks the close method from doing anything.
 * Useful for brain-dead classes that close streams that they do not actually
 * own and therefore should not be closing.
 * @author dpotter
 */
public class NoCloseOutputStream extends FilterOutputStream {
	public NoCloseOutputStream(OutputStream out) {
		super(out);
	}

	@Override
	public void close() throws IOException {
	}
}
