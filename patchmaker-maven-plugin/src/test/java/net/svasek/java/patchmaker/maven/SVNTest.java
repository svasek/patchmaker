package net.svasek.java.patchmaker.maven;

import org.junit.Test;
import org.junit.Ignore;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: Milos
 * Date: 15.12.2009
 */
public class SVNTest {
    @Ignore("for manual running only")
    @Test
    public void simpleTest() {
        String svnUrl = "http://your.subversion.repository/svn/reponame/productname/branches/XYZ-2.12-updates";
        String svnUser = "";
        String svnPass = null;
        String startRevision = "76260"; //XYZ-212-RC05
        String endRevision = "86348";
        String[] ignoredBugsArray = {"00000","10123","20345"};
        List<String> ignoredBugs = Arrays.asList(ignoredBugsArray);

        String info = MyUtilsExternal.getFixedBugs(svnUrl, ignoredBugs, endRevision, startRevision, svnUser, svnPass);
        System.out.println(info);
    }
}
