/*
 * Created by IntelliJ IDEA.
 * User: amrk
 * Date: Jul 14, 2007
 * Time: 10:10:08 PM
 */
package com.theoryinpractice.log4jira;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

import java.io.*;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class JiraAppender extends AppenderSkeleton {

    private String url;
    private String username;
    private String password;
    private String projectkey;
    private File datafile;

    private long delayPeriod = 1024 * 60 * 10;

    private Layout layout;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getProjectkey() {
        return projectkey;
    }

    public void setProjectkey(String projectkey) {
        this.projectkey = projectkey;
    }

    public long getDelayPeriod() {
        return delayPeriod;
    }

    public void setDelayPeriod(long delayPeriod) {
        this.delayPeriod = delayPeriod;
    }

    public Layout getLayout() {
        return layout;
    }

    public void setLayout(Layout layout) {
        this.layout = layout;
    }

    public File getDatafile() {
        return datafile;
    }

    public void setDatafile(File datafile) {
        this.datafile = datafile;
    }

    public JiraAppender() {
        setDefaultLayout();
    }

    private void setDefaultLayout() {
        PatternLayout patternLayout = new PatternLayout();
        patternLayout.setConversionPattern(PatternLayout.TTCC_CONVERSION_PATTERN);
        layout = patternLayout;
    }

    public JiraAppender(String url, String username, String password, String projectkey) {
        setDefaultLayout();
        this.url = url;
        this.username = username;
        this.password = password;
        this.projectkey = projectkey;
    }

    protected void append(LoggingEvent event) {

        synchronized (this) {
            if (event.getLevel().toInt() >= Level.ERROR.toInt() && event.getThrowableInformation() != null) {

                Set<IssueReference> issueReferences = loadReferences(datafile);
                
                System.out.println("username is " + username);
                if (username.equals("") || password.equals("")) {
                    System.out.println("Missing authentication details...");
                    return;
                }

                if (datafile.equals("") || datafile.equals("")) {
                    System.out.println("Missing datafile details...");
                    return;
                }

                StringBuilder issueBuilder = new StringBuilder();

                String title = layout.format(event).trim();

                issueBuilder.append("The following exception was logged by the application:\n\n");
                issueBuilder.append("{code:title=");
                issueBuilder.append(title);
                issueBuilder.append("}\n");

                String digest;
                MessageDigest md;
                try {
                    md = MessageDigest.getInstance("SHA");
                    String[] throwables = event.getThrowableStrRep();
                    for (String throwable : throwables) {
                        issueBuilder.append(throwable);
                        issueBuilder.append("\n");
                        md.update(throwable.getBytes());
                    }
                    digest = new BigInteger(1, md.digest()).toString(16);
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                    return;
                }

                issueBuilder.append("{code}\n");

                IssueReference storedDigest = getReferenceForHash(issueReferences, digest);

                if (storedDigest == null) {
                    // new exception - create a ticket
                    try {
                        JiraClientContainer clientContainer = getClientContainer();

                        List params = new ArrayList();
                        params.add(clientContainer.token);

                        System.out.println("Creating ticket in project " + projectkey);

                        Hashtable issue = new Hashtable();
                        issue.put("project", projectkey);
                        issue.put("summary", title);
                        issue.put("description", issueBuilder.toString());

                        issue.put("type", 1);
                        //                issue.put("priority", priority);

                        //                if (affectsVersionId != null) {
                        //                    Vector affectsVersions = new Vector();
                        //                    Hashtable affectsVersion = new Hashtable();
                        //                    affectsVersion.put("id", affectsVersionId);
                        //                    affectsVersions.add(affectsVersion);
                        //                    issue.put("affectsVersions", affectsVersions);
                        //                }
                        //
                        //                if (fixForVersionId != null) {
                        //                    Vector fixForVersions = new Vector();
                        //                    Hashtable fixForVersion = new Hashtable();
                        //                    fixForVersion.put("id", fixForVersionId);
                        //                    fixForVersions.add(fixForVersion);
                        //                    issue.put("fixVersions", fixForVersions);
                        //                }

                        params.add(issue);

                        HashMap newIssue = (HashMap) clientContainer.client.execute("jira1.createIssue", params);
                        String newIssueKey = (String) newIssue.get("key");

                        System.out.println("Created ticket " + newIssueKey);

                        issueReferences.add(new IssueReference(newIssueKey, digest, new Date()));


                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    } catch (XmlRpcException e) {
                        e.printStackTrace();
                    }
                } else {
                    // we've seen it before - comment the ticket instead...

                    try {
                        // Extract ticket and time of last submission
                        Date lastUpdated = storedDigest.getLastUpdated();

                        // Only comment if older than 10 minutes - should be configurable
                        Date now = new Date();

                        Date checkDate = new Date(now.getTime() - delayPeriod);

                        if (checkDate.after(lastUpdated)) {


                            JiraClientContainer clientContainer = getClientContainer();

                            System.out.println("Adding comment to " + storedDigest.getKey());
                            List params = new ArrayList();
                            params.add(clientContainer.token);
                            params.add(storedDigest.getKey());
                            params.add(issueBuilder.toString());

                            clientContainer.client.execute("jira1.addComment", params);

                            // Update timestamp
                            storedDigest.setLastUpdated(new Date());

                        }

                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    } catch (XmlRpcException e) {
                        e.printStackTrace();
                    }

                }

                saveReferences(issueReferences, datafile);

            }
        }

    }

    private IssueReference getReferenceForHash(Set<IssueReference> issueReferences, String digest) {
        for (IssueReference issueReference : issueReferences) {
            if (digest.equals(issueReference.getHash())) {
                return issueReference;
            }
        }
        return null;
    }

    private JiraClientContainer getClientContainer() throws MalformedURLException, XmlRpcException {
        JiraClientContainer clientContainer = new JiraClientContainer();

        System.out.println("Connecting to xml-rpc host on " + url);
        XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
        config.setServerURL(new URL(url + "/rpc/xmlrpc"));
        clientContainer.client = new XmlRpcClient();
        clientContainer.client.setConfig(config);

        List params = new ArrayList();
        params.add(username);
        params.add(password);

        System.out.println("Attempting to login to JIRA installation at " + url + " as " + username);

        clientContainer.token = (String) clientContainer.client.execute("jira1.login", params);
        return clientContainer;
    }

    public class JiraClientContainer {
        public XmlRpcClient client;
        public String token;
    }

    public boolean requiresLayout() {
        return true;
    }

    public void saveReferences(Set<IssueReference> issueReferences, File file) {

        try {
            FileOutputStream fout = new FileOutputStream(file);
            ObjectOutputStream oos = new ObjectOutputStream(fout);
            oos.writeObject(issueReferences);
            oos.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }


    public Set<IssueReference> loadReferences(File file) {
        Set<IssueReference> issueReferences = new HashSet<IssueReference>();
        try {
            FileInputStream fin = new FileInputStream(file);
            ObjectInputStream ois = new ObjectInputStream(fin);
            issueReferences = (Set<IssueReference>) ois.readObject();
            ois.close();
            return issueReferences;
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return issueReferences;

    }

    public void close() {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}