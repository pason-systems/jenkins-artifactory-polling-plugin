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

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.scm.ChangeLogParser;
import hudson.scm.PollingResult;
import hudson.scm.SCM;
import hudson.scm.SCMDescriptor;
import hudson.scm.SCMRevisionState;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.TaskListener;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.InterruptedException;
import java.util.ArrayList;
import java.util.logging.Logger;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.io.FileUtils;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.QueryParameter;

import static java.util.logging.Level.FINE;
import static java.util.logging.Level.WARNING;


/**
 * Class that is the main extension point for the jenkins plugin.
 * 
 * The three main functions in this class and their uses are the following:
 * 1. {@link ArtifactoryRepository#compareRemoteRevisionWith(AbstractProject, Launcher, FilePath, TaskListener, SCMRevisionState)}
 *    This function will compare the server's state with the state in the local build. If there is a difference it
 *    will trigger a build. If not it will try again when the polling period expires
 * 2. {@link ArtifactoryRepository#checkout(AbstractBuild, Launcher, FilePath, BuildListener, File)}
 *     This function will download the changed artifacts from the server, one change per build
 * 3. {@link ArtifactoryRepository#calcRevisionsFromBuild(AbstractBuild, Launcher, TaskListener)}
 *    This function will store the state of the local build so that it can be compared against the server state
 *    by the compareRemoteRevisionsWith function
 *    
 * @author ngutzmann
 *
 */
public class ArtifactoryRepository extends SCM {

	/** A logger for this class */
    private static final Logger LOGGER = Logger.getLogger(ArtifactoryRepository.class.getName());
    /** The repo that the artifact is from */
    private final String repo;
    /** The group that the artifact belongs to */
    private final String groupID;
    /** The name of the artifact */
    private final String artifactID;
    /** The version filter to use for filtering changes */
    private final String versionFilter;
    /** A dummy variable for the SCM extension */
    private final ArtifactoryBrowser browser;
    /** The directory to put the source into */
    private final String localPath;
    /** Whether or not do do the checkout */
    private final Boolean doDownload;

    /** The next artifact to download from Artifactory */
    private transient Artifact nextArtifact;

    /**
     * Constructor
     * All fields in this constructor are populated by something called a "Stapler"
     * The Stapler gets its data from the magical jelly files which are used to 
     * create the UI for jenkins
     * @param repo The repository that the artifact is found in
     * @param groupID The group that the artifact belongs to
     * @param artifactID The name of the artifact
     * @param browser The browser dummy variable
     */
    @DataBoundConstructor
    public ArtifactoryRepository(String repo, String groupID, 
        String artifactID, ArtifactoryBrowser browser, String localPath, String versionFilter, boolean doDownload)
    {
        LOGGER.log(FINE, "Configured repo with name: " + repo);
        this.repo = repo;
        this.groupID = groupID;
        this.artifactID = artifactID;
        this.browser = browser;
        this.versionFilter = versionFilter;
        this.doDownload = doDownload;
        if (localPath == null)
        {
            localPath = ".";
        }
        this.localPath = localPath;
        this.nextArtifact = null;
    }

    /**
     * See documentation at top of class.
     * This function will perform the download of the artifacts from artifactory
     */
    @Override
    public boolean checkout(AbstractBuild<?,?> build, Launcher launcher, 
        FilePath workspace, BuildListener listener, File changelogFile) 
        throws IOException, InterruptedException
    {
    	AbstractBuild<?, ?> lastBuild = build;
    	LOGGER.log(FINE, "Current job: " + build.getProject().getName() );

        // If this is the first build we need to get the next version to checkout
        if (nextArtifact == null)
        {
            ArtifactoryAPI api = new ArtifactoryAPI(getDescriptor().getArtifactoryServer());
            ArtifactoryRevisionState serverState = ArtifactoryRevisionState.fromServer(repo, groupID, artifactID, versionFilter, api);
            if (!serverState.getArtifacts().isEmpty())
            {
                nextArtifact = serverState.getArtifacts().get(0);
            }
            else
            {
                LOGGER.log(FINE, "No artifacts found for " + groupID + ":" + artifactID); 
                return false;
            }
        }

        CheckoutTask task = new CheckoutTask(getDescriptor().getArtifactoryServer(), 
                                            repo, groupID, artifactID, nextArtifact, localPath, doDownload);
        
        for (AbstractBuild<?,?> b=build; b!=null; b=b.getPreviousBuild())
        {
            if (getVersionsFile(b).exists())
            {
                build = b;
                break;
            }
        }

        File file = getVersionsFile(build);
        LOGGER.log(FINE, "Got version file for " + build.getProject().getName());
        ArtifactoryRevisionState revState = ArtifactoryRevisionState.BASE;
        
        if (file.exists())
        {
        	revState = ArtifactoryRevisionState.fromFile(file);
        
	        if ( !revState.getArtifactID().equals(artifactID) ||
	        	 !revState.getGroupID().equals(groupID) ||
	        	 !revState.getRepo().equals(repo) )
	        {
	        	revState = ArtifactoryRevisionState.BASE;
	        }
        }
        
        // Create a new Revision state with the parameters
        revState = new ArtifactoryRevisionState(repo, groupID, artifactID, revState.addOrReplaceArtifact(nextArtifact));
        LOGGER.log(FINE, revState.toJSON().toString());
        File newBuildFile = getVersionsFile(lastBuild);
        FileWriter writer = new FileWriter(newBuildFile);
        revState.toJSON().write(writer);
        writer.close();
        
        FileUtils.touch(changelogFile);
        
        return workspace.act(task);
    }

