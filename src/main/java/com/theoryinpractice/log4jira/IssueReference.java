/*
 * Created by IntelliJ IDEA.
 * User: amrk
 * Date: Oct 21, 2007
 * Time: 8:27:31 PM
 */
package com.theoryinpractice.log4jira;

import java.io.Serializable;
import java.util.Date;

public class IssueReference implements Serializable {
    private String key;
    private String hash;
    private Date lastUpdated;

    public IssueReference() {
    }

    public IssueReference(String key, String hash, Date lastUpdated) {
        this.key = key;
        this.hash = hash;
        this.lastUpdated = lastUpdated;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IssueReference that = (IssueReference) o;

        if (hash != null ? !hash.equals(that.hash) : that.hash != null) return false;
        if (key != null ? !key.equals(that.key) : that.key != null) return false;
        if (lastUpdated != null ? !lastUpdated.equals(that.lastUpdated) : that.lastUpdated != null) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = (key != null ? key.hashCode() : 0);
        result = 31 * result + (hash != null ? hash.hashCode() : 0);
        result = 31 * result + (lastUpdated != null ? lastUpdated.hashCode() : 0);
        return result;
    }
}