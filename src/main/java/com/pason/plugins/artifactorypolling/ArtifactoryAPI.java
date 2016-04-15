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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.logging.Logger;

import net.sf.json.JSON;
import net.sf.json.JSONObject;
import net.sf.json.JSONArray;
import net.sf.json.groovy.JsonSlurper;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;

import static java.util.logging.Level.FINE;
import static java.util.logging.Level.WARNING;

/**
 * A class to interact with an artifactory server via the API.
 * 
 * All the API calls necessary are encapsulated in this class
 * @author ngutzmann
 *
 */
public class ArtifactoryAPI
{
	/** The url of the artifactory instance */
    private final String artifactoryURL;

    /** A Logger for this class */
    private static final Logger LOGGER = Logger.getLogger(ArtifactoryAPI.class.getName());

    /**
     * Constructor
     * @param artifactoryURL The server's URL.
     */
    public ArtifactoryAPI(String artifactoryURL)
    {
        if (!artifactoryURL.endsWith("/"))
        {
            artifactoryURL += "/";
        }
        
        this.artifactoryURL = artifactoryURL;
    }

    /**
     * Function to retrieve all the available versions for an artifact
     * @param repo The repository the artifact is stored in
     * @param groupID The group that the artifact belongs to
     * @param artifactID The name of the artifact
     * @param versionFilter 
     * @return an array of artifact versions
     */
    public JSONArray getArtifactVersions(String repo, String groupID, String artifactID, String versionFilter)
    {
        String groupURLPart = groupID.replace('.', '/');
        String versionsURL = artifactoryURL + "api/storage/" + repo + '/' + groupURLPart + '/' + artifactID + "/";   
        JSONObject json = (JSONObject) doGET(versionsURL, false);

        JSONArray versions = getSubdirs(json, false);
        JSONArray filteredVersions = new JSONArray();
        String [] filterElements = versionFilter.split("\\.");
        for (int i = 0; i < versions.size(); i++)
        {
            Boolean isValid = true;
            String version = versions.getString(i);
            String [] numbers = version.split("\\.");

            for (int j = 0; j < numbers.length; j++)
            {
                if( !ArtifactoryUtils.isVersionDynamic(filterElements[j]) )
                {
                    if ( numbers[j].equals( filterElements[j] ) )
                    {
                        isValid &= true;
                    }
                    else
                    {
                        isValid &= false;
                        break;
                    }
                }
                else
                {
                    break;
                }
            }

            if (isValid)
            {
                LOGGER.log(FINE, "Version " + version + " matched filter: " + versionFilter);
                filteredVersions.add(version);
            }
        }

        return filteredVersions;
    }

    /**
     * Function to retrieve the files associated with an artifact
     * @param repo The repository the artifact is stored in
     * @param groupID The group that the artifact belongs to
     * @param artifactID The name of the artifact
     * @param version The version of the artifact
     * @return an array of files that belong to an artifact at a version
     */
    public JSONArray getArtifactFiles(String repo, String groupID, String artifactID, String version)
    { 
        String groupURLPart = groupID.replace('.', '/');
        String folderURL = artifactoryURL + "api/storage/" + repo + '/' + groupURLPart + '/' + artifactID + "/" + version +"/";
        JSONObject json = (JSONObject) doGET(folderURL, false);

        return getSubdirs(json, true);
    }

