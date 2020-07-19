package net.svasek.java.patchmaker.maven;

import static net.svasek.java.patchmaker.core.MyUtils.getSvnRevisionFromFile;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.FileUtils;

/**
 * This goal scans SVN log to gather list of QC defect references. These can later be used as part of parametrization of the update.
 * THIS WILL PROBABLY BE RENAMED TO MAKE MORE SENSE
 *
 * @goal holmes
 */
public class SvnHolmesMojo extends AbstractPatchMakerMojo {

    /**
     * @parameter
     */
    String svnUrl;

    /**
     * @parameter property="patchmaker.productSvnUser"
     */
    String svnUser;

    /**
     * @parameter property="patchmaker.productSvnPass"
     */
    String svnPass;

    /**
     * @parameter
     */
    List<String> ignoredBugs;

    public void execute() throws MojoExecutionException, MojoFailureException {

        try {
            String oldSvnRevision = getSvnRevisionFromFile(getOldFile(), distEarPath);
            String newSvnRevision = getSvnRevisionFromFile(getNewFile(), distEarPath);
            getLog().info("Looking for fixed bugs between revisions: "+ oldSvnRevision +" and "+ newSvnRevision +" ...");
            String descriptionTag = MyUtilsExternal.getFixedBugs(svnUrl, ignoredBugs == null ? Collections.<String>emptyList() : ignoredBugs, oldSvnRevision, newSvnRevision, svnUser, svnPass);
            getLog().info("Result: " + descriptionTag);
            //noinspection ResultOfMethodCallIgnored
            descFile.getParentFile().mkdirs();
            FileUtils.fileWrite(descFile.getAbsolutePath(), descriptionTag);
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        } catch (ArtifactResolutionException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        } catch (ArtifactNotFoundException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

}
