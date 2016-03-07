package net.svasek.java.patchmaker.maven;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import net.svasek.java.patchmaker.core.ArchEntry;
import net.svasek.java.patchmaker.core.DiffResult;
import net.svasek.java.patchmaker.core.PatchCreator;
import net.svasek.java.patchmaker.core.ZipArchiveExplorer;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.util.FileUtils;

/**
 * @goal mkpatch
 */
public class PatchMakerMojo extends AbstractPatchMakerMojo {

    /**
     * The official name of the update. This name will be used internally, and also for the filename of resulting patch inside the "target" directory.
     * However, it will NOT be used for storing inside maven repositories - that is completely under Maven's control.
     *
     * @parameter default-value="project.build.finalName"
     */
    String updateName;

    /**
     * List of matching target product timestamps to which the generated patch is applicable.
     *
     * @parameter
     */
    List<String> productsBuildtimeAplicableTo;

    /**
     * List of files needs to be locked on windows OS family
     *
     * @parameter
     */
    List<String> filesToBeLocked;

    /**
     * Regexp matching target product timestamps to which the generated patch is applicable.
     *
     * @parameter
     * @deprecated Use parameter list {@link #productsBuildtimeAplicableTo} instead
     */
    @Deprecated
    String productTimestampRegexp;

    /**
     * Product family (XYZ or XYZX). Default is XYZ
     *
     * @parameter default-value="XYZ"
     */
    String productFamily;

    /**
     * @parameter
     */
    String updateDependsOn;

    /**
     * @parameter
     */
    List<String> filesExcludeList;

    /**
     * Where to store the output and the temp directory
     *
     * @parameter default-value="project.build.directory"
     */
    File buildDirectory;

    /**
     * Where to store the resources for final update jar
     *
     * @parameter default-value="${basedir}/src/main/resources"
     * @deprecated do not use unless absolutely necessary - will be replaced with maven resources in a further version 
     */
    @Deprecated
    File updateSrcDir;

    /**
     * @component
     */
    MavenProjectHelper projectHelper;

    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            String descriptionTag = "";
            if (descFile.canRead()) {
                descriptionTag = FileUtils.fileRead(descFile.getAbsolutePath());
            }
            final File tempDirectory = new File(buildDirectory, "temp");
            final File resultFile = new File(buildDirectory, updateName + ".jar");
            final File file = makePatch(getLog(), getOldFile(), getNewFile(), distEarPath, updateName, makeproductTimestampRegexp(productsBuildtimeAplicableTo, productTimestampRegexp), productFamily, filesExcludeList, filesToBeLocked, updateDependsOn, descriptionTag, resultFile, updateSrcDir, tempDirectory);
            projectHelper.attachArtifact(project, "jar", null, file);
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        } catch (ArtifactResolutionException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        } catch (ArtifactNotFoundException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private static File makePatch(Log logger, File oldFile, File newFile, String distEarPath, String updateName, String productRegexp, String productFamily, List<String> filesExcludeList, List<String> filesToBeLocked, String updateDependsOn, String descriptionTag, File resultFile, File updateSrcDir, File tempDirectory) throws IOException {
        //Read archives
        logger.info("Reading first archive: " + oldFile);
        long startTime = System.currentTimeMillis();
        final Collection<ArchEntry> coll1 = ZipArchiveExplorer.readArchive(oldFile, distEarPath);
        long endTimeF1 = System.currentTimeMillis();
        logger.info(" [" + (endTimeF1 - startTime) + "ms]");

        logger.info("Reading second archive: " + newFile);
        final Collection<ArchEntry> coll2 = ZipArchiveExplorer.readArchive(newFile, distEarPath);
        long endTimeF2 = System.currentTimeMillis();
        logger.info(" [" + (endTimeF2 - endTimeF1) + "ms]");

        //Find changes
        logger.info("Going to find differences ...");
        final DiffResult result = ZipArchiveExplorer.findChanges(coll1, coll2, distEarPath, filesExcludeList);

        //Initialize new instance of PatchCreator 
        PatchCreator myPatchCreator = new PatchCreator(result, distEarPath, updateName, tempDirectory, productFamily, filesToBeLocked);

        //Create META-INF/files.txt
        logger.info("Creating file \"META-INF" + File.separator + "files.txt\".");
        myPatchCreator.createFilesList();

        logger.info("Creating file \"META-INF" + File.separator + "info.xml\".");
        myPatchCreator.createInfoFile(productRegexp, updateDependsOn, descriptionTag);

        //Create build.xml
        logger.info("Creating file \"build.xml\".");
        myPatchCreator.createBuildFile(productRegexp);

        //Unzip all affected files
        logger.info("Going to unzip all new, or changed files ...");
        ZipArchiveExplorer.unzipAffectedFiles(result, newFile, distEarPath, tempDirectory);

        //Make update jar distribution file
        logger.info("Making jar file ...");
        MyUtilsExternal.makeJarFile(resultFile, updateSrcDir, tempDirectory);

        //Make checksums files
        logger.info("Making checksum files ...");
        myPatchCreator.makeJarChecksums(resultFile);

        return resultFile;
    }

    private static String makeproductTimestampRegexp(List<String> productsBuildtimes, String productTimestampRegexpStr) {
        if ((productsBuildtimes != null) && (productsBuildtimes.size() > 0)) {
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (String buildtimeStr : productsBuildtimes) {
                if (first) {
                    first = false;
                    sb.append("(");
                } else {
                    sb.append("|(");
                }
                sb.append(Pattern.quote(buildtimeStr)).append(")");
            }
            if (productTimestampRegexpStr != null) {
                throw new RuntimeException("You can not use both parameters \"productsBuildtimeAplicableTo\" and \"productTimestampRegexp\"!!");
            }
            return sb.toString();
        } else {
            if (productTimestampRegexpStr != null) {
                System.out.println("");
                System.out.println("");
                System.out.println("");
                System.out.println("");
                System.out.println("  ******************************************************************");
                System.out.println("  **                                                              **");
                System.out.println("  **  WARNING:  DEPRECATED PARAMETER \"productTimestampRegexp\" !!  **");
                System.out.println("  **                                                              **");
                System.out.println("  **  Use parameter \"productsBuildtimeAplicableTo\" instead !!!    **");
                System.out.println("  **                                                              **");
                System.out.println("  ******************************************************************");
                System.out.println("");
                System.out.println("");
                System.out.println("");
                System.out.println("");
                // O.K. users has been warned, so we can continue ;)
                return productTimestampRegexpStr;
            } else {
                throw new RuntimeException("Missing parameter \"productsBuildtimeAplicableTo\" !!");
            }
        }
    }
}
