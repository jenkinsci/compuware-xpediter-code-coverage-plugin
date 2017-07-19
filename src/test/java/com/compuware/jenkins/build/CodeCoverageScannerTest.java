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
import static org.junit.Assert.assertThat;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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
		String expectedConnectionId = "1243";
		String expectedCredentialsId = "45";
		String expectedAnalysisPropertiesPath = "/a/path/to/analysis/properties/ccanalysis.properties";
		String expectedAnalysisProperties = "cc.source=/src\ncc.repos=pfhjyg0.xv20.reposit\ncc.system=\ncc.test=\ncc.ddio.override=";

		m_ccBuilder = new CodeCoverageBuilder(expectedConnectionId, expectedCredentialsId, expectedAnalysisPropertiesPath,
				expectedAnalysisProperties);
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
		// fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.compuware.jenkins.build.CodeCoverageScanner#convertStringToProperties(java.lang.String)}.
	 */
	@Test
	public void testConvertStringToProperties()
	{
		// fail("Not yet implemented");
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