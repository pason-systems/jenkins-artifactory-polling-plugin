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

import java.io.Serializable;

import java.util.logging.Logger;

import java.util.HashMap;
import java.util.Map;

import net.sf.json.JSONObject;
import net.sf.json.JSONArray;

import static java.util.logging.Level.FINE;

/**
 * Class that represents an artifact
 * @author ngutzmann
 *
 */
public class Artifact implements Serializable {

	/** The version of the artifact */
    private final String version;
    
    /** The map of files to file metadata for an artifact */
    private final HashMap<String, FileMetadata> metadata;

    /** JSON tags for serializing/deserializing json objects */
    public static final String VERSION_TAG = "version";
    public static final String FILES_TAG = "files";
    public static final String FILE_NAME_TAG = "filename";
    public static final String METADATA_TAG = "metadata";

    /** A logger for the class */
    private static final Logger LOGGER = Logger.getLogger(Artifact.class.getName());

    /**
     * Constructor
     * @param version The version of the artifact
     * @param metadata The file information about the artifact
     */
    public Artifact(String version, HashMap<String, FileMetadata> metadata)
    {
        this.version = version;
        this.metadata = new HashMap<String, FileMetadata>(metadata);
    }

    /**
     * Getter function for the file info of an artifact
     * @return The file info of an artifact
     */
    public HashMap<String, FileMetadata> getFileMetadata()
    {
        return new HashMap<String,FileMetadata>(metadata);
    }

    /**
     * Getter for an artifact's version
     * @return The version of the artifact
     */
    public String getVersion()
    {
        return version;
    }

    /**
     * Function to deserialize JSON that looks like: 
     * {
     *      "version": "V1",
     *      "files": [
     *      {
     *          "filename": "fileName",
     *          "metadata": {
     *               "size": "fileSize",
     *               "checksums": {
     *                   "sha1": "asdfasdfasdfasdfasdfa",
     *                   "md5": "fgjkfghkfghkfghkfghkfgh"
     *                }
     *          }
     *     }]
     * }
     *
     */
    public static Artifact fromJSON(JSONObject json)
    {
        String version = json.getString(VERSION_TAG);

        HashMap<String, FileMetadata> fileMetadata = new HashMap<String, FileMetadata>();
        JSONArray jsonFiles = json.getJSONArray(FILES_TAG);

        for (int i = 0; i < jsonFiles.size(); i++)
        {
            JSONObject file = (JSONObject)jsonFiles.get(i);
            fileMetadata.put(file.getString(FILE_NAME_TAG), FileMetadata.fromJSON((JSONObject)file.get(METADATA_TAG)));
        }

        return new Artifact(version, fileMetadata);
    }

    /**
     * Function to serialize json
     * @return JSON that looks like the {@link Artifact#fromJSON(JSONObject)} data
     */
    public JSONObject toJSON()
    {
        JSONObject json = new JSONObject();
        json.element(VERSION_TAG, version);

        JSONArray fileArray = new JSONArray();
        for(Map.Entry<String, FileMetadata> data : metadata.entrySet())
        {

            JSONObject fileData = new JSONObject();
            fileData.element(FILE_NAME_TAG, data.getKey());
            LOGGER.log(FINE, "JSON Artifact.FileMetadata Key: " + data.getKey());
            fileData.element(METADATA_TAG, data.getValue().toJSON());
            LOGGER.log(FINE, "JSON Artifact.FileMetadata Value: " + data.getValue().toJSON());

            fileArray.add(fileData);
        }

        json.element(FILES_TAG, fileArray);
        return json;
    }


}