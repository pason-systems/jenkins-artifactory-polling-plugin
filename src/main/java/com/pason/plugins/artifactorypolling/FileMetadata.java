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

import net.sf.json.JSONObject;

import static java.util.logging.Level.FINE;

/**
 * Class that describes a file including the MD5 and SHA1 hashes and the size of the file in bytes
 * @author ngutzmann
 *
 */
public class FileMetadata implements Serializable{

	/** A Logger for the class */
    static final Logger LOGGER = Logger.getLogger(FileMetadata.class.getName());

    /** The MD5 sum of the file */
    private final String md5Sum;
    
    /** The SHA-1 sum of the file */
    private final String sha1Sum;
    
    /** The size of the file in bytes */
    private final long fileSize;

    /** JSON Parsing constants */
    public static final String SIZE_TAG = "size";
    public static final String CHECKSUMS_TAG = "checksums";
    public static final String MD5_TAG = "md5";
    public static final String SHA1_TAG = "sha1";
    
    /**
     * Constructor
     * @param md5Sum The MD5 sum of the file
     * @param sha1Sum The SHA-1 sum of the file
     * @param size The size of the file in bytes
     */
    public FileMetadata(String md5Sum, String sha1Sum, long size)
    {
        this.md5Sum = md5Sum;
        this.sha1Sum = sha1Sum;
        this.fileSize = size;
    }

    /**
     * Getter function
     * @return the MD5 sum of the file
     */
    public String getMD5Sum()
    {
        return md5Sum;
    }

    /**
     * Getter function
     * @return the SHA-1 sum of the file
     */
    public String getSHA1Sum()
    {
        return sha1Sum;
    }

    /**
     * Getter function
     * @return the size of the file in bytes
     */
    public long getSize()
    {
        return fileSize;
    }


    /**
     * Function to serialize the object to something that looks like:
     * {
     *      "size": 123456,
     *      "checksums": {
     *          "md5": "106cd40a9210249f791afebcd0012fa7",
     *          "sha1": "f4a2f41ea4c23d151cd7a4fde5989b56cc42873e",
     *      } 
     * }
     * 
     */
    public JSONObject toJSON()
    {
        JSONObject retVal = new JSONObject();
        retVal.element(SIZE_TAG, fileSize);
        JSONObject checksums = new JSONObject();
        checksums.element(MD5_TAG, md5Sum);
        checksums.element(SHA1_TAG, sha1Sum);
        LOGGER.log(FINE, "Created checksums JSON: " + checksums.toString());
        retVal.element(CHECKSUMS_TAG, checksums);

        LOGGER.log(FINE, "Created FileMetadata JSON: " + retVal.toString());

        return retVal;
    }

    /**
     * Creates a {@link FileMetadata} object from a json object like the one described in
     * {@link FileMetadata#toJSON()}
     * @param json The json to deserialize
     * @return The FileMetadata object
     */
    public static FileMetadata fromJSON(JSONObject json)
    {
        JSONObject checksums = (JSONObject) json.get(CHECKSUMS_TAG);
        String md5Sum = checksums.getString(MD5_TAG);
        String sha1Sum = checksums.getString(SHA1_TAG);
        long filesize = json.getLong(SIZE_TAG);

        return new FileMetadata(md5Sum, sha1Sum, filesize);
    }
}