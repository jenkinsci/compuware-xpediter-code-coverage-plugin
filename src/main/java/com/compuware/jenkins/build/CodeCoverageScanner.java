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
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.lang.StringUtils;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.compuware.jenkins.build.utils.CodeCoverageUtils;
import com.compuware.jenkins.build.utils.Constants;
import com.compuware.jenkins.common.configuration.CpwrGlobalConfiguration;
import com.compuware.jenkins.common.configuration.HostConnection;
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

		String cliScriptFile = globalConfig.getTopazCLILocation(launcher) + remoteFileSeparator + osFile;
		logger.println("cliScriptFile: " + cliScriptFile); //$NON-NLS-1$
		String cliScriptFileRemote = new FilePath(vChannel, cliScriptFile).getRemote();
		logger.println("cliScriptFileRemote: " + cliScriptFileRemote); //$NON-NLS-1$
		HostConnection connection = globalConfig.getHostConnection(m_config.getConnectionId());
		String host = CodeCoverageUtils.escapeForScript(connection.getHost(), isShell);
		String port = CodeCoverageUtils.escapeForScript(connection.getPort(), isShell);
		StandardUsernamePasswordCredentials credentials = globalConfig.getLoginInformation(run.getParent(),
				m_config.getCredentialsId());
		String codePage = CodeCoverageUtils.escapeForScript(connection.getCodePage(), isShell);
		String userId = CodeCoverageUtils.escapeForScript(credentials.getUsername(), isShell);
		String password = CodeCoverageUtils.escapeForScript(credentials.getPassword().getPlainText(), isShell);
		String topazCliWorkspace = workspace.getRemote() + remoteFileSeparator + Constants.TOPAZ_CLI_WORKSPACE;
		logger.println("topazCliWorkspace: " + topazCliWorkspace); //$NON-NLS-1$
		String analysisPropertiesPath = m_config.getAnalysisPropertiesPath();
		String analysisPropertiesStr = m_config.getAnalysisProperties();
		Properties analysisProperties = buildAnalysisProperties(analysisPropertiesPath, analysisPropertiesStr, logger);

		args.add(cliScriptFileRemote);
		args.add(Constants.HOST_PARM, host);
		args.add(Constants.PORT_PARM, port);
		args.add(Constants.CODE_PAGE_PARM, codePage);
		args.add(Constants.USERID_PARM, userId);
		args.add(Constants.PASSWORD_PARM);
		args.add(password, true);
		args.add(Constants.TARGET_FOLDER_PARM, workspace.getRemote());
		args.add(Constants.DATA_PARM, topazCliWorkspace);

		logger.print("Analysis properties after parsing/merging: "); //$NON-NLS-1$
		for (Map.Entry<?, ?> entry : analysisProperties.entrySet())
		{
			String key = prefixWithDash((String) entry.getKey());
			String value = CodeCoverageUtils.escapeForScript((String) entry.getValue(), isShell);
			logger.print(key + '=' + value + ' ');
			args.add(key, value);
		}
		logger.println();
		
		String connectionId = m_config.getConnectionId();
		logger.println("Host connection ID: " + CodeCoverageUtils.escapeForScript(connectionId, isShell)); //$NON-NLS-1$
		
		String description = CodeCoverageUtils.escapeForScript(globalConfig.getHostConnection(connectionId).getDescription(), isShell);
		logger.println("Host connection: " + description); //$NON-NLS-1$

		FilePath workDir = new FilePath(vChannel, workspace.getRemote());
		workDir.mkdirs();

		int exitValue = launcher.launch().cmds(args).envs(env).stdout(listener.getLogger()).pwd(workDir).join();
		logger.println("Call " + osFile + " exited with value = " + exitValue); //$NON-NLS-1$ //$NON-NLS-2$
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
	 * @param logger
	 *            the <code>PrintStream</code> to use for capting log statements
	 * 
	 * @return the built <code>Properties</code>
	 */
	private Properties buildAnalysisProperties(String analysisPropertiesFilePath, String analysisPropertiesStr, PrintStream logger)
	{
		Properties analysisProperties = new Properties();

		// get properties from the file
		if (StringUtils.isNotEmpty(analysisPropertiesFilePath))
		{
			try
			{
				logger.println("Analysis properties file path: " + analysisPropertiesFilePath); //$NON-NLS-1$
				byte[] bytes = Files.readAllBytes(Paths.get(analysisPropertiesFilePath));
				String filePropertiesStr = new String(bytes, Constants.UTF_8);
				logger.println("Analysis properties string from file: " + filePropertiesStr); //$NON-NLS-1$
				Properties fileProperties = convertStringToProperties(filePropertiesStr);
				analysisProperties.putAll(fileProperties);
			}
			catch (IOException e)
			{
				logger.print(e.getMessage());
			}
		}

		// get properties from the string (these take precedence, so load them after the file properties)
		if (StringUtils.isNotEmpty(analysisPropertiesStr))
		{
			try
			{
				logger.println("Analysis properties string from UI: " + analysisPropertiesStr); //$NON-NLS-1$
				Properties strProperties = convertStringToProperties(analysisPropertiesStr);
				analysisProperties.putAll(strProperties);
			}
			catch (IOException e)
			{
				logger.print(e.getMessage());
			}
		}

		return analysisProperties;
	}

	/**
	 * Converts the given properties string to a properties object.
	 * 
	 * @param propertiesString
	 *            the <code>String</code> to convert
	 * 
	 * @return the <code>Properties</code> object
	 * 
	 * @throws IOException
	 *             if an error occurred during conversion
	 */
	private Properties convertStringToProperties(String propertiesString) throws IOException
	{
		Properties properties = new Properties();

		Reader reader = new StringReader(propertiesString);
		properties.load(reader);

		return properties;
	}

	/**
	 * Prefixes the given property with a dash (-).
	 * <p>
	 * If the property is already prefixed with a dash, the propery is returned.
	 * 
	 * @param property
	 *            the <code>String</code> property
	 * 
	 * @return the prefixed <code>String</code> property
	 */
	private String prefixWithDash(String property)
	{
		String prefixedProperty = property;

		if (!StringUtils.startsWith(property, Constants.DASH))
		{
			prefixedProperty = Constants.DASH + prefixedProperty;
		}

		return prefixedProperty;
	}
}