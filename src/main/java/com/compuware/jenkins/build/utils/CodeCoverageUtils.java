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
package com.compuware.jenkins.build.utils;

import org.apache.commons.lang.StringUtils;
import com.compuware.jenkins.build.utils.Constants;

/**
 * 
 */
public class CodeCoverageUtils
{
	/**
	 * Returns an escaped version of the given input String for a Batch or Shell script.
	 * 
	 * @param input
	 *            the <code>String</code> to escape
	 * @param isShell
	 *            <code>true</code> if the script is a Shell script, <code>false</code> if it is a Batch script
	 * 
	 * @return the escaped <code>String</code>
	 */
	public static String escapeForScript(String input, boolean isShell)
	{
		String output = null;

		if (input != null)
		{
			// escape any double quotes (") with another double quote (") for both batch and shell scripts
			output = StringUtils.replace(input, Constants.DOUBLE_QUOTE, Constants.DOUBLE_QUOTE_ESCAPED);

			// wrap the input in quotes
			output = wrapInQuotes(output, isShell);
		}

		return output;
	}

	/**
	 * Wraps the given input String in quotes for a Batch or Shell script.
	 * 
	 * @param input
	 *            the <code>String</code> to wrap in quotes
	 * @param isShell
	 *            <code>true</code> if the script is a Shell script, <code>false</code> if it is a Batch script
	 * 
	 * @return the quoted <code>String</code>
	 */
	public static String wrapInQuotes(String input, boolean isShell)
	{
		String output = null;

		if (input != null)
		{
			// shell scripts don't need args wrapped in quotes
			if (isShell == false)
			{
				output = String.format("\"%s\"", input); //$NON-NLS-1$
			}
		}

		return output;
	}
}
