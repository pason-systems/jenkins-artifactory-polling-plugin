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

import hudson.scm.SCMRevisionState;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

import net.sf.json.JSONObject;
import net.sf.json.JSONArray;
import net.sf.json.groovy.JsonSlurper;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.WARNING;

/**
 * Class that represents the state of an artifact either locally or on the server
 * This class contains all the data necessary to determine this state as well as 
 * some helper methods and is immutable
 * @author ngutzmann
 *
 */
public final class ArtifactoryRevisionState extends SCMRevisionState{

	/** JSON serialization/deserialization tags */
    public static final String ARTIFACT_ID_TAG = "artifactID";
    public static final String GROUP_TAG = "groupID";
    public static final String REPO_TAG = "repo";
    public static final String ARTIFACTS_TAG = "artifacts";

    /** A logger for the task */
    private static final Logger LOGGER = Logger.getLogger(ArtifactoryRevisionState.class.getName());
    
    /**
     * A dummy state so that we don't have to pass null objects around
     */
    public static final ArtifactoryRevisionState BASE = new ArtifactoryRevisionState();

    /** The repo that contains the artifact */
    private String repo;
    
    /** The group that the artifact belongs to */
    private String groupID;
    
    /** The name of the artifact */
    private String artifactID;
    
    /** The individual artifacts and their versions that have been published */
    private ArrayList<Artifact> artifacts;
    
    /** 
     * Private constructor for the {@link ArtifactoryRevisionState#BASE} object
     */
    private ArtifactoryRevisionState()
    {  
        artifacts = new ArrayList<Artifact>();
        this.repo = "";
        this.groupID = "";
        this.artifactID = "";
    }

    /**
     * Constructor
     * @param repo The repository that contains the artifact
     * @param groupID The group that the artifact belongs to 
     * @param artifactID The name of the artifact
     * @param artifacts Individual artifacts and their versions that have been published
     */
    public ArtifactoryRevisionState(String repo, String groupID, String artifactID, ArrayList<Artifact> artifacts)
    {
        this.repo = repo;
        this.groupID = groupID;
        this.artifactID = artifactID;
        this.artifacts = new ArrayList<Artifact>(artifacts);
    }

    /**
     * Getter function
     * @return The repository that contains the artifact
     */
    public String getRepo()
    {
        return repo;
    }

    /**
     * Getter function
     * @return The group that the artifact belongs to
     */
    public String getGroupID()
    {
        return groupID;
    }

    /**
     * Getter function 
     * @return The name of the artifact
     */
    public String getArtifactID()
    {
        return artifactID;
    }
    
    /**
     * Getter function 
     * @return The individual published artifacts
     */
    public ArrayList<Artifact> getArtifacts()
    {
        return new ArrayList<Artifact>(artifacts);
    }

    /**
     * Function to search for a specific version of an artifact
     * @param version The version to search for
     * @return The artifact that contains that version or null if it doesn't exist
     */
    public Artifact getVersion(String version)
    {
        Artifact retVal = null;

        for (Artifact artifact : artifacts)
        {
            if (version.equals(artifact.getVersion()))
            {
                retVal = artifact;
                break;
            }
        }

        return retVal;
    }
    
    /**
     * Function to add or replace an artifact in the revision state object
     * @param artifact The artifact to add or replace
     */
    public ArrayList<Artifact> addOrReplaceArtifact(Artifact artifact)
    {
    	ArrayList<Artifact> artifacts = getArtifacts();
        Artifact oldArtifact = getVersion(artifact.getVersion());
        
        // Check if the version already exists
        if (oldArtifact != null)
        {
            artifacts.remove(oldArtifact);
        }
        
        artifacts.add(artifact);
        
        return artifacts;
    }

    /**
     * Function to parse the local file which contains the revision state
     * {
     *   "artifactID": "aID",
     *   "groupID": "gID",
     *   "repo": "repo",
     *   "artifacts": [
     *     {
     *       "version": "V1",
     *       "files": [
     *         {
     *           "filename": "fileName",
     *           "metadata": {
     *             "size": "fileSize",
     *             "checksums": {
     *               "sha1": "asdfasdfasdfasdfasdfa",
     *               "md5": "fgjkfghkfghkfghkfghkfgh"
     *                }
     *             }
     *           }
     *        ]
     *     }  
     *   ]
     * }
     */
    public static ArtifactoryRevisionState fromFile(File file)
    {
        if (file.exists())
        {
            JsonSlurper slurper = new JsonSlurper();
            JSONObject json = new JSONObject();
            try 
            {
                json = (JSONObject)slurper.parse(file);
            }
            catch(IOException e)
            {
                LOGGER.log(WARNING, "Caught IOException reading: " + file + " " + e.getMessage());
            }

            String artifactID = (String) json.getString(ARTIFACT_ID_TAG);
            String groupID = (String) json.getString(GROUP_TAG);
            String repo = (String) json.getString(REPO_TAG);

            JSONArray jsonArtifacts = json.getJSONArray(ARTIFACTS_TAG);
            ArrayList<Artifact> artifacts = new ArrayList<Artifact>();

            for (int i = 0; i < jsonArtifacts.size(); i++)
            {
                artifacts.add(Artifact.fromJSON( (JSONObject)jsonArtifacts.get(i) ));
            }

            return new ArtifactoryRevisionState(repo, groupID, artifactID, artifacts);
        }
        else
        {
            return BASE;
        }

    }

