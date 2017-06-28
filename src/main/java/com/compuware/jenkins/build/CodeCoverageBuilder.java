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

import static java.util.Arrays.asList;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import javax.servlet.ServletException;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.ListBoxModel.Option;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;

/**
 * Captures the configuration information for a Code Coverage build step.
 */
public class CodeCoverageBuilder extends Builder implements SimpleBuildStep
{
	// Member Variables
	private final String m_hostConnection;
	private final String m_credentialsId;
	private final String m_analysisPropertiesPath;
	private final String m_analysisProperties;

	/**
	 * Constructor.
	 * 
	 * @param hostConnection a host connection
	 * @param credentialsId unique id of the selected credential
	 * @param analysisPropertiesPath the path of Code Coverage analysis properties file
	 * @param analysisProperties the Code Coverage analysis properties
	 */
	@DataBoundConstructor
	public CodeCoverageBuilder(String hostConnection, String credentialsId, String analysisPropertiesPath, String analysisProperties)
	{
		m_hostConnection = StringUtils.trimToEmpty(hostConnection);
		m_credentialsId = StringUtils.trimToEmpty(credentialsId);
		m_analysisPropertiesPath = StringUtils.trimToEmpty(analysisPropertiesPath);
		m_analysisProperties = StringUtils.trimToEmpty(analysisProperties);
	}

	/**
	 * Gets the value of the 'Host connection'.
	 * 
	 * @return <code>String</code> value of m_hostConnection
	 */
	public String getHostConnection()
	{
		return m_hostConnection;
	}

	/**
	 * Gets the value of the 'Login credentials'.
	 * 
	 * @return <code>String</code> value of m_credentialsId
	 */
	public String getCredentialsId()
	{
		return m_credentialsId;
	}

	/**
	 * Gets the value of the 'Path to analysis properties'.
	 * 
	 * @return <code>String</code> value of m_analysisPropertiesPath
	 */
	public String getAnalysisPropertiesPath()
	{
		return m_analysisPropertiesPath;
	}

	/**
	 * Gets the value of the 'Analysis properties'.
	 * 
	 * @return <code>String</code> value of m_analysisProperties
	 */
	public String getAnalysisProperties()
	{
		return m_analysisProperties;
	}
    
    /*
	 * (non-Javadoc)
	 * @see hudson.tasks.Builder#getDescriptor()
	 */
    @Override
    public CodeCoverageDescriptorImpl getDescriptor() 
    {
        return (CodeCoverageDescriptorImpl)super.getDescriptor();
    }

	/**
	 * DescriptorImpl is used to create instances of <code>CodeCoverageBuilder</code>. It also contains the global configuration
	 * options as fields, just like the <code>CodeCoverageBuilder</code> contains the configuration options for a job
	 */
	@Extension
	public static final class CodeCoverageDescriptorImpl extends BuildStepDescriptor<Builder>
	{
		/**
		 * In order to load the persisted global configuration, you have to call load() in the constructor.
		 */
		public CodeCoverageDescriptorImpl()
		{
			super(CodeCoverageBuilder.class);
			load();
		}

		/*
		 * (non-Javadoc)
		 * @see hudson.tasks.BuildStepDescriptor#isApplicable(java.lang.Class)
		 */
		@SuppressWarnings("rawtypes")
		public boolean isApplicable(Class<? extends AbstractProject> aClass)
		{
			// Indicates that this builder can be used with all kinds of project types
			return true;
		}

		/*
		 * (non-Javadoc)
		 * @see hudson.model.Descriptor#getDisplayName()
		 */
		public String getDisplayName()
		{
			return Messages.descriptorDisplayName();
		}

		/*
		 * (non-Javadoc)
		 * @see hudson.model.Descriptor#configure(org.kohsuke.stapler.StaplerRequest, net.sf.json.JSONObject)
		 */
		@Override
		public boolean configure(StaplerRequest req, JSONObject formData) throws FormException
		{
			save();
			return super.configure(req, formData);
		}

		/**
		 * Get the default value for 'Analysis properties path'
		 * 
		 * @return the default value for 'Analysis properties path'
		 */
		public String getDefaultAnalysisPropertiesPath()
		{
			return Messages.defaultAnalysisPropertiesPath();
		}

		/**
		 * Get the default value for 'Analysis properties'
		 * 
		 * @return the default value for 'Analysis properties'
		 */
		public String getDefaultAnalysisProperties()
		{
			return Messages.defaultAnalysisProperties();
		}

