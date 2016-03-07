package net.svasek.java.patchmaker.maven;

import net.svasek.java.patchmaker.core.MyUtils;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.SVNAuthenticationException;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.wc.SVNWCUtil;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.codehaus.plexus.archiver.jar.Manifest;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.jar.ManifestException;
import org.codehaus.plexus.archiver.util.DefaultFileSet;
import org.codehaus.plexus.archiver.ArchiverException;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.File;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: svasek@gmail.com
 * Date: 14.4.2010
 */
public class MyUtilsExternal {
    /**
     * Getting SVN log and parseing it.
     *
     * @param svnUrl         URL to the SVN of the product
     * @param ignoredBugs    List of ignored bugs
     * @param oldSvnRevision SVN revision of old distribution
     * @param newSvnRevision SVN revision of new distribution
     * @param svnUser        SVN user name (can be read-only)
     * @param svnPass        SVN user password
     * @return List<String> List of fixed bug numbers except ignored.
     */
    static Set<String> getFixedBugsFromSvn(String svnUrl, List<String> ignoredBugs, String oldSvnRevision, String newSvnRevision, String svnUser, String svnPass) {
        long startRevision = Long.parseLong(oldSvnRevision);
        long endRevision = Long.parseLong(newSvnRevision);
        File svnCacheDir = new File(System.getProperty("java.io.tmpdir"));
        DAVRepositoryFactory.setup();
        SVNRepository repository;
        Collection logEntries = null;

        try { // Login to the SVN repository
            repository = SVNRepositoryFactory.create(SVNURL.parseURIEncoded(svnUrl));
            ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager();
            if ((svnUser != null) && (svnUser.length() > 0)) {
                authManager = SVNWCUtil.createDefaultAuthenticationManager(svnCacheDir, svnUser, svnPass.toCharArray(), false);
                repository.setAuthenticationManager(authManager);
            } else {
                //Workaround for "wincrypted" passwords on MS Windows
                repository.setAuthenticationManager(authManager);
                try {
                    repository.testConnection();
                } catch (SVNAuthenticationException e) {
                    authManager = SVNWCUtil.createDefaultAuthenticationManager(svnCacheDir, "nobody", "secure".toCharArray(), false);
                    repository.setAuthenticationManager(authManager);
                }
            }

            logEntries = repository.log(new String[]{""}, null, startRevision, endRevision, true, true);
        } catch (SVNException e) {
            e.printStackTrace();
        }

        Set<String> fixedBugs = new LinkedHashSet<String>();
        if (logEntries != null) {
            for (Object oneLogEntry : logEntries) {
                SVNLogEntry logEntry = (SVNLogEntry) oneLogEntry;
                if (logEntry.getMessage().toLowerCase().contains("fixing defect")) {
                    Set<String> bugsFromMessage = exctractBugNumbers(logEntry.getMessage());
                    for (String bugNum : bugsFromMessage) {
                        if (!ignoredBugs.contains(bugNum)) {
                            fixedBugs.add(bugNum);
                        }
                    }
                }
            }
        }
        return fixedBugs;
    }

    /**
     * Extracting numbers of fixed bugs from SVN log message.
     * SVN message must be in format "fixing defect #12345: message", or "fixing defects #12345, #54321: message"
     *
     * @param toFind SVN message to search in
     * @return List<String> List of bug numbers
     */
    static Set<String> exctractBugNumbers(String toFind) {
        String pattern = "^\\s*(fixing\\s+defect)s?\\s*((#\\d+,*\\s*)+):(\\s*.*\\s*)";

        // For multiline messages is needed flag "Pattern.DOTALL"
        Pattern searchPattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE + Pattern.DOTALL);
        Matcher searchMatcher = searchPattern.matcher(toFind);
        if (searchMatcher.matches()) {
            // Group2 are bug numbers (separated by colon and space)
            // Group4 contains full svn message (for future use)
            String myBugs = searchMatcher.group(2).replaceAll("[^0-9,]", "");
            return new HashSet<String>(Arrays.asList(myBugs.split(",")));
        }
        return Collections.emptySet();
    }

    public static void makeJar(File dstJar, File tmpDir) {
        try {
            // Add date to the manifest file
            Manifest manifest = new Manifest();
            manifest.addConfiguredAttribute(new Manifest.Attribute("Build-Date", MyUtils.getDateTime()));

            // Make jar archive
            final JarArchiver jarArchiver = new JarArchiver();
            jarArchiver.setCompress(true);
            jarArchiver.addConfiguredManifest(manifest);
            final DefaultFileSet fileSet = new DefaultFileSet();
            fileSet.setDirectory(tmpDir);
            jarArchiver.addFileSet(fileSet);
            jarArchiver.setDestFile(dstJar);
            jarArchiver.createArchive();
        } catch (ManifestException e) {
            e.printStackTrace();
        } catch (ArchiverException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void makeJarFile(File dstJar, File updateSrcDir, File tmpDir) {
        try {
            // Copy content of update src directory to the jar structure
            MyUtils.copyDirectory(updateSrcDir, tmpDir);
            // Make jar archive
            makeJar(dstJar, tmpDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static String getFixedBugs(String productSvnUrl, List<String> ignoredBugs, String oldSvnRevision, String newSvnRevision, String svnUser, String svnPass) {
        final List<String> fixedBugs = new ArrayList<String>(MyUtilsExternal.getFixedBugsFromSvn(productSvnUrl, ignoredBugs, oldSvnRevision, newSvnRevision, svnUser, svnPass));
        Collections.sort(fixedBugs);
        if (fixedBugs.size() > 0) {
            return "Bug fixes (" + fixedBugs.size() + "): " + fixedBugs.toString().replace("[", "").replace("]", "");
        } else {
            return "No bugs fixed";
        }
    }

}