    /**
     * See documentation at top of class.
     * This function will determine the state of the local files from the 
     * previous build
     */
    @Override 
    public SCMRevisionState calcRevisionsFromBuild(AbstractBuild<?,?> build, 
        Launcher launcher, TaskListener listener) 
        throws IOException, InterruptedException
    {
        LOGGER.log(FINE, "Calculating revisions from build");
        // First find the last build that had a version file
        for (AbstractBuild<?,?> b=build; b!=null; b=b.getPreviousBuild())
        {
            if (getVersionsFile(b).exists())
            {
                build = b;
                break;
            }
        }

        File file = getVersionsFile(build);
        ArtifactoryRevisionState revState = ArtifactoryRevisionState.BASE;
        
        if (!file.exists())
        {
            // The file doesn't exist for the first build, return the base value
            LOGGER.log(WARNING, "No previous build contained a versions file");
            return revState;
        }
        else
        {
            revState = ArtifactoryRevisionState.fromFile(file);
        }

        return revState;
    }

    /**
     * See documentation top of class
     * This function will determine the state of the artifactory server and compare it with the
     * local state. If there is a condition where a build needs to be triggered, trigger a build
     */
    @Override
    public PollingResult compareRemoteRevisionWith(AbstractProject<?,?> project,
        Launcher launcher, FilePath workspace, TaskListener listener, 
        SCMRevisionState baseline) throws IOException, InterruptedException
    {
        LOGGER.log(FINE, "Comparing remote revisions with baseline");
        
        ArtifactoryRevisionState localState = (ArtifactoryRevisionState) baseline;

        ArtifactoryAPI api = new ArtifactoryAPI(getDescriptor().getArtifactoryServer());

        ArtifactoryRevisionState serverState = ArtifactoryRevisionState.fromServer(repo, groupID, artifactID, versionFilter, api);

        //Compare server state to local state
        nextArtifact = ArtifactoryRevisionState.compareRevisionStates(serverState, localState);
        if (nextArtifact != null)
        {
            LOGGER.log(FINE, "Found new data for version: " + nextArtifact);
            return PollingResult.BUILD_NOW;
        }

        // Compare local state to server state
        nextArtifact = ArtifactoryRevisionState.compareRevisionStates(localState, serverState);
        if (nextArtifact != null)
        {
            LOGGER.log(FINE, "Found new data for version: " + nextArtifact);
            return PollingResult.BUILD_NOW;
        }

        return PollingResult.NO_CHANGES;
    }

    @Override
    public ChangeLogParser createChangeLogParser()
    {
        return new ArtifactoryChangeLogParser();
    }

    @Override
    public ArtifactoryBrowser getBrowser()
    {
        return browser;
    }

    /**
     * This plugin supports polling
     * @return true
     */
    @Override
    public boolean supportsPolling()
    {
        return true;
    }

