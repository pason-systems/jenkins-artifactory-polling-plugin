// Copyright (c) 2014 Pason Systems, Inc.

// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:

// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.

// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

package com.pason.plugins.artifactorypolling;

import hudson.model.Descriptor;

import hudson.scm.ChangeLogSet;
import hudson.scm.RepositoryBrowser;

import hudson.Extension;

import java.io.IOException;

import java.net.URL;

import org.kohsuke.stapler.DataBoundConstructor;


/**
 * Class that must be overriden to create an SCM implementation
 * @author ngutzmann
 *
 */
public class ArtifactoryBrowser extends RepositoryBrowser<ChangeLogSet.Entry>
{
	private static final long serialVersionUID = 1L;
	
	/**
	 * Constructor
	 */
	@DataBoundConstructor
    public ArtifactoryBrowser()
    {}

	/**
	 * Descriptor internal class for the Browser
	 * For user with the UI
	 * @author ngutzmann
	 *
	 */
    @Extension
    public static class DescriptorImpl extends Descriptor<RepositoryBrowser<?>>
    {
        public String getDisplayName() 
        {
            return "Artifactory API Browser";
        }
    }

    /**
     * Function to return a link to the changeset
     */
    @Override
    public URL getChangeSetLink(ChangeLogSet.Entry changeSet) throws IOException
    {
        //Determines the link to the give changeset
        return null;
    }

}