package aa.worldline.vbu.infinitest;

import static java.util.Arrays.asList;

import java.applet.Applet;
import java.awt.Color;
import java.awt.Dimension;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.JApplet;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.infinitest.ConcurrencyController;
import org.infinitest.EventQueue;
import org.infinitest.InfinitestCore;
import org.infinitest.InfinitestCoreBuilder;
import org.infinitest.MultiCoreConcurrencyController;
import org.infinitest.RuntimeEnvironment;
import org.infinitest.parser.JavaClass;
import org.jgraph.JGraph;
import org.jgrapht.DirectedGraph;
import org.jgrapht.ListenableGraph;
import org.jgrapht.ext.JGraphModelAdapter;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.ListenableDirectedGraph;
import org.junit.runner.manipulation.Sortable;

import com.google.common.reflect.ClassPath;

public class Runner {
	
	public static String separatorOfJavaClassPath=";";

	public static String root;

	public static File currentJavaHome() {
		return new File(System.getProperty("java.home"));
	}

	public static String systemClasspath() {
		String classpath = null;
		try {
			FileInputStream classpathFile = new FileInputStream(root
					+ "/classpath.cp");
			classpath = IOUtils.toString(classpathFile);
		} catch (IOException e) {
			System.out
					.println("[Infinitest]Classpath.cp file doesn't exist or is unreadable: "
							+ e.getMessage() + "! \nEXIT");
			System.exit(10);
		}

		return classpath;
	}

	public static File classDirectory() {
		return new File(root + "/target/test-classes");
	}

	public static File workingDirectory() {
		return new File(root);
	}

	public static List<File> buildPaths() {
		return asList(new File(root + "/target/classes"), classDirectory());
	}

	public static Collection<File> getCollectionOfFileFromFolder(File folder) {
		// Directory path here
		System.out.println("[Infninitest debug]Folder=" + folder);
		Collection<File> listOfFiles = FileUtils.listFiles(folder,
				new org.apache.commons.io.filefilter.RegexFileFilter(
						".*\\.class"), DirectoryFileFilter.DIRECTORY);

		return listOfFiles;

	}

	public static void main(String[] args) {
		System.out.println("[Infinitest]Begin treatment");
		root = args[0];
		System.out.println("[Infinitest]The directory used is:" + root);
		final EventQueue eventQueue = null;
		final ConcurrencyController concurrencyController = new MultiCoreConcurrencyController();
		RuntimeEnvironment environment = new RuntimeEnvironment(buildPaths(),
				workingDirectory(), systemClasspath(), currentJavaHome());
		System.out.println("[Infinitest]Env: buildPaths" + buildPaths());
		System.out.println("[Infinitest]Env: PWD: " + workingDirectory());
		System.out.println("[Infinitest]Env: System CP: " + systemClasspath());
		System.out.println("[Infinitest]Env: JavaHome: " + currentJavaHome());

		InfinitestCoreBuilder coreBuilder = new InfinitestCoreBuilder(
				environment, eventQueue);
		coreBuilder.setUpdateSemaphore(concurrencyController);
		coreBuilder.setName("infinitest-lib");

		InfinitestCore core = coreBuilder.createCore();

		List<File> collectionOfFiles = environment
				.classDirectoriesInClasspath();
		collectionOfFiles.addAll(buildPaths());
		List<File> changeFiles = new ArrayList<File>();

		for (File f : collectionOfFiles) {
			changeFiles.addAll(getCollectionOfFileFromFolder(f));
		}
		System.out.println("[Infinitest debug]Files changed:" + changeFiles);
		for (File f : changeFiles) {
			List<File> newColl = new ArrayList<File>();
			newColl.add(f);
			core.update((Collection<File>) newColl);
		}
		// exec twice to have the model created to have the graph representing
		// the code made
		for (File f : changeFiles) {
			List<File> newColl = new ArrayList<File>();
			newColl.add(f);
			core.update((Collection<File>) newColl);
		}
		// a third time is necessary -> should comparison made!
		String targetDirectory = "infinitestExport/";
		new File(targetDirectory).mkdirs();
		PrintWriter writer = null;
		Comparator<File> comparator = new Comparator<File>() {
			@Override
			public int compare(File file1, File file2) {

				return file1.toString().compareTo(file2.toString());
			}
		};
		Collections.sort(changeFiles, comparator);
		executeAndSerialize(targetDirectory + "infinitestExport", core,
				changeFiles, writer);
		// executeAndSerialize("infinitestExport3", core, changeFiles, writer);
		// executeAndSerialize("infinitestExport4", core, changeFiles, writer);

		System.out.println("[Infinitest]All treatments ended!");
	}

	private static void executeAndSerialize(String name, InfinitestCore core,
			List<File> changeFiles, PrintWriter writer) {
		try {
			writer = new PrintWriter(
					name + System.currentTimeMillis() + ".txt", "UTF-8");

			for (File f : changeFiles) {
				List<File> newColl = new ArrayList<File>();
				newColl.add(f);

				Collection<JavaClass> res = core
						.update((Collection<File>) newColl);
				writer.println(f + "[");
				Comparator<JavaClass> comparator = new Comparator<JavaClass>() {
					@Override
					public int compare(JavaClass clazz1, JavaClass clazz2) {

						return clazz1.toString().compareTo(clazz2.toString());
					}
				};
				List<JavaClass> list = new ArrayList<JavaClass>(res);
				Collections.sort(list, comparator);

				for (JavaClass clazz : res) {
					writer.println("\t" + clazz + ",");
				}
				writer.println("]");
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			writer.close();
		}
		System.out.println("[Infinitest]Treatment of " + name + "ended!");
	}
}
