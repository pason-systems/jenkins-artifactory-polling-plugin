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

import hudson.FilePath.FileCallable;

import hudson.remoting.VirtualChannel;

import java.io.File;
import java.io.IOException;

import java.lang.InterruptedException;
import java.lang.Boolean;

import java.net.URL;

import java.util.logging.Logger;

import net.sf.json.JSONArray;

import org.apache.commons.io.FileUtils;

import static java.util.logging.Level.FINE;

/**
 * This class performs the checkout of any new versions found in artifactory
 * Given the repository, groupID, artifactID and version of an artifact it will 
 * download any files that are part of that specific artifact into the workspace
 */
public class CheckoutTask implements FileCallable<Boolean> {

	private static final long serialVersionUID = 1L;
	
	private final String repo;
    private final String groupID;
    private final String artifactID;
    private final String artifactoryURL;
    private final Artifact artifact;
    private final String localDirectory;
    private final Boolean doDownload;

    /** A logger for the task */
    private static final Logger LOGGER = Logger.getLogger(CheckoutTask.class.getName());

    /**
     * Constructor
     * @param artifactoryURL The url to artifactory. Guaranteed to end in a '/'
     * @param repo The artifactory repository to download from
     * @param groupID The groupID of the artifact
     * @param artifactID The artifactID of the artifact
     * @param version The version of the artifact
     * @param localDirectory The directory to put the files into
     * @param doDownload Whether a checkout should be done
     */
    public CheckoutTask(String artifactoryURL, String repo, String groupID, String artifactID, 
        Artifact artifact, String localDirectory, Boolean doDownload)
    {
        this.repo = repo;
        this.groupID = groupID;
        this.artifactID = artifactID;
        this.artifactoryURL = artifactoryURL;
        this.artifact = artifact;
        this.localDirectory = localDirectory;
        this.doDownload = doDownload;
    }

    /**
     * Function that does the heavy lifting of the checkout. First it gets the metadata about
     * an artifact which includes all the files that are included in that artifact. Then it 
     * proceeds to download each of those files.
     * @param workspace The file where the downloads will go to
     * @param channel The channel back to the job
     * @return True if successful, false otherwise
     * @throws IOException
     * @throws InterruptedException
     */
    public Boolean invoke(File workspace, VirtualChannel channel) throws IOException, InterruptedException
    {
        if (doDownload)
        {
            // Delete any artifacts of previous builds
            File checkoutDir = new File(workspace, localDirectory);
            checkoutDir.mkdirs();
            FileUtils.cleanDirectory(checkoutDir);   

            ArtifactoryAPI api = new ArtifactoryAPI(artifactoryURL);
            JSONArray json = api.getArtifactFiles(repo, groupID, artifactID, artifact.getVersion());

            String groupURLPart = groupID.replace('.', '/');
            for (int i = 0; i < json.size(); i++)
            {
                String uri = json.getString(i);
                LOGGER.log(FINE, "Found child URI: " + uri);
                String resourceURL = artifactoryURL + repo + '/' + groupURLPart + '/' + artifactID + '/' + artifact.getVersion() + '/' + uri;
                LOGGER.log(FINE, "Starting download of " + uri);
                FileUtils.copyURLToFile(new URL(resourceURL), new File(checkoutDir, uri));
                LOGGER.log(FINE, "Finished downloading: " + uri);
            }
        }

        return true;
    }
    
}