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
import hudson.model.User;
import hudson.scm.ChangeLogSet;

import java.net.URL;
import java.net.MalformedURLException;
import java.util.Collection;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;

import org.kohsuke.stapler.export.Exported;

/**
 * Class to represent a changelog for artifactory
 * Must be overriden to implement an SCM
 * @author ngutzmann
 *
 */
public class ArtifactoryChangeLogSet extends ChangeLogSet<LogEntry> {

    private final List<LogEntry> logs;

    /**
     * Constructor
     * @param build The build that will use the changeset
     * @param logs The logs that new since the last build
     */
    ArtifactoryChangeLogSet(AbstractBuild<?,?> build, List<LogEntry> logs)
    {
        super(build);
        this.logs = logs;
    }

    @Override
    public String getKind() 
    {
        return "Artifactory";
    }

    /**
     * Getter for the logs
     * @return The logs
     */
    public List<LogEntry> getLogs()
    {
        return logs;
    }

    @Override
    public boolean isEmptySet() 
    {
        return logs.isEmpty();        
    }
    
    public Iterator<LogEntry> iterator() 
    {
        return logs.iterator();
    }

    /**
     * Inner class to represent a single entry
     * @author ngutzmann
     *
     */
    public static class LogEntry extends ChangeLogSet.Entry {
        private int version;
        private long timestamp;
        private URL url;
        private User user;
        
        @Override
        public ArtifactoryChangeLogSet getParent() {
            return (ArtifactoryChangeLogSet) super.getParent();
        }

        @Override
        protected void setParent(@SuppressWarnings("rawtypes") ChangeLogSet changeLogSet)
        {
            super.setParent(changeLogSet);
        }

        @Exported
        public int getVersion()
        {
            return version;
        }

        public void setVersion(int version)
        {
            this.version = version;
        }


        @Override
        public long getTimestamp()
        {
            return timestamp;
        }

        public void setTimestamp(long timestamp)
        {
            this.timestamp = timestamp;
        }

        @Exported
        public URL getURL()
        {
            return url;
        }

        public void setURL(String url) throws MalformedURLException
        {
            this.url = new URL(url);
        }

        @Override
        public User getAuthor()
        {
            if (user == null)
            {
                return User.getUnknown();
            }
            return user;
        }

        @Override
        public Collection<String> getAffectedPaths()
        {
            ArrayList<String> list = new ArrayList<String>();
            list.add(url.toString());
            return list;
        }

        @Override @Exported
        public String getMsg()
        {
            return new String("");
        }

    }

}