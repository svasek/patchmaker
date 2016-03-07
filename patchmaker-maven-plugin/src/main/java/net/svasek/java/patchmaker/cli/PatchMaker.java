package net.svasek.java.patchmaker.cli;

import net.svasek.java.patchmaker.core.*;
import net.svasek.java.patchmaker.maven.*;

import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: svasek@gmail.com
 * Date: 11.12.2009
 */
public class PatchMaker {

    public static void main(String[] args) throws IOException {

        if (args.length < 4) {
            System.out.println("ERROR: Missing parameters!");
            System.out.println("Example: PatchCreator originalFile.[zip|jar|war|ear] newFile.[zip|jar|war|ear] config.properties [excludes.txt]");
            return;
        }

        final File buildDirectory = MyUtils.getTmpDir();
        final File updateSrcDir = new File("src/update/");
        final File oldFile = new File(args[0]);
        final File newFile = new File(args[1]);

        Properties properties = new Properties();
        properties.load(new FileInputStream(args[2]));

        final String updateName = properties.getProperty("update.name");
        final String productMask = properties.getProperty("product.regexp");
        final String productFamily = properties.getProperty("product.family");
        final String productSvnUrl = properties.getProperty("product.svn.url");
        final String productSvnUser = properties.getProperty("product.svn.user");
        final String productSvnPass = properties.getProperty("product.svn.pass");
        final String dependsOnUpdate = properties.getProperty("update.depends.on");
        final String distEarPath = properties.getProperty("dist.ear.path");
        final List<String> ignoredBugs = Arrays.asList(properties.getProperty("ignored.bugs").split(","));
        final List<String> excludeList = MyUtils.readLines(new File(args[3]));


        //Read archives
        System.out.print("Reading first archive: " + args[0]);
        long startTime = System.currentTimeMillis();
        final Collection<ArchEntry> coll1 = ZipArchiveExplorer.readArchive(oldFile, distEarPath);
        long endTimeF1 = System.currentTimeMillis();
        System.out.println(" [" + (endTimeF1 - startTime) / 1000 + "s]");

        System.out.print("Reading second archive: " + args[1]);
        final Collection<ArchEntry> coll2 = ZipArchiveExplorer.readArchive(newFile, distEarPath);
        long endTimeF2 = System.currentTimeMillis();
        System.out.println(" [" + (endTimeF2 - endTimeF1) / 1000 + "s]");

        //Find changes
        System.out.println("Going to find differencies ...");
        final DiffResult result = ZipArchiveExplorer.findChanges(coll1, coll2, distEarPath, excludeList);

        //Initialize new instance of PatchCreator
        PatchCreator myPatchCreator = new PatchCreator(result, distEarPath, updateName, buildDirectory, productFamily, null);

        //Create META-INF/info.xml
        String oldSvnRevision = MyUtils.getSvnRevisionFromFile(oldFile, distEarPath);
        String newSvnRevision = MyUtils.getSvnRevisionFromFile(newFile, distEarPath);
        System.out.println("Creating file \"META-INF"+ File.separator +"info.xml\".");
        String descriptionTag = MyUtilsExternal.getFixedBugs(productSvnUrl, ignoredBugs == null ? Collections.<String>emptyList() : ignoredBugs, oldSvnRevision, newSvnRevision, productSvnUser, productSvnPass);
        myPatchCreator.createInfoFile(productMask, dependsOnUpdate, descriptionTag);

        //Create META-INF/files.txt
        System.out.println("Creating file \"META-INF"+ File.separator +"files.txt\".");
        myPatchCreator.createFilesList();

        //Create build.xml
        System.out.println("Creating file \"build.xml\".");
        myPatchCreator.createBuildFile(productMask);

        //Unzip all affected files
        System.out.println("Going to unzip all new, or changed files ...");
        ZipArchiveExplorer.unzipAffectedFiles(result, newFile, distEarPath, buildDirectory);

        //Make update jar distribution file
        System.out.println("Making jar file ...");
        MyUtilsExternal.makeJarFile(new File(updateName, ".jar"), updateSrcDir, buildDirectory);

        //Make checksums files
        System.out.println("Making checksum files ...");
        myPatchCreator.makeJarChecksums(new File(updateName, ".jar"));

        //Delete the temporary directory
        MyUtils.deleteTempDir(buildDirectory);
    }

}
