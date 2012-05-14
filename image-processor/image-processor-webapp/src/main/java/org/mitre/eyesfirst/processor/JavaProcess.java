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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Class used to invoke a separate Java VM to run some bit of code.
 * @author dpotter
 */
public abstract class JavaProcess extends ExternalProcess {
	/**
	 * Create the classpath to use for the child VM. The default implementation
	 * simply returns the result from {@link #getClassPath()} to reuse the
	 * current classpath.
	 * @return the classpath to use.
	 */
	protected List<File> createClassPath() {
		return getClassPath();
	}

	/**
	 * Gets the current classpath as a list. The returned list is mutable and
	 * is based on the current value of the {@code "java.class.path"} property
	 * when the method is invoked.
	 * @return
	 */
	public static List<File> getClassPath() {
		String cp = System.getProperty("java.class.path");
		if (cp == null) {
			// None? OK.
			return new ArrayList<File>();
		}
		return parseClassPath(cp);
	}

	/**
	 * Creates a list of files based on the given File.pathSeparator deliminted
	 * set of files.
	 * @param classPath
	 * @return
	 */
	public static List<File> parseClassPath(String classPath) {
		List<File> result = new ArrayList<File>(32);
		if (classPath == null || classPath.length() == 0)
			return result;
		// We can't use split because we can't be sure that the path separator
		// isn't a metacharacter. (It probably isn't, but let's be safe.)
		int start = 0;
		int index;
		int length = classPath.length();
		while (true) {
			index = classPath.indexOf(File.pathSeparatorChar, start);
			if (index < 0)
				break;
			// Start may be the index in cases like ";;;" (assuming ';' is pathSeparator)
			// So don't bother with empty entries.
			if (start < index)
				result.add(new File(classPath.substring(start, index)));
			// Index will now point to the path separator, so move it one
			// character past.
			start = index + 1;
		}
		// We'll fall through with one last entry
		index = length;
		if (start < index)
			result.add(new File(classPath.substring(start, index)));
		return result;
	}

	/**
	 * Generates the Java command to run.
	 */
	@Override
	protected String getCommand() {
		String javaHome = System.getProperty("java.home");
		if (javaHome == null)
			throw new RuntimeException("java.home not set?");
		return javaHome + File.separator + "bin" + File.separator + "java";
	}

	/**
	 * Gets the maximum amount of memory in megabytes the forked VM should use.
	 * This is used to generate the -Xmx argument. The default is 0, which means
	 * that no -Xmx argument will be generated.
	 * <p>
	 * Note that this is invoked via {@link #createVMArguments(List)}, so if
	 * that method is overridden, this method may not be invoked at all.
	 * 
	 * @return the amount of memory to pass through the -Xmx argument, or 0 (or
	 *         any negative number) not to generate a -Xmx argument.
	 */
	protected int getMaxMemory() {
		return 0;
	}

	/**
	 * Create the VM arguments - the arguments passed to the VM and not the
	 * class. By default this uses {@link #getMaxMemory()} and
	 * {@link #createClassPath()} to generate the arguments it uses.
	 * @param arguments
	 */
	protected void createVMArguments(List<String> arguments) {
		int max = getMaxMemory();
		if (max > 0) {
			arguments.add("-Xmx" + max + "M");
		}
		List<File> cp = createClassPath();
		if (cp.size() > 0) {
			arguments.add("-cp");
			StringBuilder sb = new StringBuilder();
			for (File f : cp) {
				sb.append(f.getAbsolutePath());
				sb.append(File.pathSeparatorChar);
			}
			// Remove the last character
			sb.setLength(sb.length()-1);
			arguments.add(sb.toString());
		}
	}

	/**
	 * Create the arguments to give to the Java process. The default
	 * implementation does nothing.
	 * @param arguments
	 */
	protected void createJavaArguments(List<String> arguments) {
		// Nothing
	}

	/**
	 * Gets the class name for the class to run.
	 * @return the class name to run
	 */
	protected abstract String getClassName();

	/**
	 * Generates the arguments for the final VM. This simply invokes
	 * {@link #createVMArguments(List)}, then {@link #getClassName()}, and 
	 * finally {@link #createArguments(List)} to create the arguments.
	 */
	protected final void createArguments(List<String> arguments) {
		createVMArguments(arguments);
		arguments.add(getClassName());
		createJavaArguments(arguments);
	}
}
