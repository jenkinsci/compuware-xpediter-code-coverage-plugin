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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.lang.StringUtils;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.compuware.jenkins.build.utils.CodeCoverageConstants;
import com.compuware.jenkins.common.configuration.CpwrGlobalConfiguration;
import com.compuware.jenkins.common.configuration.HostConnection;
import com.compuware.jenkins.common.utils.ArgumentUtils;
import com.compuware.jenkins.common.utils.CommonConstants;
import hudson.AbortException;
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
	// Member Variables
	private CodeCoverageBuilder m_ccBuilder;

	/**
	 * Constructor.
	 * 
	 * @param config
	 *            the <code>CodeCoverageBuilder</code> to use for scanning
	 */
	public CodeCoverageScanner(CodeCoverageBuilder config)
	{
		m_ccBuilder = config;
	}

	/**
	 * Performs the Code Coverage scan.
	 * 
	 * @param run
	 *            the current running Jenkins build
	 * @param workspace
	 *            the Jenkins job workspace directory
	 * @param launcher
	 *            the way to start a process
	 * @param listener
	 *            the build listener
	 * 
	 * @throws IOException
	 *             if an error occurs performing the scan
	 * @throws InterruptedException
	 *             if the user cancels the scan
	 */
	public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener)
			throws IOException, InterruptedException
	{
		// obtain argument values to pass to the CLI
		PrintStream logger = listener.getLogger();
		CpwrGlobalConfiguration globalConfig = CpwrGlobalConfiguration.get();
		VirtualChannel vChannel = launcher.getChannel();
		Properties remoteProperties = vChannel.call(new RemoteSystemProperties());
		String remoteFileSeparator = remoteProperties.getProperty(CommonConstants.FILE_SEPARATOR_PROPERTY_KEY);
		boolean isShell = launcher.isUnix();
		String osFile = isShell ? CodeCoverageConstants.CODE_COVERAGE_CLI_SH : CodeCoverageConstants.CODE_COVERAGE_CLI_BAT;
		String cliScriptFile = globalConfig.getTopazCLILocation(launcher) + remoteFileSeparator + osFile;
		logger.println("cliScriptFile: " + cliScriptFile); //$NON-NLS-1$
		String cliScriptFileRemote = ArgumentUtils.escapeForScript(new FilePath(vChannel, cliScriptFile).getRemote());
		logger.println("cliScriptFileRemote: " + cliScriptFileRemote); //$NON-NLS-1$
		HostConnection connection = globalConfig.getHostConnection(m_ccBuilder.getConnectionId());
		String host = ArgumentUtils.escapeForScript(connection.getHost());
		String port = ArgumentUtils.escapeForScript(connection.getPort());
		StandardUsernamePasswordCredentials credentials = globalConfig.getLoginInformation(run.getParent(),
				m_ccBuilder.getCredentialsId());
		String userId = ArgumentUtils.escapeForScript(credentials.getUsername());
		String password = ArgumentUtils.escapeForScript(credentials.getPassword().getPlainText());
		String codePage = connection.getCodePage();
		String timeout = connection.getTimeout();
		String targetFolder = ArgumentUtils.escapeForScript(workspace.getRemote());
		String topazCliWorkspace = ArgumentUtils
				.escapeForScript(workspace.getRemote() + remoteFileSeparator + CommonConstants.TOPAZ_CLI_WORKSPACE);
		logger.println("topazCliWorkspace: " + topazCliWorkspace); //$NON-NLS-1$
		String analysisPropertiesPath = m_ccBuilder.getAnalysisPropertiesPath();
		String analysisPropertiesStr = m_ccBuilder.getAnalysisProperties();
		Properties analysisProperties = buildAnalysisProperties(analysisPropertiesPath, analysisPropertiesStr, workspace,
				logger);

		// build the list of arguments to pass to the CLI
		ArgumentListBuilder args = new ArgumentListBuilder();
		args.add(cliScriptFileRemote);
		args.add(CommonConstants.HOST_PARM, host);
		args.add(CommonConstants.PORT_PARM, port);
		args.add(CommonConstants.USERID_PARM, userId);
		args.add(CommonConstants.PW_PARM);
		args.add(password, true);
		args.add(CommonConstants.CODE_PAGE_PARM, codePage);
		args.add(CommonConstants.TIMEOUT_PARM, timeout);
		args.add(CommonConstants.TARGET_FOLDER_PARM, targetFolder);
		args.add(CommonConstants.DATA_PARM, topazCliWorkspace);

		logger.print("Analysis properties after parsing/merging: "); //$NON-NLS-1$
		for (Map.Entry<?, ?> entry : analysisProperties.entrySet())
		{
			String key = (String) entry.getKey();
			String value = (String) entry.getValue();
			logger.print(key + '=' + value + ' ');

			// don't add properties that don't have values
			if (StringUtils.isNotBlank(value))
			{
				if (key.equals(CodeCoverageConstants.SOURCES_PARM))
				{
					value = ArgumentUtils.escapeCommaDelimitedPathsForScript(value);
				}
				else
				{
					value = ArgumentUtils.escapeForScript(value);
				}
				key = ArgumentUtils.prefixWithDash((String) key);

				args.add(key, value);
			}
		}
		logger.println();

		// create the CLI workspace (in case it doesn't already exist)
		EnvVars env = run.getEnvironment(listener);
		FilePath workDir = new FilePath(vChannel, workspace.getRemote());
		workDir.mkdirs();

		// invoke the CLI (execute the batch/shell script)
		int exitValue = launcher.launch().cmds(args).envs(env).stdout(logger).pwd(workDir).join();
		if (exitValue != 0)
		{
			throw new AbortException("Call " + osFile + " exited with value = " + exitValue); //$NON-NLS-1$ //$NON-NLS-2$
		}
		else
		{
			logger.println("Call " + osFile + " exited with value = " + exitValue); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	/**
	 * Builds and returns a list of properties using the given analysis file path and string.
	 * <p>
	 * Properties in the given string take precedence over properties in the file located at the given path.
	 * 
	 * @param analysisPropertiesFilePath
	 *            the <code>String</code> path of a file containing analysis properties
	 * @param analysisPropertiesStr
	 *            the <code>String</code> containing analysis properties
	 * @param workspace
	 *            the workspace directory
	 * @param logger
	 *            the <code>PrintStream</code> to use for capturing log statements
	 * 
	 * @return the built <code>Properties</code>
	 */
	protected Properties buildAnalysisProperties(String analysisPropertiesFilePath, String analysisPropertiesStr,
			FilePath workspace, PrintStream logger)
	{
		Properties analysisProperties = new Properties();

		// get properties from the file
		Path filePath = null;

		boolean filePathSpecified = StringUtils.isNotBlank(analysisPropertiesFilePath);
		if (filePathSpecified)
		{
			// the path specified can be absolute or relative to the workspace
			filePath = Paths.get(analysisPropertiesFilePath);
			if (!filePath.isAbsolute())
			{
				filePath = Paths.get(workspace.getRemote(), analysisPropertiesFilePath);
			}
		}
		else
		{
			// the user did not specify a file path, so use the default file path
			filePath = Paths.get(workspace.getRemote(), CodeCoverageConstants.DEFAULT_ANALYSIS_PROPERTIES_FILE_NAME);
		}

		byte[] bytes = null;
		try
		{
			logger.println("Analysis properties file path: " + filePath.toAbsolutePath()); //$NON-NLS-1$
			bytes = Files.readAllBytes(filePath);
		}
		catch (IOException e)
		{
			logger.println("An IOException occurred while obtaining analysis properties from the file: " + e.toString()); //$NON-NLS-1$
			if (filePathSpecified)
			{
				e.printStackTrace(logger);
			}
		}

		if (bytes != null)
		{
			try
			{
				String filePropertiesStr = new String(bytes, CommonConstants.UTF_8);
				logger.println("Analysis properties string from file: " + filePropertiesStr); //$NON-NLS-1$
				Properties fileProperties = ArgumentUtils.convertStringToProperties(filePropertiesStr);
				analysisProperties.putAll(fileProperties);
			}
			catch (IOException e)
			{
				logger.println("An IOException occurred while parsing analysis properties from the file: " + e.toString()); //$NON-NLS-1$
				e.printStackTrace(logger);
			}
		}

		// get properties from the string (these take precedence, so load them after the file properties)
		if (StringUtils.isNotBlank(analysisPropertiesStr))
		{
			try
			{
				logger.println("Analysis properties string from UI: " + analysisPropertiesStr); //$NON-NLS-1$
				Properties strProperties = ArgumentUtils.convertStringToProperties(analysisPropertiesStr);
				analysisProperties.putAll(strProperties);
			}
			catch (IOException e)
			{
				logger.println("An IOException occurred while obtaining analysis properties from the UI: " + e.toString()); //$NON-NLS-1$
				e.printStackTrace(logger);
			}
		}

		return analysisProperties;
	}
}