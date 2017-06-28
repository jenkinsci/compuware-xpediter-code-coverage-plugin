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
import java.util.concurrent.ExecutionException;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import com.compuware.jenkins.build.CodeCoverageBuilder.CodeCoverageDescriptorImpl;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
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
	
	private TestScanner m_testScanner = null;
	
	@Before
	public void setup()
	{
		m_testScanner = new TestScanner(null);
	}

	@Test
	public void constructBuilderTest()
	{
		String expectedHostConnection = "TestConnection";
		String expectedCredentialsId = "pfhvvv0";
		String expectedAnalysisPropertiesPath = "/a/path/to/analysis/properties/ccanalysis.properties";
		String expectedAnalysisProperties = "cc.source=/src\ncc.repos=pfhjyg0.xv20.reposit\ncc.system=\ncc.test=\ncc.ddio.override=";
		
		CodeCoverageBuilder builder = new CodeCoverageBuilder(expectedHostConnection, expectedCredentialsId, expectedAnalysisPropertiesPath, expectedAnalysisProperties);
		
		assertThat(String.format("Expected CodeCoverageBuilder.getHostConnection() to return %s", expectedHostConnection),
				builder.getHostConnection(), is(equalTo(expectedHostConnection)));
		
		assertThat(String.format("Expected CodeCoverageBuilder.getCredentialsId() to return %s", expectedCredentialsId),
				builder.getCredentialsId(), is(equalTo(expectedCredentialsId)));
		
		assertThat(String.format("Expected CodeCoverageBuilder.getAnalysisPropertiesPath() to return %s", expectedAnalysisPropertiesPath),
				builder.getAnalysisPropertiesPath(), is(equalTo(expectedAnalysisPropertiesPath)));
		
		assertThat(String.format("Expected CodeCoverageBuilder.getAnalysisProperties() to return %s", expectedAnalysisProperties),
				builder.getAnalysisProperties(), is(equalTo(expectedAnalysisProperties)));
	}

//	@Test
//	public void descriptorValuesTest()
//	{
//		CodeCoverageBuilder builder = new CodeCoverageBuilder(null, null, null);
//		CodeCoverageDescriptorImpl descriptor = (CodeCoverageDescriptorImpl) builder.getDescriptor();
//		
//		String displayName = descriptor.getDisplayName();
//		assertThat("Expected CodeCoverageBuilder.DescriptorImpl.getDisplayName() to not be null.", displayName, is(notNullValue()));
//		assertThat("Expected CodeCoverageBuilder.DescriptorImpl.getDisplayName() to not be empty.", displayName.isEmpty(), is(false));
//		
//		String defaultAnalysisPropertiesPath = descriptor.getDefaultAnalysisPropertiesPath();
//		assertThat("Expected CodeCoverageBuilder.DescriptorImpl.getDefaultAnalysisPropertiesPath() to not be null.", defaultAnalysisPropertiesPath, is(notNullValue()));
//		assertThat("Expected CodeCoverageBuilder.DescriptorImpl.getDefaultAnalysisPropertiesPath() to not be empty.", defaultAnalysisPropertiesPath.isEmpty(), is(false));
//		
//		String defaultAnalysisProperties = descriptor.getDefaultAnalysisProperties();
//		assertThat("Expected CodeCoverageBuilder.DescriptorImpl.getDefaultAnalysisProperties() to not be null.", defaultAnalysisProperties, is(notNullValue()));
//		assertThat("Expected CodeCoverageBuilder.DescriptorImpl.getDefaultAnalysisProperties() to not be empty.", defaultAnalysisProperties.isEmpty(), is(false));
//	}
	
//	@Test
//	public void executionTest()
//	{
//		String expectedHostConnection = "TestConnection";
//		String expectedAnalysisPropertiesPath = "/a/path/to/analysis/properties/ccanalysis.properties";
//		String expectedAnalysisProperties = "cc.source=/src\ncc.repos=pfhjyg0.xv20.reposit\ncc.system=\ncc.test=\ncc.ddio.override=";
//
//		String logFileOutput = null;
//		try
//		{
//			FreeStyleProject project = j.createFreeStyleProject("TestProject");
//			project.getBuildersList().add(new CodeCoverageBuilder(expectedHostConnection, expectedAnalysisPropertiesPath, expectedAnalysisProperties));
//			FreeStyleBuild build = project.scheduleBuild2(0).get();
//			logFileOutput = FileUtils.readFileToString(build.getLogFile());
//
//			assertThat(String.format("Expected log to contain %s.", expectedHostConnection), logFileOutput,
//					containsString(expectedHostConnection));
//
//			assertThat(String.format("Expected log to contain %s.", expectedAnalysisPropertiesPath), logFileOutput,
//					containsString(expectedAnalysisPropertiesPath));
//
//			assertThat(String.format("Expected log to contain %s.", expectedAnalysisProperties), logFileOutput,
//					containsString(expectedAnalysisProperties));		
//		}
//		catch (IOException | InterruptedException | ExecutionException e)
//		{
//			fail(e.getMessage());
//		}
//	}

//	@Test
//	public void roundTripTest()
//	{
//		String expectedHostConnection = "cw09.compuware.com";
//		String expectedCredentialsId = "pfhvvv0";
//		String expectedAnalysisPropertiesPath = "/a/path/to/analysis/properties/ccanalysis.properties";
//		String expectedAnalysisProperties = "cc.source=/src\ncc.repos=pfhjyg0.xv20.reposit\ncc.system=\ncc.test=\ncc.ddio.override=";
//
//		try
//		{
//			FreeStyleProject project = j.createFreeStyleProject("TestProject");
//			CodeCoverageBuilder before = new CodeCoverageBuilder(expectedHostConnection, expectedCredentialsId, expectedAnalysisPropertiesPath, expectedAnalysisProperties);
//			project.getBuildersList().add(before);
//			
//			j.configRoundtrip(project);
//			j.assertLogContains("hostConnection,credentialsId,analysisPropertiesPath,analysisProperties,...", j.buildAndAssertSuccess(project));
//			
////			HtmlPage page = j.submit(j.createWebClient().getPage(project,"configure").getFormByName("config"));
////
////			CodeCoverageBuilder after = project.getBuildersList().get(CodeCoverageBuilder.class);
////
////			j.assertEqualBeans(before,after,"hostConnection,analysisPropertiesPath,analysisProperties,...");			
//		}
//		catch (Exception e)
//		{
//			fail(e.getMessage());
//		}
//	}

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