		/**
		 * Validator for the 'Host connection' field.
		 * 
		 * @param hostConnection
		 *            host connection passed from the config.jelly "hostConnection" field
		 * 
		 * @return validation message
		 * 
		 * @throws IOException
		 * @throws ServletException
		 */
		public FormValidation doCheckHostConnection(@QueryParameter String hostConnection) throws IOException, ServletException
		{
			String tempValue = StringUtils.trimToEmpty(hostConnection);
			if (tempValue.isEmpty() == true)
			{
				return FormValidation.error(Messages.checkHostConnectionError());
			}

			return FormValidation.ok();
		}

		/**
		 * Validator for the 'Login credentials' field.
		 * 
		 * @param credentialsId
		 *            login credentials passed from the config.jelly "credentialsId" field
		 * 
		 * @return validation message
		 * 
		 * @throws IOException
		 * @throws ServletException
		 */
		public FormValidation doCheckCredentialsId(@QueryParameter String credentialsId) throws IOException, ServletException
		{
			String tempValue = StringUtils.trimToEmpty(credentialsId);
			if (tempValue.isEmpty() == true)
			{
				return FormValidation.error(Messages.checkLoginCredentialsError());
			}

			return FormValidation.ok();
		}
		
		/**
		 * Fills in the Host Connection selection box with applicable connections.
		 * 
		 * @param context
		 *            filter for host connections
		 * @param hostConnection
		 *            an existing host connection; can be null
		 * 
		 * @return host connection selections
		 * 
		 * @throws IOException
		 * @throws ServletException
		 */
		public ListBoxModel doFillHostConnectionItems(@AncestorInPath Jenkins context, @QueryParameter String hostConnection,
				@AncestorInPath Item project) throws IOException, ServletException
		{
			// TODO (pfhjyg0) : Fill out items when 'common' plugin has been created; for now use dummy data and keep example
			// code commented out
			// List<StandardUsernamePasswordCredentials> creds = CredentialsProvider.lookupCredentials(
			// StandardUsernamePasswordCredentials.class, project, ACL.SYSTEM,
			// Collections.<DomainRequirement> emptyList());

			ListBoxModel model = new ListBoxModel();
			model.add(new Option(StringUtils.EMPTY, StringUtils.EMPTY, false));

			for (String s : asList("cw01.compuware.com", "cw09.compuware.com")) //$NON-NLS-1$ //$NON-NLS-2$
			{
				boolean isSelected = false;
				if (hostConnection != null)
				{
					isSelected = hostConnection.matches(s);
				}

				model.add(new Option(s, s, isSelected));
			}

			// TODO (pfhjyg0) : Fill out items when 'common' plugin has been created; for now use dummy data and keep example
			// code commented out
			// for (StandardUsernamePasswordCredentials c : creds)
			// {
			// boolean isSelected = false;
			// if (hostConnection != null)
			// {
			// isSelected = hostConnection.matches(c.getId());
			// }
			//
			// String description = Util.fixEmptyAndTrim(c.getDescription());
			// model.add(new Option(c.getUsername() + (description != null ? " (" + description + ')' : StringUtils.EMPTY),
			// //$NON-NLS-1$
			// c.getId(), isSelected));
			// }

			return model;
		}

		/**
		 * Fills in the Login Credentials selection box with applicable connections.
		 * 
		 * @param context
		 *            filter for login credentials
		 * @param credentialsId
		 *            existing login credentials; can be null
		 * 
		 * @return login credentials selection
		 * 
		 * @throws IOException
		 * @throws ServletException
		 */
		public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Jenkins context, @QueryParameter String credentialsId,
				@AncestorInPath Item project) throws IOException, ServletException
		{
			List<StandardUsernamePasswordCredentials> creds = CredentialsProvider.lookupCredentials(
					StandardUsernamePasswordCredentials.class, project, ACL.SYSTEM,
					Collections.<DomainRequirement> emptyList());

			ListBoxModel model = new ListBoxModel();
			model.add(new Option(StringUtils.EMPTY, StringUtils.EMPTY, false));

			for (StandardUsernamePasswordCredentials c : creds)
			{
				boolean isSelected = false;
				if (credentialsId != null)
				{
					isSelected = credentialsId.matches(c.getId());
				}

				String description = Util.fixEmptyAndTrim(c.getDescription());
				model.add(new Option(c.getUsername() + (description != null ? " (" + description + ')' : StringUtils.EMPTY),
						// $NON-NLS-1$
						c.getId(), isSelected));
			}

			return model;
		}
	}

	/* (non-Javadoc)
	 * @see jenkins.tasks.SimpleBuildStep#perform(hudson.model.Run, hudson.FilePath, hudson.Launcher, hudson.model.TaskListener)
	 */
	@Override
	public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener)
			throws InterruptedException, IOException
	{
		CodeCoverageScanner scanner = new CodeCoverageScanner(this);
		scanner.perform(run, workspace, launcher, listener);
	}
}