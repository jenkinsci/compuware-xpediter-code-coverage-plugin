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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import java.io.IOException;
import java.util.Properties;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

/**
 * CodeCoverageScanner unit tests.
 */
@SuppressWarnings("nls")
public class CodeCoverageScannerTest
{
	// Member Variables
	private CodeCoverageBuilder m_ccBuilder;
	private CodeCoverageScanner m_ccScanner;

	/**
	 * Setup that occurs before each test.
	 * 
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception
	{
		String connectionId = "12345";
		String credentialsId = "67890";
		String analysisPropertiesPath = "\\a\\path\\to\\analysis\\properties\\ccanalysis.properties";
		String analysisProperties = "cc.sources=\ncc.repos=\ncc.system=\ncc.test=\ncc.ddio.overrides=";

		m_ccBuilder = new CodeCoverageBuilder(connectionId, credentialsId, analysisPropertiesPath, analysisProperties);
		m_ccScanner = new CodeCoverageScanner(m_ccBuilder);
	}

	/**
	 * Tear down that occurs before each test.
	 * 
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception
	{
	}

	/**
	 * Test method for {@link com.compuware.jenkins.build.CodeCoverageScanner#buildAnalysisProperties(java.lang.String, java.lang.String, hudson.FilePath, java.io.PrintStream)}.
	 */
	@Test
	public void testBuildAnalysisProperties()
	{
		String propertiesFilePathStr = "ccanalysis.properties";
		String propertiesStr = "";
		JenkinsRule jenkinsRule = new JenkinsRule();
	}

