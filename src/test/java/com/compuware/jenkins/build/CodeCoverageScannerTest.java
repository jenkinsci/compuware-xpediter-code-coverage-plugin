/**
 * The MIT License (MIT)
 * 
 * Copyright (c) 2018 Compuware Corporation
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
}