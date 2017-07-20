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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import hudson.FilePath;

/**
 * CodeCoverageScanner unit tests.
 */
@SuppressWarnings("nls")
public class CodeCoverageScannerTest
{
	// Constants
	private static final String PROPERTIES_FILENAME = "ccanalysis.properties";

	// Member Variables
	private static CodeCoverageBuilder m_ccBuilder;
	private static CodeCoverageScanner m_ccScanner;

	@Rule
	public JenkinsRule m_jenkinsRule = new JenkinsRule();

	/**
	 * Setup that occurs before all tests.
	 * 
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void classSetUp() throws Exception
	{
		String connectionId = "12345";
		String credentialsId = "67890";
		String analysisPropertiesPath = "\\a\\path\\to\\analysis\\properties\\ccanalysis.properties";
		String analysisProperties = "cc.sources=\ncc.repos=\ncc.system=\ncc.test=\ncc.ddio.overrides=";

		m_ccBuilder = new CodeCoverageBuilder(connectionId, credentialsId, analysisPropertiesPath, analysisProperties);
		m_ccScanner = new CodeCoverageScanner(m_ccBuilder);
	}

	/**
	 * Test method for {@link com.compuware.jenkins.build.CodeCoverageScanner#buildAnalysisProperties(java.lang.String, java.lang.String, hudson.FilePath, java.io.PrintStream)}.
	 */
	@Test
	public void testBuildAnalysisProperties() throws IOException
	{
		String propertiesStr = null;
		FilePath workspace = m_jenkinsRule.getInstance().getRootPath();
		PrintStream logger = new PrintStream(System.out);
		Properties expectedProperties = null;
		Properties actualProperties = null;

		// create the analysis properties file
		Path analysisPropertiesFilePath = Paths.get(workspace.getRemote(), PROPERTIES_FILENAME);
		List<String> analysisPropertiesFileContent = Arrays.asList("cc.sources=/fileSrc", "cc.repos=FILE.CC.REPOSIT",
				"cc.system=fileSystem", "cc.test=fileTest", "cc.ddio.overrides=FILE.CC.DDIO");
		Files.write(analysisPropertiesFilePath, analysisPropertiesFileContent, Charset.forName("UTF-8"),
				StandardOpenOption.CREATE_NEW);

		try
		{
			// test with properties only in the file
			expectedProperties = new Properties();
			expectedProperties.put("cc.sources", "/fileSrc");
			expectedProperties.setProperty("cc.repos", "FILE.CC.REPOSIT");
			expectedProperties.setProperty("cc.system", "fileSystem");
			expectedProperties.setProperty("cc.test", "fileTest");
			expectedProperties.setProperty("cc.ddio.overrides", "FILE.CC.DDIO");

			actualProperties = m_ccScanner.buildAnalysisProperties(PROPERTIES_FILENAME, propertiesStr, workspace, logger);
			assertThat("Expected properties only in a file to match properties returned.", actualProperties.entrySet(),
					is(expectedProperties.entrySet()));

			// test with properties only in the UI
			propertiesStr = "cc.sources=/uiSrc\ncc.repos=UI.CC.REPOSIT\ncc.system=uiSystem\ncc.test=uiTest\ncc.ddio.overrides=UI.CC.DDIO";

			expectedProperties = new Properties();
			expectedProperties.put("cc.sources", "/uiSrc");
			expectedProperties.setProperty("cc.repos", "UI.CC.REPOSIT");
			expectedProperties.setProperty("cc.system", "uiSystem");
			expectedProperties.setProperty("cc.test", "uiTest");
			expectedProperties.setProperty("cc.ddio.overrides", "UI.CC.DDIO");

			actualProperties = m_ccScanner.buildAnalysisProperties(PROPERTIES_FILENAME, propertiesStr, workspace, logger);
			assertThat("Expected properties only in the UI to match properties returned.", actualProperties.entrySet(),
					is(expectedProperties.entrySet()));

			// test with properties in both (UI should override file)
			propertiesStr = "cc.sources=/uiSrc\ncc.system=uiSystem\ncc.ddio.overrides=UI.CC.DDIO";

			expectedProperties = new Properties();
			expectedProperties.put("cc.sources", "/uiSrc");
			expectedProperties.setProperty("cc.repos", "FILE.CC.REPOSIT");
			expectedProperties.setProperty("cc.system", "uiSystem");
			expectedProperties.setProperty("cc.test", "fileTest");
			expectedProperties.setProperty("cc.ddio.overrides", "UI.CC.DDIO");

			actualProperties = m_ccScanner.buildAnalysisProperties(PROPERTIES_FILENAME, propertiesStr, workspace, logger);
			assertThat("Expected properties in both a file and the UI to match properties returned.",
					actualProperties.entrySet(), is(expectedProperties.entrySet()));
		}
		finally
		{
			// delete the analysis properties file
			Files.deleteIfExists(analysisPropertiesFilePath);
		}
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