	/**
	 * Test method for {@link com.compuware.jenkins.build.CodeCoverageScanner#convertStringToProperties(java.lang.String)}.
	 */
	@Test
	public void testConvertStringToProperties() throws IOException
	{
		Properties expectedPropertiesWithoutDashes = new Properties();
		expectedPropertiesWithoutDashes.put("cc.sources", "/testSrc,C:/Users/");
		expectedPropertiesWithoutDashes.setProperty("cc.repos", "XDEVREG.CC.REPOSIT,XDEVREG.CC.REPOSIT2");
		expectedPropertiesWithoutDashes.setProperty("cc.system", "ccSystem");
		expectedPropertiesWithoutDashes.setProperty("cc.test", "ccTest");
		expectedPropertiesWithoutDashes.setProperty("cc.ddio.overrides", "XDEVREG.CC.DDIO,XDEVREG.CC.DDIO2");

		Properties expectedPropertiesWithDashes = new Properties();
		expectedPropertiesWithDashes.put("-cc.sources", "/testSrc,C:/Users/");
		expectedPropertiesWithDashes.setProperty("-cc.repos", "XDEVREG.CC.REPOSIT,XDEVREG.CC.REPOSIT2");
		expectedPropertiesWithDashes.setProperty("-cc.system", "ccSystem");
		expectedPropertiesWithDashes.setProperty("-cc.test", "ccTest");
		expectedPropertiesWithDashes.setProperty("-cc.ddio.overrides", "XDEVREG.CC.DDIO,XDEVREG.CC.DDIO2");

		// test property String without dashes, without whitespace, with newlines
		String propertyStr1 = "cc.sources=/testSrc,C:/Users/\ncc.repos=XDEVREG.CC.REPOSIT,XDEVREG.CC.REPOSIT2\ncc.system=ccSystem\ncc.test=ccTest\ncc.ddio.overrides=XDEVREG.CC.DDIO,XDEVREG.CC.DDIO2";
		Properties properties1 = m_ccScanner.convertStringToProperties(propertyStr1);
		assertThat(
				"Expected property String without dashes, with newlines to be converted to Properties correctly.",
				properties1.entrySet(), is(expectedPropertiesWithoutDashes.entrySet()));

		// test property String with dashes, with newlines
		String propertyStr2 = "-cc.sources=/testSrc,C:/Users/\n-cc.repos=XDEVREG.CC.REPOSIT,XDEVREG.CC.REPOSIT2\n-cc.system=ccSystem\n-cc.test=ccTest\n-cc.ddio.overrides=XDEVREG.CC.DDIO,XDEVREG.CC.DDIO2";
		Properties properties2 = m_ccScanner.convertStringToProperties(propertyStr2);
		assertThat("Expected property String with dashes, with newlines to be converted to Properties correctly.",
				properties2.entrySet(), is(expectedPropertiesWithDashes.entrySet()));

		// test property String without dashes, with carraige returns
		String propertyStr3 = "cc.sources=/testSrc,C:/Users/\rcc.repos=XDEVREG.CC.REPOSIT,XDEVREG.CC.REPOSIT2\rcc.system=ccSystem\rcc.test=ccTest\rcc.ddio.overrides=XDEVREG.CC.DDIO,XDEVREG.CC.DDIO2";
		Properties properties3 = m_ccScanner.convertStringToProperties(propertyStr3);
		assertThat(
				"Expected property String without dashes, with carraige returns to be converted to Properties correctly.",
				properties3.entrySet(), is(expectedPropertiesWithoutDashes.entrySet()));

		// test property String with dashes, with carraige returns
		String propertyStr4 = "-cc.sources=/testSrc,C:/Users/\r-cc.repos=XDEVREG.CC.REPOSIT,XDEVREG.CC.REPOSIT2\r-cc.system=ccSystem\r-cc.test=ccTest\r-cc.ddio.overrides=XDEVREG.CC.DDIO,XDEVREG.CC.DDIO2";
		Properties properties4 = m_ccScanner.convertStringToProperties(propertyStr4);
		assertThat(
				"Expected property String with dashes, with carraige returns to be converted to Properties correctly.",
				properties4.entrySet(), is(expectedPropertiesWithDashes.entrySet()));

		// test property String without dashes, with newline/carraige returns
		String propertyStr5 = "cc.sources=/testSrc,C:/Users/\n\rcc.repos=XDEVREG.CC.REPOSIT,XDEVREG.CC.REPOSIT2\n\rcc.system=ccSystem\n\rcc.test=ccTest\n\rcc.ddio.overrides=XDEVREG.CC.DDIO,XDEVREG.CC.DDIO2";
		Properties properties5 = m_ccScanner.convertStringToProperties(propertyStr5);
		assertThat(
				"Expected property String without dashes, with newline/carraige returns to be converted to Properties correctly.",
				properties5.entrySet(), is(expectedPropertiesWithoutDashes.entrySet()));

		// test property String with dashes, with newline/carraige returns
		String propertyStr6 = "-cc.sources=/testSrc,C:/Users/\n\r-cc.repos=XDEVREG.CC.REPOSIT,XDEVREG.CC.REPOSIT2\n\r-cc.system=ccSystem\n\r-cc.test=ccTest\n\r-cc.ddio.overrides=XDEVREG.CC.DDIO,XDEVREG.CC.DDIO2";
		Properties properties6 = m_ccScanner.convertStringToProperties(propertyStr6);
		assertThat(
				"Expected property String with dashes, with newline/carraige returns to be converted to Properties correctly.",
				properties6.entrySet(), is(expectedPropertiesWithDashes.entrySet()));
	}

	/**
	 * Test method for {@link com.compuware.jenkins.build.CodeCoverageScanner#prefixWithDash(java.lang.String)}.
	 */
	@Test
	public void testPrefixWithDash()
	{
		String strWithDash = "-hasDash";
		String strWithoutDash = "hasDash";

		String strWithDashAfterCall = m_ccScanner.prefixWithDash(strWithDash);
		assertThat("Expected the string with a dash to not be changed after call.", strWithDashAfterCall, equalTo(strWithDash));

		String strWithoutDashAfterCall = m_ccScanner.prefixWithDash(strWithoutDash);
		assertThat("Expected the string without a dash to be changed after call.", strWithoutDashAfterCall,
				equalTo(strWithDash));
	}
}