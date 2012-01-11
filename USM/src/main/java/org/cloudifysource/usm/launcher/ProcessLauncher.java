package org.cloudifysource.usm.launcher;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.cloudifysource.usm.USMComponent;
import org.cloudifysource.usm.USMException;


public interface ProcessLauncher extends USMComponent {

	// Process launch(UniversalServiceManagerConfiguration config, File
	// workingDir) throws USMException;

	String getCommandLine();

	// void run(String commandLine, File workingDir) throws USMException;

	Object launchProcess(final Object arg, final File workingDir)
		throws USMException;
	Object launchProcess(final Object arg, final File workingDir, final int retries, boolean redirectErrorStream, Map<String, Object> params)
			throws USMException;

	Process launchProcessAsync(final Object arg, final File workingDir, final File outputFile, final File errorFile)
	throws USMException;
	Process launchProcessAsync(final Object arg, final File workingDir, final int retries, boolean redirectErrorStream, List<String> params)
			throws USMException;

	Object launchProcess(Object arg, File workingDir, Map<String, Object> params)
			throws USMException;
}