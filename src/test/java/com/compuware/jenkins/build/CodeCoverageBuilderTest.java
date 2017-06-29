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
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import java.io.IOException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import com.compuware.jenkins.build.CodeCoverageBuilder.CodeCoverageDescriptorImpl;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Run;
import hudson.model.TaskListener;

/**
 * Code Coverage Builder tests.
 */
@SuppressWarnings("nls")
public class CodeCoverageBuilderTest
{
	@Rule
	public JenkinsRule j = new JenkinsRule();

	// TODO (pfhjyg0) : to be handled later, when actually performing Code Coverage.
	private TestScanner m_testScanner = null;

	@Before
	public void setup()
	{
		// TODO (pfhjyg0) : to be handled later, when actually performing Code Coverage.
		m_testScanner = new TestScanner(null);
	}

	/**
	 * Tests the construction of a builder, verifying configuration values.
	 */
	@Test
	public void constructBuilderTest()
	{
		String expectedHostConnection = "TestConnection";
		String expectedCredentialsId = "pfhvvv0";
		String expectedAnalysisPropertiesPath = "/a/path/to/analysis/properties/ccanalysis.properties";
		String expectedAnalysisProperties = "cc.source=/src\ncc.repos=pfhjyg0.xv20.reposit\ncc.system=\ncc.test=\ncc.ddio.override=";

		CodeCoverageBuilder builder = new CodeCoverageBuilder(expectedHostConnection, expectedCredentialsId,
				expectedAnalysisPropertiesPath, expectedAnalysisProperties);

		assertThat(String.format("Expected CodeCoverageBuilder.getHostConnection() to return %s", expectedHostConnection),
				builder.getHostConnection(), is(equalTo(expectedHostConnection)));

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
		CodeCoverageBuilder builder = new CodeCoverageBuilder(null, null, null, null);
		CodeCoverageDescriptorImpl descriptor = (CodeCoverageDescriptorImpl) builder.getDescriptor();

		String displayName = descriptor.getDisplayName();
		assertThat("Expected CodeCoverageBuilder.DescriptorImpl.getDisplayName() to not be null.", displayName,
				is(notNullValue()));
		assertThat("Expected CodeCoverageBuilder.DescriptorImpl.getDisplayName() to not be empty.", displayName.isEmpty(),
				is(false));

		String defaultAnalysisPropertiesPath = descriptor.getDefaultAnalysisPropertiesPath();
		assertThat("Expected CodeCoverageBuilder.DescriptorImpl.getDefaultAnalysisPropertiesPath() to not be null.",
				defaultAnalysisPropertiesPath, is(notNullValue()));
		assertThat("Expected CodeCoverageBuilder.DescriptorImpl.getDefaultAnalysisPropertiesPath() to not be empty.",
				defaultAnalysisPropertiesPath.isEmpty(), is(false));

		String defaultAnalysisProperties = descriptor.getDefaultAnalysisProperties();
		assertThat("Expected CodeCoverageBuilder.DescriptorImpl.getDefaultAnalysisProperties() to not be null.",
				defaultAnalysisProperties, is(notNullValue()));
		assertThat("Expected CodeCoverageBuilder.DescriptorImpl.getDefaultAnalysisProperties() to not be empty.",
				defaultAnalysisProperties.isEmpty(), is(false));
	}

	/**
	 * Tests the results of an execution.
	 * 
	 * <p>
	 * A project is created, configured and executed where the log is examined to verify results.
	 */
	@Test
	public void executionTest()
	{
		String expectedHostConnection = "TestConnection";
		String expectedCredentialsId = "pfhvvv0";
		String expectedAnalysisPropertiesPath = "/a/path/to/analysis/properties/ccanalysis.properties";
		String expectedAnalysisProperties = "cc.source=/src\ncc.repos=pfhjyg0.xv20.reposit\ncc.system=\ncc.test=\ncc.ddio.override=";

		try
		{
			FreeStyleProject project = j.createFreeStyleProject("TestProject");
			project.getBuildersList().add(new CodeCoverageBuilder(expectedHostConnection, expectedCredentialsId,
					expectedAnalysisPropertiesPath, expectedAnalysisProperties));

			FreeStyleBuild build = j.buildAndAssertSuccess(project);

			// Could use JenkinsRule.java#assertLogContains(String, Run), but message on failure was odd regarding expected value.

			String logFileOutput = JenkinsRule.getLog(build);

			assertThat(String.format("Expected log to contain Host connection: \"%s\".", expectedHostConnection), logFileOutput,
					containsString(expectedHostConnection));

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
	 * 
	 * <p>
	 * A project is created, configured, submitted / saved, and reloaded where the original configuration is compared against
	 * the reloaded configuration for equality.
	 */
	@Test
	public void roundTripTest()
	{
		String expectedHostConnection = "cw09.compuware.com";
		String expectedCredentialsId = "pfhvvv0";
		String expectedAnalysisPropertiesPath = "/a/path/to/analysis/properties/ccanalysis.properties";
		String expectedAnalysisProperties = "cc.source=/src\ncc.repos=pfhjyg0.xv20.reposit\ncc.system=\ncc.test=\ncc.ddio.override=";

		try
		{
			FreeStyleProject project = j.createFreeStyleProject("TestProject");
			CodeCoverageBuilder before = new CodeCoverageBuilder(expectedHostConnection, expectedCredentialsId,
					expectedAnalysisPropertiesPath, expectedAnalysisProperties);
			project.getBuildersList().add(before);
			j.assertEqualBeans(before, j.configRoundtrip(before), "hostConnection,analysisPropertiesPath,analysisProperties");
		}
		catch (Exception e)
		{
			fail(e.getMessage());
		}
	}

	// TODO (pfhjyg0) : to be handled later, when actually performing Code Coverage.
	private class TestScanner extends CodeCoverageScanner
	{
		/**
		 * @param config
		 */
		public TestScanner(CodeCoverageBuilder config)
		{
			super(config);
		}

		/*
		 * (non-Javadoc)
		 * @see com.compuware.jenkins.build.CodeCoverageScanner#perform(hudson.model.Run, hudson.FilePath, hudson.Launcher, hudson.model.TaskListener)
		 */
		@Override
		public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener)
				throws IOException, InterruptedException
		{
			super.perform(run, workspace, launcher, listener);
		}
	};
}
