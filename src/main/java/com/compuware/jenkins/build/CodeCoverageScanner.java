/**
 * The MIT License (MIT)
 * 
 * Copyright (c) 2017 Compuware Corporation
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions: The above copyright notice and this permission notice
 * shall be included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.compuware.jenkins.build;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Properties;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.compuware.jenkins.build.utils.CodeCoverageUtils;
import com.compuware.jenkins.build.utils.Constants;
import com.compuware.jenkins.common.configuration.CpwrGlobalConfiguration;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.util.ArgumentListBuilder;

/**
 * Class used to initiate a Code Coverage scan. This class will utilize the Topaz command line interface to do the scan.
 */
public class CodeCoverageScanner
{
	private CodeCoverageBuilder m_config;

	/**
	 * Constructor.
	 * 
	 * @param config
	 *            the <code>CodeCoverageBuilder</code> to use for scanning
	 */
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
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener)
			throws IOException, InterruptedException
	{
		PrintStream logger = listener.getLogger();
		CpwrGlobalConfiguration globalConfig = CpwrGlobalConfiguration.get();

		ArgumentListBuilder args = new ArgumentListBuilder();
		EnvVars env = run.getEnvironment(listener);
		VirtualChannel vChannel = launcher.getChannel();
		Properties remoteProperties = vChannel.call(new RemoteSystemProperties());
		String remoteFileSeparator = remoteProperties.getProperty(Constants.FILE_SEPARATOR);
		boolean isShell = launcher.isUnix();
		String osFile = isShell ? Constants.TOPAZ_CLI_SH : Constants.TOPAZ_CLI_BAT;

<<<<<<< HEAD
		String cliScriptFile = globalConfig.getTopazCLILocation(launcher) + remoteFileSeparator + osFile;
		logger.println("cliScriptFile: " + cliScriptFile); //$NON-NLS-1$
		String cliScriptFileRemote = new FilePath(vChannel, cliScriptFile).getRemote();
		logger.println("cliScriptFileRemote: " + cliScriptFileRemote); //$NON-NLS-1$
		String host = CodeCoverageUtils.escapeForScript(m_config.getHost(), isShell);
		String port = CodeCoverageUtils.escapeForScript(m_config.getPort(), isShell);
		StandardUsernamePasswordCredentials credentials = globalConfig.getLoginInformation(run.getParent(),
				m_config.getCredentialsId());
		String userId = CodeCoverageUtils.escapeForScript(credentials.getUsername(), isShell);
		String password = CodeCoverageUtils.escapeForScript(credentials.getPassword().getPlainText(), isShell);
=======
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
>>>>>>> refs/remotes/origin/CWE-115482_PullHostConnectionsFromGlobalCfg

		String analysisPropertiesPath = CodeCoverageUtils.escapeForScript(m_config.getAnalysisPropertiesPath(), isShell);
		logger.println("Analysis properties path: " + analysisPropertiesPath); //$NON-NLS-1$

		String analysisProperties = CodeCoverageUtils.escapeForScript(m_config.getAnalysisProperties(), isShell);
		logger.println("Analysis properties: " + analysisProperties); //$NON-NLS-1$

		// args.add(cliScriptFileRemote);
		// args.add(Constants.HOST_PARM, host);
		// args.add(Constants.PORT_PARM, port);
		// args.add(Constants.USERID_PARM, userId);
		// args.add(Constants.PASSWORD_PARM);
		// args.add(password, true);
		// args.add(Constants.FILTER_PARM, cdDatasets);
		// args.add(Constants.TARGET_FOLDER_PARM, workspaceFilePath.getRemote());
		// args.add(Constants.SCM_TYPE_PARM, Constants.PDS);
		// args.add(Constants.FILE_EXT_PARM, fileExtension);
		// args.add(Constants.CODE_PAGE_PARM, codePage);
		// args.add(Constants.DATA_PARM, topazCliWorkspace);

		FilePath workDir = new FilePath(vChannel, workspace.getRemote());
		workDir.mkdirs();

		int exitValue = launcher.launch().cmds(args).envs(env).stdout(listener.getLogger()).pwd(workDir).join();
		logger.println("Call " + osFile + " exited with value = " + exitValue); //$NON-NLS-1$ //$NON-NLS-2$
	}
}