    /**
     * Function to create the state from a remote server
     * @param repo The repository that contains the artifact
     * @param groupID The group that the artifact belongs to
     * @param artifactID The name of the artifact
     * @param api The API object to use to create the state
     * @return The state on the server
     */
    public static ArtifactoryRevisionState fromServer(String repo, String groupID, String artifactID, String versionFilter, ArtifactoryAPI api)
    {
        ArrayList<Artifact> versions = new ArrayList<Artifact>();

        JSONArray jsonVersions = api.getArtifactVersions(repo, groupID, artifactID, versionFilter);

        for(int i = 0; i < jsonVersions.size(); i++)
        {
            String version = (String) jsonVersions.get(i);
            HashMap<String, FileMetadata> metadataMap = new HashMap<String, FileMetadata>();

            JSONArray jsonFiles = api.getArtifactFiles(repo, groupID, artifactID, version);

            for (int j = 0; j < jsonFiles.size(); j++)
            {
                String fileName = (String) jsonFiles.get(j);
                JSONObject fileMetadataObject = api.getFileMetadata(repo, groupID, artifactID, version, fileName); 
                FileMetadata metadata = FileMetadata.fromJSON(fileMetadataObject);

                metadataMap.put(fileName, metadata);
            }

            versions.add(new Artifact(version, metadataMap));
        }
        return new ArtifactoryRevisionState(repo, groupID, artifactID, versions);
    }

    /**
     * Function to serialize the object into JSON as in the {@link ArtifactoryRevisionState#fromFile(File)} function javadoc
     * @return The serialized object
     */
    public JSONObject toJSON()
    {
        JSONObject json = new JSONObject();
        json.put(ARTIFACT_ID_TAG, artifactID);
        json.put(GROUP_TAG, groupID);
        json.put(REPO_TAG, repo);

        JSONArray jsonArtifacts = new JSONArray();
        for (Artifact artifact: artifacts )
        {
            JSONObject data = artifact.toJSON();
            jsonArtifacts.add(data);
        }

        json.put(ARTIFACTS_TAG, jsonArtifacts);

        return json;
    }


    /**
     * Function to compare two revision states
     * @param stateA The first state to compare
     * @param stateB The second state to compare
     * @return The first different artifact encountered between states, or null if no differences are encountered
     */
    public static Artifact compareRevisionStates(ArtifactoryRevisionState stateA, ArtifactoryRevisionState stateB)
    {
        Artifact nextArtifact = null;

        for (Artifact artifactA : stateA.getArtifacts())
        {
            Artifact artifactB = stateB.getVersion(artifactA.getVersion());

            if (null == artifactB)
            {
            	// There is a new version in stateB
                LOGGER.log(FINE, "Found new version of artifact: " + stateA.artifactID+":"+artifactA.getVersion());
                nextArtifact = artifactA;
                return nextArtifact;
            }
            else
            {
                // Double check that the files are all the same between states
                HashMap<String, FileMetadata> filesA = artifactA.getFileMetadata();
                HashMap<String, FileMetadata> filesB = artifactB.getFileMetadata();

                for (String fileName : filesA.keySet())
                {
                    FileMetadata metadataA = filesA.get(fileName);
                    FileMetadata metadataB = filesB.get(fileName);

                    // metadataB may not exist
                    if (metadataB == null)
                    {
                        // if the file doesn't exist in B, but it exists in A
                    	LOGGER.log(FINE, "Found new file in artifact: " + stateA.artifactID+":"+artifactA.getVersion());
                        nextArtifact = artifactA;
                        return nextArtifact;
                    }
                    else if (! metadataA.getMD5Sum().equals(metadataB.getMD5Sum()) )
                    {
                        // if the file exists in both A and B but has a different md5 sum
                    	LOGGER.log(FINE, "Found changed file in artifact: " + stateA.artifactID+":"+artifactA.getVersion());
                        nextArtifact = artifactA;
                        return nextArtifact;
                    }
                }
            }
        }

        return nextArtifact;
    }
}