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
package org.mitre.eyesfirst.processor;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.LinkedList;
import java.util.Queue;

/**
 * A helper class for dealing with reading strings from the command line.
 * <p>
 * Essentially, dump incoming bytes into this class using {@link #write(byte[], int, int)},
 * and then use {@link #readLine()} to read any parsed lines.
 * @author dpotter
 */
public class ConsoleInputHelper {
	private final CharsetDecoder decoder;
	private final ByteBuffer buffer;
	private final CharBuffer characters;
	private final Queue<String> readLines;
	private StringBuilder currentLine;

	/**
	 * Creates a new helper using the default charset with a buffer size of
	 * 8KB.
	 */
	public ConsoleInputHelper() {
		this(Charset.defaultCharset(), 1024*8);
	}

	/**
	 * Creates a new helper using the given character set and buffer size.
	 * 
	 * @param charset
	 *            the character set to use when decoding bytes
	 * @param bufferSize
	 *            the size to allocate for storing incoming byte and character
	 *            data
	 * @throws NullPointerException
	 *             if {@code charset} is {@code null}
	 * @throws IllegalArgumentException
	 *             if the buffer size is less than 16
	 */
	public ConsoleInputHelper(Charset charset, int bufferSize) {
		if (charset == null)
			throw new NullPointerException("charset");
		if (bufferSize < 16)
			throw new IllegalArgumentException("Bad buffer size " + bufferSize);
		buffer = ByteBuffer.allocate(bufferSize);
		characters = CharBuffer.allocate(bufferSize);
		decoder = charset.newDecoder();
		readLines = new LinkedList<String>();
	}

	/**
	 * Write the given byte array to this helper.
	 * @param b the bytes to write.
	 */
	public void write(byte[] b) {
		write(b, 0, b.length);
	}

	/**
	 * Write the given byte array to this helper.
	 * 
	 * @param b
	 *            the bytes to write
	 * @param offset
	 *            the starting index in the buffer, inclusive
	 * @param length
	 *            the total number of bytes to read from the given buffer
	 */
	public void write(byte[] b, int offset, int length) {
		while (length > 0) {
			// Write whatever we can:
			int l = Math.min(buffer.remaining(), length);
			//System.out.println("Writing " + l + " bytes...");
			buffer.put(b, offset, l);
			// Update what we've written:
			offset += l;
			length -= l;
			//System.out.println("Input is now at " + offset + ", " + length + " remaining...");
			// And process whatever we can:
			processBytes(false);
		}
	}

	/**
	 * Indicates that all character writing has completed, and whatever is left
	 * should be treated as one final line.
	 */
	public void finish() {
		processBytes(true);
		// Whatever characters are left, use them.
		characters.flip();
		if (characters.hasRemaining()) {
			char[] chs = new char[characters.remaining()];
			characters.get(chs);
			newLine(chs);
			characters.clear();
		}
	}

	private void processBytes(boolean done) {
		// Flip the buffer for reading...
		buffer.flip();
		//System.out.println("Decoding " + buffer.remaining() + " bytes, have " + characters.remaining() + " characters left...");
		decoder.decode(buffer, characters, done);
		buffer.compact();
		// Go through the characters we've read and see if we can find a line
		characters.flip();
		int start = 0;
		//System.out.println("Looking for lines through " + characters.remaining() + " characters.");
		while (characters.hasRemaining()) {
			char c = characters.get();
			if (c == '\r' || c == '\n') {
				// Dump the current position.
				int ip = characters.position();
				if (c == '\r') {
					// If the character is a carriage return, see if we can peak at
					// the next character. If it's a newline, eat it. Otherwise,
					// "unread" it.
					if (characters.hasRemaining()) {
						if (characters.get() != '\n') {
							characters.position(characters.position()-1);
						}
					} else {
						// Ooops... can't peak... give up for now.
						break;
					}
					c = '\n';
				}
				// Final position may be after the eaten newline
				int fp = characters.position();
				//System.out.println("New line at " + ip + ", start is " + start);
				// A new line.
				char[] chs = new char[ip - start - 1];
				characters.position(start);
				characters.get(chs);
				//System.out.println("Read " + chs.length + " characters, now at " + characters.position());
				start = fp;
				characters.position(fp);
				newLine(chs);
			}
		}
		//System.out.println("Scan completed, advanced to " + start + ", now have " + readLines.size() + " lines buffered.");
		// start has moved as far as we can, so move position back to it
		characters.position(start);
		// And compact
		characters.compact();
		if (!characters.hasRemaining()) {
			// If that didn't work, throw them into a temp buffer
			characters.flip();
			//System.out.println("OVERFLOW! Throwing " + characters.remaining() + " into overflow buffer.");
			char[] chs = new char[characters.remaining()];
			characters.get(chs);
			characters.clear();
			if (currentLine == null) {
				currentLine = new StringBuilder(chs.length);
			}
			currentLine.append(chs);
		}
	}

	private void newLine(char[] chs) {
		if (currentLine != null) {
			// If the current line overflowed our buffer, we'll have this
			// for scratch space.
			currentLine.append(chs);
			readLines.add(currentLine.toString());
			currentLine = null;
		} else {
			// Otherwise, just add it directly.
			readLines.add(new String(chs));
		}
	}

	/**
	 * Attempts to read a line. After a call to {@link #write(byte[], int, int)}
	 * , lines may be read. This will return {@code null} if no line is
	 * available. Multiple calls to {@code write(byte[], int, int)} may not
	 * produce lines. This method will not return the terminating newline.
	 * 
	 * @return the next line read, or {@code null} if no lines are currently
	 *         available
	 */
	public String readLine() {
		return readLines.poll();
	}
}
