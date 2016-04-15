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

import com.pason.plugins.artifactorypolling.ArtifactoryChangeLogSet.LogEntry;

import hudson.model.AbstractBuild;
import hudson.scm.ChangeLogParser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.xml.sax.SAXException;


/**
 *  A class that can parse an artifactory change log. Must be overridden to create
 *  a SCM implementation
 */
public class ArtifactoryChangeLogParser extends ChangeLogParser {


	/**
	 * Function to parse the changelog
	 */
    @Override
    public ArtifactoryChangeLogSet parse(@SuppressWarnings("rawtypes") AbstractBuild build, File changeLogFile)
        throws IOException, SAXException
    {
        return new ArtifactoryChangeLogSet(build, new ArrayList<LogEntry>());
    }
}