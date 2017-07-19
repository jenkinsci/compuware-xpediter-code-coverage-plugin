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
	@Rule
	public JenkinsRule j = new JenkinsRule();
	private CpwrGlobalConfiguration m_globalConfig = null;

	@Before
	public void setup()
	{
		try
		{
			JSONObject json = new JSONObject();

			JSONObject hostConnection = new JSONObject();
			hostConnection.put("description", "TestConnection");
			hostConnection.put("hostPort", "cw01:30947");
			hostConnection.put("codePage", "1047");
			hostConnection.put("connectionId", "1243");

			JSONArray hostConnections = new JSONArray();
			hostConnections.add(hostConnection);

			json.put("hostConn", hostConnections);
			json.put("topazCLILocationLinux", "/opt/Compuware/TopazCLI");
			json.put("topazCLILocationWindows", "C:\\Program Files\\Compuware\\Topaz Workbench CLI");

			m_globalConfig = CpwrGlobalConfiguration.get();
			m_globalConfig.configure(Stapler.getCurrentRequest(), json);
		}
		catch (Exception e)
		{
			fail(e.getMessage());
		}
	}

	/**
	 * Tests the construction of a builder, verifying configuration values.
	 */
	@Test
	public void constructBuilderTest()
	{
		String expectedConnectionId = "1243";
		String expectedCredentialsId = "45";
		String expectedAnalysisPropertiesPath = "/a/path/to/analysis/properties/ccanalysis.properties";
		String expectedAnalysisProperties = "cc.source=/src\ncc.repos=pfhjyg0.xv20.reposit\ncc.system=\ncc.test=\ncc.ddio.override=";

		CodeCoverageBuilder builder = new CodeCoverageBuilder(expectedConnectionId, expectedCredentialsId,
				expectedAnalysisPropertiesPath, expectedAnalysisProperties);

		assertThat(String.format("Expected CodeCoverageBuilder.getConnectionId() to return %s", expectedConnectionId),
				builder.getConnectionId(), is(equalTo(expectedConnectionId)));

		assertThat(String.format("Expected CodeCoverageBuilder.getCredentialsId() to return %s", expectedCredentialsId),
				builder.getCredentialsId(), is(equalTo(expectedCredentialsId)));

		assertThat(
				String.format("Expected CodeCoverageBuilder.getAnalysisPropertiesPath() to return %s",
						expectedAnalysisPropertiesPath),
				builder.getAnalysisPropertiesPath(), is(equalTo(expectedAnalysisPropertiesPath)));

		assertThat(
				String.format("Expected CodeCoverageBuilder.getAnalysisProperties() to return %s", expectedAnalysisProperties),
				builder.getAnalysisProperties(), is(equalTo(expectedAnalysisProperties)));
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
		String expectedConnectionId = "1243";
		String expectedCredentialsId = "5432";
		String expectedAnalysisPropertiesPath = "/a/path/to/analysis/properties/ccanalysis.properties";
		String expectedAnalysisProperties = "cc.source=/src\ncc.repos=pfhjyg0.xv20.reposit\ncc.system=\ncc.test=\ncc.ddio.override=";

		try
		{
			FreeStyleProject project = j.createFreeStyleProject("TestProject");
			project.getBuildersList().add(new CodeCoverageBuilder(expectedConnectionId, expectedCredentialsId,
					expectedAnalysisPropertiesPath, expectedAnalysisProperties));

			FreeStyleBuild build = j.buildAndAssertSuccess(project);

			// Could use JenkinsRule.java#assertLogContains(String, Run), but message on failure was odd regarding expected
			// value.
			String logFileOutput = JenkinsRule.getLog(build);

			assertThat(String.format("Expected log to contain Host connection: \"%s\".", expectedConnectionId), logFileOutput,
					containsString(expectedConnectionId));

			assertThat(String.format("Expected log to contain Login credentials: \"%s\".", expectedCredentialsId),
					logFileOutput, containsString(expectedCredentialsId));

			assertThat(
					String.format("Expected log to contain Analysis properties path: \"%s\".", expectedAnalysisPropertiesPath),
					logFileOutput, containsString(expectedAnalysisPropertiesPath));

			assertThat(String.format("Expected log to contain Analysis properties: \"%s\".", expectedAnalysisProperties),
					logFileOutput, containsString(expectedAnalysisProperties));
		}
		catch (Exception e)
		{
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
		String expectedConnectionId = "1243";
		String expectedCredentialsId = "456";
		String expectedAnalysisPropertiesPath = "/a/path/to/analysis/properties/ccanalysis.properties";
		String expectedAnalysisProperties = "cc.source=/src\ncc.repos=pfhjyg0.xv20.reposit\ncc.system=\ncc.test=\ncc.ddio.override=";

		try
		{
			FreeStyleProject project = j.createFreeStyleProject("TestProject");
			CodeCoverageBuilder before = new CodeCoverageBuilder(expectedConnectionId, expectedCredentialsId,
					expectedAnalysisPropertiesPath, expectedAnalysisProperties);
			project.getBuildersList().add(before);

			// workaround for eclipse compiler Ambiguous method call
			project.save();
			j.jenkins.reload();

			FreeStyleProject reloaded = j.jenkins.getItemByFullName(project.getFullName(), FreeStyleProject.class);
			assertNotNull(reloaded);

			CodeCoverageBuilder after = reloaded.getBuildersList().get(CodeCoverageBuilder.class);
			assertNotNull(after);

			j.assertEqualBeans(before, after, "connectionId,credentialsId,analysisPropertiesPath,analysisProperties");
		}
		catch (Exception e)
		{
			fail(e.getMessage());
		}
	}
}