    @Override
    public boolean requiresWorkspaceForPolling()
    {
        return true;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    /**
     * Getter function for the repository
     * @return the repo that contains the artifact
     */
    public String getRepo()
    {
        return repo;
    }

    /**
     * Getter function for the groupID
     * @return the group that the artifact belongs to
     */
    public String getGroupID()
    {
        return groupID;
    }

    /**
     * Getter function for the artifactID
     * @return the name of the artifact
     */
    public String getArtifactID()
    {
        return artifactID;
    }

    /** 
     * Getter function for the local path
     * @return the local path to checkout the artifacts to
     */
    public String getLocalPath()
    {
        return localPath;
    }

    /** 
     * Getter function for the versionFilter
     * @return the version filter for the artifact
     */
    public String getVersionFilter()
    {
        return versionFilter;
    }

    /** 
    * Getter function for the doDownload variable
    * @return the if the checkout checkbox is checked
    */
    public Boolean getDoDownload()
    {
        return doDownload;
    }



    /**
     * Getter function for the file containing the local state of the build
     * @param build The build which contains the file
     * @return The file containing the state
     */
    public File getVersionsFile(AbstractBuild<?,?> build)
    {
        return new File(build.getRootDir(), "artifactoryVersions" +"-"+ repo +"-"+ groupID +"-"+ artifactID +"-"+ versionFilter + ".json" );
    }

    /**
     * The descriptor provides the data for much of what this plugin does
     * Every class that interacts with the UI must provide a descriptor
     * @author ngutzmann
     *
     */
    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends SCMDescriptor<SCM>
    {
    	/** The artifactory server. Guaranteed to end in '/' */
        private String artifactoryServer;

        /** 
         * Constructor
         */
        public DescriptorImpl()
        {
            super(ArtifactoryBrowser.class);
            load();
        }

        @Override
        public SCM newInstance(StaplerRequest req, JSONObject json) throws FormException
        {
            return (ArtifactoryRepository) super.newInstance(req, json);
        }

        @Override
        public String getDisplayName()
        {
            return "Artifactory";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException
        {
            // Any configurations from global.jelly should be saved here as fields in 
            // the DescriptorImpl class.
            artifactoryServer = json.getString("artifactoryServer");
            if (!artifactoryServer.endsWith("/"))
            {
                artifactoryServer += "/";
            }
            save();
            return super.configure(req, json);
        }

        /**
         * Setter function for the server
         * @param server The URL to the server
         */
        public void setArtifactoryServer(String server)
        {
        	artifactoryServer = server;
        	if (!artifactoryServer.endsWith("/"))
            {
                artifactoryServer += "/";
            }
            save();
        }

        /**
         * Getter function for the server
         * @return
         */
        public String getArtifactoryServer()
        {
            return artifactoryServer;
        }

        /** 
         * Function that does form validation for the artifactID field in the UI
         * @param value The value of the field
         * @return The form state depending on the value
         */
        public FormValidation doCheckArtifactID(@QueryParameter String value, @QueryParameter String groupID, @QueryParameter String repo)
        {

            if (value != "") 
            {   
                if (checkIfArtifactExists(repo, groupID, value))
                {
                    return FormValidation.ok();
                }
                else
                {
                    return FormValidation.error("Could not find artifact: " + groupID + ":" + value + " in repo " + repo);
                }
            }
            else
            {
                return FormValidation.error("Artifact ID cannot be empty");
            }

        }

        /** 
         * Function that does form validation for the groupID field in the UI
         * @param value The value of the field
         * @return The form state depending on the value
         */
        public FormValidation doCheckGroupID(@QueryParameter String value, @QueryParameter String artifactID, @QueryParameter String repo)
        {
            if (value != "") 
            {   
                if (checkIfArtifactExists(repo, value, artifactID))
                {
                    return FormValidation.ok();
                }
                else
                {
                    return FormValidation.error("Could not find artifact: " + value + ":" + artifactID + " in repo " + repo);
                }
            }
            else
            {
                return FormValidation.error("Group ID cannot be empty");
            }
        }

        public FormValidation doCheckLocalPath(@QueryParameter String value)
        {
            if (value.equals(".")) return FormValidation.warning("Defaulting to workspace");
            else                   return FormValidation.ok();
        }

        public FormValidation doCheckVersionFilter(@QueryParameter String value)
        {
            if (value != "") return FormValidation.ok();
            else             return FormValidation.error("Version Filter cannot be empty");
        }

        private boolean checkIfArtifactExists(String repository, String groupId, String artifactId)
        {
            boolean retVal = false;

            if (repository != "" && groupId != "" && artifactId != "" )
            {
                String groupURLPart = groupId.replace('.', '/');
                String url = artifactoryServer +  "api/storage/" + repository + '/' + groupURLPart + '/' + artifactId + "/";

                try {
                    LOGGER.log(FINE, "GET " + url);

                    HttpClient client = HttpClientBuilder.create().build();
                    HttpGet request = new HttpGet(url);
                    HttpResponse response = client.execute(request);

                    if (200 <= response.getStatusLine().getStatusCode()  && 300 > response.getStatusLine().getStatusCode())
                    {
                        retVal = true;
                    }
                }
                catch(IOException e)
                {
                    LOGGER.log(WARNING, "Caught IOException during GET: " + url + " " + e.getMessage() );
                }
            }

            return retVal;
        }

        
        /**
         * Function to populate the repositories drop down menu in the job configuration
         * @return All the local repositories on the artifactory server
         */
        public ListBoxModel doFillRepoItems() 
        {
        	ListBoxModel items = new ListBoxModel();
        	ArtifactoryAPI api = new ArtifactoryAPI(artifactoryServer);
        	JSONArray json = api.getRepositories();
        	
        	for (int i = 0; i < json.size(); i++)
        	{
        		JSONObject jsonRepo = json.getJSONObject(i);
        		if(jsonRepo.getString("type").equals("LOCAL"))
        		{
        			items.add(jsonRepo.getString("key"), jsonRepo.getString("key"));
        		}
        	}
        		
        	return items;
        }
    }
}