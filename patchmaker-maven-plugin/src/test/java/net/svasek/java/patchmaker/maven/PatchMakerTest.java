package net.svasek.java.patchmaker.maven;

import org.junit.Test;
import org.junit.Ignore;

import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.util.*;

import net.svasek.java.patchmaker.core.*;

/**
 * Created by IntelliJ IDEA.
 * User: svasek@gmail.com
 * Date: 7.12.2009
 */
public class PatchMakerTest {
    /**
     * Need this system properties: <br>
     * fileOld:  Old distribution jar <br>
     * fileNew:  New distribution jar <br>
     * config:   Properties file <br>
     * excludes: Exclude list file <br>
     *
     * @throws IOException IOException
     */
    @Ignore("for manual running only")
    @Test
    public void PatchMakerRunTest() throws IOException {
        final File tempDirectory = new File("working");
        //final File updateSrcDir = new File("src/update/");
        final File oldFile = new File(System.getProperty("fileOld"));
        final File newFile = new File(System.getProperty("fileNew"));

        Properties properties = new Properties();
        properties.load(new FileInputStream(System.getProperty("config")));

        final String updateName = properties.getProperty("update.name");
/*
        final String productMask = properties.getProperty("product.regexp");
        final String productSvnUrl = properties.getProperty("product.svn.url");
        final String productSvnUser = properties.getProperty("product.svn.user");
        final String productSvnPass = properties.getProperty("product.svn.pass");
        final String dependsOnUpdate = properties.getProperty("update.depends.on");
*/
        final String distEarPath = properties.getProperty("dist.ear.path");
        final String productFamily = properties.getProperty("product.family");
        //final List<String> ignoredBugs = Arrays.asList(properties.getProperty("ignored.bugs").split(","));
        final List<String> excludeList = MyUtils.readLines(new File(System.getProperty("excludes")));

        //Read archives
        System.out.print("Reading first archive: " + System.getProperty("fileOld"));
        long startTime = System.currentTimeMillis();
        final Collection<ArchEntry> coll1 = ZipArchiveExplorer.readArchive(oldFile, distEarPath);
        long endTimeF1 = System.currentTimeMillis();
        System.out.println(" [" + (endTimeF1 - startTime) / 1000 + "s]");

        System.out.print("Reading second archive: " + System.getProperty("fileNew"));
        final Collection<ArchEntry> coll2 = ZipArchiveExplorer.readArchive(newFile, distEarPath);
        long endTimeF2 = System.currentTimeMillis();
        System.out.println(" [" + (endTimeF2 - endTimeF1) / 1000 + "s]");

        //Find changes
        System.out.println("Going to find differencies ...");
        final DiffResult result = ZipArchiveExplorer.findChanges(coll1, coll2, distEarPath, excludeList);

        //Initialize new instance of PatchCreator
        PatchCreator myPatchCreator = new PatchCreator(result, distEarPath, updateName, tempDirectory, productFamily, null);

        //Create META-INF/info.xml
        //String oldSvnRevision = getSvnRevisionFromFile(oldFile, distEarPath);
        //String newSvnRevision = getSvnRevisionFromFile(newFile, distEarPath);
        //System.out.println("Creating file \"META-INF"+ File.separator +"info.xml\".");
        //String descriptionTag = MyUtils.getFixedBugs(productSvnUrl, ignoredBugs == null ? Collections.<String>emptyList() : ignoredBugs, oldSvnRevision, newSvnRevision, productSvnUser, productSvnPass);
        //String descriptionTag = "";
        //myPatchCreator.createInfoFile(productMask, dependsOnUpdate, descriptionTag);

        //Create META-INF/files.txt
        System.out.println("Creating file \"META-INF"+ File.separator +"files.txt\".");
        myPatchCreator.createFilesList();

        //Create build.xml
        //System.out.println("Creating file \"build.xml\".");
        //myPatchCreator.createBuildFile(productMask);

        //Unzip all affected files
        System.out.println("Going to unzip all new, or changed files ...");
        ZipArchiveExplorer.unzipAffectedFiles(result, newFile, distEarPath, tempDirectory);

        //Make update jar distribution file
        //System.out.println("Making jar file ...");
        //myPatchCreator.makeJarFile(new File(updateName, ".jar"), updateSrcDir);

        //Make checksums files
        //System.out.println("Making checksum files ...");
        //myPatchCreator.makeJarChecksums(new File(updateName, ".jar"));

        //Delete the temporary directory
        //MyUtils.deleteTempDir(tempDirectory);
    }
}
