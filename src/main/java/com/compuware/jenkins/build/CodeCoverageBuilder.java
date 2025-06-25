/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2018 Compuware Corporation
 * (c) Copyright 2015 - 2019, 2021 BMC Software, Inc.
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

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest2;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.compuware.jenkins.common.configuration.CpwrGlobalConfiguration;
import com.compuware.jenkins.common.configuration.HostConnection;

import hudson.AbortException;
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
	private static final Logger LOGGER = Logger.getLogger("hudson.CodeCoverageBuilder"); //$NON-NLS-1$

	// Member Variables
	private final String m_connectionId;
	private final String m_credentialsId;
	private final String m_analysisPropertiesPath;
	private final String m_analysisProperties;

	/**
	 * Constructor.
	 *
	 * @param connectionId
				  a unique host connection identifier
	 * @param credentialsId
	 *            unique id of the selected credential
	 * @param analysisPropertiesPath
	 *            the path of Code Coverage analysis properties file
	 * @param analysisProperties
	 *            the Code Coverage analysis properties
	 */
	@DataBoundConstructor
	public CodeCoverageBuilder(String connectionId, String credentialsId, String analysisPropertiesPath,
			String analysisProperties)
	{
		m_connectionId = StringUtils.trimToEmpty(connectionId);
		m_credentialsId = StringUtils.trimToEmpty(credentialsId);
		m_analysisPropertiesPath = StringUtils.trimToEmpty(analysisPropertiesPath);
		m_analysisProperties = StringUtils.trimToEmpty(analysisProperties);
	}

	/**
	 * Gets the unique identifier of the 'Host connection'.
	 *
	 * @return <code>String</code> value of m_connectionId
	 */
	public String getConnectionId()
	{
		return m_connectionId;
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
		return (CodeCoverageDescriptorImpl) super.getDescriptor();
    }

	/**
	 * DescriptorImpl is used to create instances of <code>CodeCoverageBuilder</code>. It also contains the global configuration
	 * options as fields, just like the <code>CodeCoverageBuilder</code> contains the configuration options for a job
	 */
	@Extension
	public static final class CodeCoverageDescriptorImpl extends BuildStepDescriptor<Builder>
	{
		/**
		 * Constructor.
		 * <p>
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
		@Override
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
		@NonNull
		@Override
		public String getDisplayName()
		{
			return Messages.descriptorDisplayName();
		}

		/*
		 * (non-Javadoc)
		 * @see hudson.model.Descriptor#configure(org.kohsuke.stapler.StaplerRequest2, net.sf.json.JSONObject)
		 */
		@Override
		public boolean configure(StaplerRequest2 req, JSONObject formData) throws FormException
		{
			save();
			return super.configure(req, formData);
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
		 * @param connectionId
		 *            unique identifier for the host connection passed from the config.jelly "connectionId" field
		 *
		 * @return validation message
		 */
		public FormValidation doCheckConnectionId(@QueryParameter String connectionId)
		{
			String tempValue = StringUtils.trimToEmpty(connectionId);
			if (tempValue.isEmpty())
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
		 */
		public FormValidation doCheckCredentialsId(@QueryParameter String credentialsId)
		{
			String tempValue = StringUtils.trimToEmpty(credentialsId);
			if (tempValue.isEmpty())
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
		 * @param connectionId
		 *            an existing host connection identifier; can be null
		 * @param project
		 *            the Jenkins project
		 *
		 * @return host connection selections
		 */
		public ListBoxModel doFillConnectionIdItems(@AncestorInPath Jenkins context, @QueryParameter String connectionId,
				@AncestorInPath Item project)
		{
			if (project == null) {
				Jenkins.get().checkPermission(Jenkins.ADMINISTER);
			} else {
				project.checkPermission(Item.CONFIGURE);
			}

			CpwrGlobalConfiguration globalConfig = CpwrGlobalConfiguration.get();
			HostConnection[] hostConnections = globalConfig.getHostConnections();

			ListBoxModel model = new ListBoxModel();
			model.add(new Option(StringUtils.EMPTY, StringUtils.EMPTY, false));

			for (HostConnection connection : hostConnections)
			{
				boolean isSelected = false;
				if (connectionId != null)
				{
					isSelected = connectionId.matches(connection.getConnectionId());
				}

				model.add(new Option(connection.getDescription() + " [" + connection.getHostPort() + ']', //$NON-NLS-1$
						connection.getConnectionId(), isSelected));
			}

			return model;
		}

		/**
		 * Fills in the Login Credentials selection box with applicable connections.
		 *
		 * @param context
		 *            filter for login credentials
		 * @param credentialsId
		 *            existing login credentials; can be null
		 * @param project
		 *            the Jenkins project
		 *
		 * @return login credentials selection
		 */
		public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Jenkins context, @QueryParameter String credentialsId,
				@AncestorInPath Item project)
		{
			if (project == null) {
				Jenkins.get().checkPermission(Jenkins.ADMINISTER);
			} else {
				project.checkPermission(Item.CONFIGURE);
			}

			List<StandardCredentials> creds = CredentialsProvider.lookupCredentialsInItem(
					StandardCredentials.class, project, ACL.SYSTEM2,
					Collections.emptyList());

			ListBoxModel model = new ListBoxModel();
			model.add(new Option(StringUtils.EMPTY, StringUtils.EMPTY, false));

			for (StandardCredentials c : creds)
			{
				boolean isSelected = false;
				if (credentialsId != null)
				{
					isSelected = credentialsId.matches(c.getId());
				}

				String description = Util.fixEmptyAndTrim(c.getDescription());
				try {
					model.add(new Option(CpwrGlobalConfiguration.get().getCredentialsUser(c)
							+ (description != null ? (" (" + description + ')') : StringUtils.EMPTY), c.getId(), isSelected)); //$NON-NLS-1$
				} catch (AbortException e) {
					LOGGER.log(Level.WARNING, e.getMessage());
				}
			}

			return model;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see jenkins.tasks.SimpleBuildStep#perform(hudson.model.Run, hudson.FilePath, hudson.Launcher, hudson.model.TaskListener)
	 */
	@Override
	public void perform(@NonNull Run<?, ?> run, @NonNull FilePath workspace, @NonNull Launcher launcher, @NonNull TaskListener listener)
			throws InterruptedException, IOException
	{
		CodeCoverageScanner scanner = new CodeCoverageScanner(this);
		scanner.perform(run, workspace, launcher, listener);
	}
}