    /**
     * Function to retrieve file metadata of an artifact's file
     * @param repo The repository the artifact is stored in
     * @param groupID The group that the artifact belongs to
     * @param artifactID The name of the artifact
     * @param version The version of the artifact
     * @param fileName The filename to fetch the data for
     * @return JSON that looks like: 
     *   {
	 *		"uri": "http://localhost:8080/artifactory/api/storage/libs-release-local/org/acme/lib/ver/lib-ver.pom",
 	 *		"downloadUri": "http://localhost:8080/artifactory/libs-release-local/org/acme/lib/ver/lib-ver.pom",
	 *		"repo": "libs-release-local",
	 * 		"path": "/org/acme/lib/ver/lib-ver.pom",
	 *		"remoteUrl": "http://some-remote-repo/mvn/org/acme/lib/ver/lib-ver.pom",
	 *		"created": ISO8601 (yyyy-MM-dd'T'HH:mm:ss.SSSZ),
	 *		"createdBy": "userY",
	 *		"lastModified": ISO8601 (yyyy-MM-dd'T'HH:mm:ss.SSSZ),
	 *		"modifiedBy": "userX",
	 *		"lastUpdated": ISO8601 (yyyy-MM-dd'T'HH:mm:ss.SSSZ),
	 *		"size": "1024", //bytes
	 *		"mimeType": "application/pom+xml",
	 *		"checksums":
	 *		{
	 *		        "md5" : string,
	 *		        "sha1" : string
	 *		    },
	 *		"originalChecksums":{
	 *		        "md5" : string,
	 *		        "sha1" : string
	 *		    }
	 *		}
     */
    public JSONObject getFileMetadata(String repo, String groupID, String artifactID, String version, String fileName)
    {
        String groupURLPart = groupID.replace('.', '/');
        String metadataURL = artifactoryURL + "api/storage/" + repo + '/' + groupURLPart + '/' + artifactID + "/" + version +"/" + fileName;
        return (JSONObject) doGET(metadataURL, false);
    }

    /**
     * 	Function to return the local repositories of an artifactory server (not the mirrored ones)
     * @return An array of repositories
     */
    public JSONArray getRepositories()
    {
    	String reposURL = artifactoryURL + "api/repositories/";
    	JSONArray json = (JSONArray) doGET(reposURL, true);
    	return json;
    }
    
    /**
     * Function to log the headers of a web request
     * @param response The response to a web request
     */
    private void logHeaders(HttpResponse response)
    {
        Header[] headers = response.getAllHeaders();
        String headerString = "Status: " + response.getStatusLine().getStatusCode() + " " + response.getStatusLine().getReasonPhrase();
        for (Header header : headers)
        {
            headerString += "\n"+ header.getName() + ": " + header.getValue();
        }
        LOGGER.log(FINE, headerString);
    }

    /**
     * Function that does a GET request to the artifactory server
     * @param url The URL to get 
     * @param isArray whether or not the data is expected as a JSONArray or JSONObject
     * @return A JSONArray or JSONObject depending on the isArray parameter
     */
    private JSON doGET(String url, boolean isArray)
    {
    	JSON json = new JSONObject();
    	if (isArray)
    	{
    		json = new JSONArray();
    	}

        try{
            LOGGER.log(FINE, "GET " + url);

            HttpClient client = HttpClientBuilder.create().build();
            HttpGet request = new HttpGet(url);
            HttpResponse response = client.execute(request);
            
            logHeaders(response);

            BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

            String line = "";
            String jsonString = "";
            while ((line=reader.readLine())!= null)
            {
                jsonString += line;
            }
            reader.close();

            if (200 <= response.getStatusLine().getStatusCode()  && 300 > response.getStatusLine().getStatusCode())
            {
            	JsonSlurper slurper = new JsonSlurper();
            	json = slurper.parseText(jsonString);
            }
        }
        catch(IOException e)
        {
            LOGGER.log(WARNING, "Caught IOException during GET: " + url + " " + e.getMessage() );
        }

        return json;
    } 
    
    /**
     * Function to retrieve subdirs from an artifactory server folder info response
     * @param folderMetadata the data to parse
     * @param allowFiles whether or not to allow files or strictly return directories
     * @return The folders and possibly the files inside a directory as an array
     */
    private JSONArray getSubdirs(JSONObject folderMetadata, boolean allowFiles)
    {
        JSONArray subdirs = new JSONArray();

        if (folderMetadata.has("children"))
        {
            JSONArray children = folderMetadata.getJSONArray("children");
            
            for (int i = 0; i < children.size(); i++)
            {
                JSONObject uriObject = (JSONObject) children.get(i);
                boolean isFolder = uriObject.getBoolean("folder");
                // if we only want folders only add children with the folder property
                if (!allowFiles && isFolder)
                {
                	subdirs.add(uriObject.getString("uri").substring(1));
                }
                else if (allowFiles)
                {
                	subdirs.add(uriObject.getString("uri").substring(1));
                }
                
            }
        }

        return subdirs; 
    }
}