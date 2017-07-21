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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.kohsuke.stapler.Stapler;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import com.compuware.jenkins.build.CodeCoverageBuilder.CodeCoverageDescriptorImpl;
import com.compuware.jenkins.common.configuration.CpwrGlobalConfiguration;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * CodeCoverageBuilder unit tests.
 */
@SuppressWarnings("nls")
public class CodeCoverageBuilderTest
{
	// Constants
	private static final String CONNECTION_ID = "12345";
	private static final String CREDENTIALS_ID = "67890";
	private static final String ANALYSIS_PROPERTIES_FILEPATH = "\\a\\path\\to\\analysis\\properties\\ccanalysis.properties";
	private static final String ANALYSIS_PROEPRTIES_STRING = "cc.sources=\ncc.repos=\ncc.system=\ncc.test=\ncc.ddio.overrides=";
	
	private static final String HOST = "cw01";
	private static final String PORT = "30947";
	private static final String CODE_PAGE = "1047";
	private static final String TIMEOUT = "123";
	private static final String USER_ID = "xdevreg";
	private static final String PASSWORD = "********";

	// Member Variables
	@Rule
	public JenkinsRule m_jenkinsRule = new JenkinsRule();
	private CpwrGlobalConfiguration m_globalConfig;

	@Before
	public void setup()
	{
		try
		{
			JSONObject hostConnection = new JSONObject();
			hostConnection.put("description", "TestConnection");
			hostConnection.put("hostPort", HOST + ':' + PORT);
			hostConnection.put("codePage", CODE_PAGE);
			hostConnection.put("timeout", TIMEOUT);
			hostConnection.put("connectionId", CONNECTION_ID);

			JSONArray hostConnections = new JSONArray();
			hostConnections.add(hostConnection);

			JSONObject json = new JSONObject();
			json.put("hostConn", hostConnections);
			json.put("topazCLILocationLinux", "/opt/Compuware/TopazCLI");
			json.put("topazCLILocationWindows", "C:\\Program Files\\Compuware\\Topaz Workbench CLI");

			m_globalConfig = CpwrGlobalConfiguration.get();
			m_globalConfig.configure(Stapler.getCurrentRequest(), json);

			SystemCredentialsProvider.getInstance().getCredentials()
					.add(new UsernamePasswordCredentialsImpl(CredentialsScope.USER, CREDENTIALS_ID, null, USER_ID, PASSWORD));
			SystemCredentialsProvider.getInstance().save();
		}
		catch (Exception e)
		{
			// Add the print of the stacktrace because the exception message is not enough to troubleshoot the root issue. For
			// example, if the exception is constructed without a message, you get no information from executing fail().
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	/**
	 * Tests the construction of a builder, verifying configuration values.
	 */
	@Test
	public void constructBuilderTest()
	{
		CodeCoverageBuilder builder = new CodeCoverageBuilder(CONNECTION_ID, CREDENTIALS_ID, ANALYSIS_PROPERTIES_FILEPATH,
				ANALYSIS_PROEPRTIES_STRING);

		assertThat(String.format("Expected CodeCoverageBuilder.getConnectionId() to return %s", CONNECTION_ID),
				builder.getConnectionId(), is(equalTo(CONNECTION_ID)));

		assertThat(String.format("Expected CodeCoverageBuilder.getCredentialsId() to return %s", CREDENTIALS_ID),
				builder.getCredentialsId(), is(equalTo(CREDENTIALS_ID)));

		assertThat(
				String.format("Expected CodeCoverageBuilder.getAnalysisPropertiesPath() to return %s",
						ANALYSIS_PROPERTIES_FILEPATH),
				builder.getAnalysisPropertiesPath(), is(equalTo(ANALYSIS_PROPERTIES_FILEPATH)));

		assertThat(
				String.format("Expected CodeCoverageBuilder.getAnalysisProperties() to return %s", ANALYSIS_PROEPRTIES_STRING),
				builder.getAnalysisProperties(), is(equalTo(ANALYSIS_PROEPRTIES_STRING)));
	}

	/**
	 * Tests build descriptor values, such as default values and display name.
	 */
	@Test
	public void descriptorValuesTest()
	{
		try
		{
			CodeCoverageDescriptorImpl descriptor = new CodeCoverageDescriptorImpl();

			String displayName = descriptor.getDisplayName();
			assertThat("Expected CodeCoverageBuilder.DescriptorImpl.getDisplayName() to not be null.", displayName,
					is(notNullValue()));
			assertThat("Expected CodeCoverageBuilder.DescriptorImpl.getDisplayName() to not be empty.", displayName.isEmpty(),
					is(false));

			String defaultAnalysisProperties = descriptor.getDefaultAnalysisProperties();
			assertThat("Expected CodeCoverageBuilder.DescriptorImpl.getDefaultAnalysisProperties() to not be null.",
					defaultAnalysisProperties, is(notNullValue()));
			assertThat("Expected CodeCoverageBuilder.DescriptorImpl.getDefaultAnalysisProperties() to not be empty.",
					defaultAnalysisProperties.isEmpty(), is(false));
		}
		catch (Exception e)
		{
			// Add the print of the stacktrace because the exception message is not enough to troubleshoot the root issue. For
			// example, if the exception is constructed without a message, you get no information from executing fail().
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	/**
	 * Tests the results of an execution.
	 * <p>
	 * A project is created, configured and executed where the log is examined to verify results.
	 */
	@Test
	public void executionTest()
	{
		try
		{
			FreeStyleProject project = m_jenkinsRule.createFreeStyleProject("TestProject");
			project.getBuildersList().add(new CodeCoverageBuilder(CONNECTION_ID, CREDENTIALS_ID, ANALYSIS_PROPERTIES_FILEPATH,
					ANALYSIS_PROEPRTIES_STRING));

			// don't expect the build to succeed since no CLI exists
			if (project.scheduleBuild(null))
			{
				while (project.getLastCompletedBuild() == null)
				{
					// wait for the build to complete before obtaining the log
					continue;
				}

				FreeStyleBuild build = project.getLastCompletedBuild();
				String logFileOutput = JenkinsRule.getLog(build);
				
				String expectedConnectionStr = String.format("-host \"%s\" -port \"%s\"", HOST, PORT);
				assertThat("Expected log to contain Host connection: " + expectedConnectionStr + '.', logFileOutput,
						containsString(expectedConnectionStr));

				String expectedCodePageStr = String.format("-code %s", CODE_PAGE);
				assertThat("Expected log to contain Host code page: " + expectedCodePageStr + '.', logFileOutput,
						containsString(expectedCodePageStr));

				String expectedTimeoutStr = String.format("-timeout \"%s\"", TIMEOUT);
				assertThat("Expected log to contain Host timeout: " + expectedTimeoutStr + '.', logFileOutput,
						containsString(expectedTimeoutStr));

				String expectedCredentialsStr = String.format("-id \"%s\" -pass %s", USER_ID, PASSWORD);
				assertThat("Expected log to contain Login credentials: " + expectedCredentialsStr + '.', logFileOutput,
						containsString(expectedCredentialsStr));

				assertThat(String.format("Expected log to contain Analysis properties path: \"%s\".",
						ANALYSIS_PROPERTIES_FILEPATH), logFileOutput, containsString(ANALYSIS_PROPERTIES_FILEPATH));

				assertThat(String.format("Expected log to contain Analysis properties: \"%s\".", ANALYSIS_PROEPRTIES_STRING),
						logFileOutput, containsString(ANALYSIS_PROEPRTIES_STRING));
			}
		}
		catch (Exception e)
		{
			// Add the print of the stacktrace because the exception message is not enough to troubleshoot the root issue. For
			// example, if the exception is constructed without a message, you get no information from executing fail().
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	/**
	 * Perform a round trip test on the Code Coverage configuration builder.
	 * <p>
	 * A project is created, configured, submitted / saved, and reloaded where the original configuration is compared against
	 * the reloaded configuration for equality.
	 */
	@Test
	public void roundTripTest()
	{
		try
		{
			FreeStyleProject project = m_jenkinsRule.createFreeStyleProject("TestProject");
			CodeCoverageBuilder before = new CodeCoverageBuilder(CONNECTION_ID, CREDENTIALS_ID, ANALYSIS_PROPERTIES_FILEPATH,
					ANALYSIS_PROEPRTIES_STRING);
			project.getBuildersList().add(before);

			// workaround for eclipse compiler Ambiguous method call
			project.save();
			m_jenkinsRule.jenkins.reload();

			FreeStyleProject reloaded = m_jenkinsRule.jenkins.getItemByFullName(project.getFullName(), FreeStyleProject.class);
			assertNotNull(reloaded);

			CodeCoverageBuilder after = reloaded.getBuildersList().get(CodeCoverageBuilder.class);
			assertNotNull(after);

			m_jenkinsRule.assertEqualBeans(before, after, "connectionId,credentialsId,analysisPropertiesPath,analysisProperties");
		}
		catch (Exception e)
		{
			// Add the print of the stacktrace because the exception message is not enough to troubleshoot the root issue. For
			// example, if the exception is constructed without a message, you get no information from executing fail().
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
}