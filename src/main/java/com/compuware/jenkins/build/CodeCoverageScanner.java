/**
 * These materials contain confidential information and trade secrets of Compuware Corporation. You shall maintain the materials
 * as confidential and shall not disclose its contents to any third party except as may be required by law or regulation. Use,
 * disclosure, or reproduction is prohibited without the prior express written permission of Compuware Corporation.
 * 
 * All Compuware products listed within the materials are trademarks of Compuware Corporation. All other company or product
 * names are trademarks of their respective owners.
 * 
 * Copyright (c) 2017 Compuware Corporation. All rights reserved.
 */
package com.compuware.jenkins.build;

import java.io.IOException;
import java.io.PrintStream;
import com.compuware.jenkins.build.utils.CodeCoverageUtils;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;

/**
 * Class used to initiate a Code Coverage scan. This class will utilize the Topaz command line interface to do the scan.
 */
public class CodeCoverageScanner
{
	private CodeCoverageBuilder m_config;

	public CodeCoverageScanner(CodeCoverageBuilder config)
	{
		m_config = config;
	}

	/**
	 * Perform the Code Coveage scan.
	 * 
	 * @param run
	 *            the current running Jenkins build
	 * @param launcher
	 *            the machine that the files will be checked out.
	 * @param workspace
	 *            a directory to check out the source code.
	 * @param launcher
	 *            a way to start a process
	 * @param listener
	 *            build listener
	 * 
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener)
			throws IOException, InterruptedException
	{
		PrintStream logger = listener.getLogger();

		// TODO (pfhjyg0) : Get command setup information once 'common' plugin is created
		// ArgumentListBuilder args = new ArgumentListBuilder();
		// EnvVars env = run.getEnvironment(listener);
		// VirtualChannel vChannel = launcher.getChannel();
		// Properties remoteProperties = vChannel.call(new RemoteSystemProperties());
		// String remoteFileSeparator = remoteProperties.getProperty(Constants.FILE_SEPARATOR);
		boolean isShell = launcher.isUnix();
		// String osFile = isShell ? Constants.TOPAZ_CLI_SH : Constants.TOPAZ_CLI_BAT;

		// TODO (pfhjyg0) : get the global Topaz CLI information from the 'common' plugin when it is created
		// String cliScriptFile = m_config.getTopazCLILocation(launcher) + remoteFileSeparator + osFile;
		// logger.println("cliScriptFile: " + cliScriptFile); //$NON-NLS-1$
		//
		// String cliScriptFileRemote = new FilePath(vChannel, cliScriptFile).getRemote();
		// logger.println("cliScriptFileRemote: " + cliScriptFileRemote); //$NON-NLS-1$

		// TODO (pfhjyg0) : Log all UI parameters for now to show task completion; possibly remove the logging once next phase
		// is ironed out
		logger.println("Performing Code Coverage..."); //$NON-NLS-1$

		String hostConnection = CodeCoverageUtils.escapeForScript(m_config.getHostConnection().getDescription(), isShell);
		String connectionId = CodeCoverageUtils.escapeForScript(m_config.getConnectionId(), isShell);
		logger.println("Host connection: " + hostConnection); //$NON-NLS-1$
		logger.println("Host connection ID: " + connectionId); //$NON-NLS-1$

		String credentialsId = CodeCoverageUtils.escapeForScript(m_config.getCredentialsId(), isShell);
		logger.println("Login credentials: " + credentialsId); //$NON-NLS-1$

		String analysisPropertiesPath = CodeCoverageUtils.escapeForScript(m_config.getAnalysisPropertiesPath(), isShell);
		logger.println("Analysis properties path: " + analysisPropertiesPath); //$NON-NLS-1$

		String analysisProperties = CodeCoverageUtils.escapeForScript(m_config.getAnalysisProperties(), isShell);
		logger.println("Analysis properties: " + analysisProperties); //$NON-NLS-1$

		// TODO (pfhjyg0) : fill out arguments and launch the command through the CLI once the next phase has been ironed out
		// TODO (pfhjyg0) : get the global Topaz CLI information from the 'common' plugin when it is created
		// String topazCliWorkspace = workspace.getRemote() + remoteFileSeparator + Constants.TOPAZ_CLI_WORKSPACE;
		// logger.println("topazCliWorkspace: " + topazCliWorkspace); //$NON-NLS-1$
		//
		// args.add(cliScriptFileRemote);
		// args.add(Constants.HOST_CONNECTION_PARM, hostConnection);
		// args.add(Constants.ANALYSIS_PROPERTIES_PATH_PARM, analysisPropertiesPath);
		// args.add(Constants.ANALYSIS_PROPERTIES_PARM, analysisProperties);
		//
		// FilePath workDir = new FilePath(vChannel, workspace.getRemote());
		//
		// int exitValue = launcher.launch().cmds(args).envs(env).stdout(listener.getLogger()).pwd(workDir).join();
		// logger.println("Call " + osFile + " exited with value = " + exitValue); //$NON-NLS-1$ //$NON-NLS-2$
	}
}
