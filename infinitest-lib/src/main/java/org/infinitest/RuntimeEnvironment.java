/*
 * Infinitest, a Continuous Test Runner.
 *
 * Copyright (C) 2010-2013
 * "Ben Rady" <benrady@gmail.com>,
 * "Rod Coffin" <rfciii@gmail.com>,
 * "Ryan Breidenbach" <ryan.breidenbach@gmail.com>
 * "David Gageot" <david@gageot.net>, et al.
 *
 * Copyright (C) 2014-2017 Atos Worldline
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.infinitest;

import static com.google.common.collect.Lists.*;
import static com.google.common.collect.Maps.*;
import static java.io.File.*;
import static java.util.logging.Level.*;
import static org.infinitest.util.InfinitestUtils.*;

import java.io.*;
import java.util.*;

import org.infinitest.testrunner.*;

import com.google.common.annotations.*;

/**
 * Defines the runtime environment for test execution.
 * 
 * @author bjrady
 */
public class RuntimeEnvironment implements ClasspathProvider {
	private final int heapSize = 256;
	private final File javaHome;
	private final File workingDirectory;
	private final List<File> classOutputDirs;
	private final String rawClasspath;
	private final List<String> additionalArgs;
	private String infinitestRuntimeClasspath;
	private List<File> classDirs;
	private final CustomJvmArgumentsReader customArgumentsReader;

	/**
	 * Creates a new environment for test execution.
	 * 
	 * @param classOutputDirs
	 *            A list of class directories containing classes generated by an
	 *            IDE or compiler
	 * @param workingDirectory
	 *            The "Current Directory" used to resolve relative paths when
	 *            running tests
	 * @param classpath
	 *            The classpath used to launch the testrunner process (must
	 *            include directories in classOutputDirs)
	 * @param javaHome
	 *            The location of the JDK home directory, similar to the
	 *            JAVA_HOME environment variable.
	 */
	public RuntimeEnvironment(List<File> classOutputDirs, File workingDirectory, String classpath, File javaHome) {
		this.classOutputDirs = classOutputDirs;
		this.workingDirectory = workingDirectory;
		rawClasspath = classpath;
		this.javaHome = javaHome;
		infinitestRuntimeClasspath = classpath;
		additionalArgs = newArrayList();
		customArgumentsReader = new FileCustomJvmArgumentReader(workingDirectory);
	}

	public List<String> createProcessArguments() {
		String memorySetting = "-mx" + getHeapSize() + "m";
		List<String> args = newArrayList(getJavaExecutable(), memorySetting);
		args.addAll(additionalArgs);
		args.addAll(addCustomArguments());
		return args;
	}

	public Map<String, String> createProcessEnvironment() {
		Map<String, String> environment = newHashMap();
		environment.put("CLASSPATH", getCompleteClasspath());
		return environment;
	}

	private List<String> addCustomArguments() {
		return customArgumentsReader.readCustomArguments();
	}

	/**
	 * The classpath that is used to launch Infinitest, not the classpath of the
	 * project that you're trying to test
	 */
	public void setInfinitestRuntimeClassPath(String infinitestRuntimeClassPath) {
		infinitestRuntimeClasspath = infinitestRuntimeClassPath;
	}

	@Override
	public String getCompleteClasspath() {
		String completeClasspath = getRawClasspath();
		String infinitestJarPath = findInfinitestJar();
		log(CONFIG, "Found infinitest jar classpath entry at " + infinitestJarPath);
		if (infinitestJarPath != null) {
			completeClasspath = completeClasspath + File.pathSeparator + infinitestJarPath;
		} else {
			log(SEVERE, "Could not find a classpath entry for Infinitest Core in " + infinitestRuntimeClasspath);
		}
		validateClasspath(completeClasspath);
		return completeClasspath;
	}

	@VisibleForTesting
	public String findInfinitestJar() {
		return findClasspathEntryFor(infinitestRuntimeClasspath, TestRunnerProcess.class);
	}

	private void validateClasspath(String completeClasspath) {
		for (String entry : getClasspathElements(completeClasspath)) {
			if (!(new File(getWorkingDirectory(), entry).exists() || new File(entry).exists())) {
				log(WARNING, "Could not find classpath entry [" + entry + "] at file system root or relative to " + "working directory [" + getWorkingDirectory() + "].");
			}
		}
	}

	private List<String> getClasspathElements(String classpath) {
		return newArrayList(classpath.split(File.pathSeparator));
	}

	private String getJavaExecutable() {
		File javaExecutable = createJavaExecutableFile("java");
		if (!javaExecutable.exists()) {
			javaExecutable = createJavaExecutableFile("java.exe");
			if (!javaExecutable.exists()) {
				throw new JavaHomeException(javaExecutable);
			}
		}
		return javaExecutable.getAbsolutePath();
	}

	private File createJavaExecutableFile(String fileName) {
		File javaExecutable = new File(javaHome.getAbsolutePath() + separator + "bin" + separator + fileName);
		return javaExecutable;
	}

	/**
	 * The heap size, in megabytes, that will be used when launching the test
	 * runner process.
	 */
	public int getHeapSize() {
		return heapSize;
	}

	/**
	 * The working directory that will be used when launching the test runner
	 * process. That is, if a test run by the core creates a new File object
	 * like: <code>
	 * new File(".");
	 * </code> It will be equal to this directory
	 */
	public File getWorkingDirectory() {
		return workingDirectory;
	}

	@Override
	public List<File> getClassOutputDirs() {
		return classOutputDirs;
	}

	private String getRawClasspath() {
		return rawClasspath;
	}

	public void addVMArgs(List<String> newArgs) {
		additionalArgs.addAll(newArgs);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof RuntimeEnvironment) {
			RuntimeEnvironment other = (RuntimeEnvironment) obj;
			return other.classOutputDirs.equals(classOutputDirs) && other.workingDirectory.equals(workingDirectory) && other.rawClasspath.equals(rawClasspath) && other.javaHome.equals(javaHome) && other.additionalArgs.equals(additionalArgs);
		}
		return false;
	}

	@Override
	public int hashCode() {
		// CHECKSTYLE:OFF
		return classOutputDirs.hashCode() ^ additionalArgs.hashCode() ^ javaHome.hashCode() ^ workingDirectory.hashCode() ^ rawClasspath.hashCode();
		// CHECKSTYLE:ON
	}

	@Override
	public List<File> classDirectoriesInClasspath() {
		// RISK Caching this prevents tons of disk access, but we risk caching a
		// bad set of
		// classDirs
		if (classDirs == null) {
			classDirs = newArrayList();
			for (String each : getClasspathElements(rawClasspath)) {
				File classEntry = new File(each);
				if (classEntry.isDirectory()) {
					classDirs.add(classEntry);
				}
			}
		}
		return classDirs;
	}
}
