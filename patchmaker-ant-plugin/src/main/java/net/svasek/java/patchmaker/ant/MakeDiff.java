package net.svasek.java.patchmaker.ant;

import net.svasek.java.patchmaker.core.*;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.PatternSet;
import org.codehaus.plexus.archiver.jar.Manifest;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.jar.ManifestException;
import org.codehaus.plexus.archiver.util.DefaultFileSet;
import org.codehaus.plexus.archiver.ArchiverException;

import java.io.*;
import java.util.*;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;

/**
 * Created by IntelliJ IDEA.
 * User: svasek@gmail.com
 * Date: 14.4.2010
 */

public class MakeDiff extends Task {
    private static final int defaultChunkSize = 8196;
    private static final String updateRunnerJarLocation = "resources/update-runner.jar";
    private static final String distEarPath = "";
    private String updateName;
    private File oldFile;
    private File newFile;
    private File tempDir;
    private File updateResources;
    private File dstJar;
    private static List<String> excludeList = new ArrayList<String>();
    private Vector<PatternSet> patternsets = new Vector<PatternSet>();

    // The method executing the task
    public void execute() throws BuildException {
        try {
            //Read archives
            System.out.println("Reading first (old) archive: " + oldFile);
            long startTime = System.currentTimeMillis();
            final Collection<ArchEntry> coll1 = ZipArchiveExplorer.readArchive(oldFile, distEarPath);
            long endTimeF1 = System.currentTimeMillis();
            System.out.println("- [read in " + (endTimeF1 - startTime) + " ms]");

            System.out.println("Reading second (new) archive: " + newFile);
            final Collection<ArchEntry> coll2 = ZipArchiveExplorer.readArchive(newFile, distEarPath);
            long endTimeF2 = System.currentTimeMillis();
            System.out.println("- [read in " + (endTimeF2 - endTimeF1) + " ms]");

            //Get exclude list
            excludeList.addAll(getExcludeList());

            //Find changes
            System.out.println("Going to find differencies ...");
            final DiffResult result = ZipArchiveExplorer.findChanges(coll1, coll2, distEarPath, excludeList);

            //Initialize new instance of PatchCreator
            PatchCreator myPatchCreator = new PatchCreator(result, distEarPath, updateName, tempDir, "XYZX", null);

            //Cleaning tempDir
            cleanTempDir(tempDir);

            //Create META-INF/files.txt
            System.out.println("Creating file \"META-INF" + File.separator + "files.txt\".");
            myPatchCreator.createFilesList();

            //Create build.xml
            System.out.println("Creating file \"build.xml\".");
            myPatchCreator.createBuildFile("");

            //Unzip all affected files
            System.out.println("Going to unzip all new, or changed files ...");
            // In XYZX is everything in "dstribution" because there are not any EAR fie
            File distSubDir = new File(tempDir, "distribution");
            MyUtils.createDir(distSubDir);
            ZipArchiveExplorer.unzipAffectedFiles(result, newFile, distEarPath, distSubDir);

            //Adding update-runner funcionality
            System.out.println("Adding main-class and libraries ...");
            unpackUpdateRunner(tempDir);

            //Make update jar distribution file
            System.out.println("Making distribution jar file ...");
            dstJar = new File(tempDir.getParent() + File.separator + updateName + ".jar");
            makeFinalJar();

            //Make checksums files
            System.out.println("Making checksum files ...");
            myPatchCreator.makeJarChecksums(dstJar);

            // Delete temporary directory
            MyUtils.deleteTempDir(tempDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void makeFinalJar() {
        try {
            // Trying to use previously created manifest file
            File manifestFile = new File(tempDir, "META-INF/MANIFEST.MF");
            Manifest manifest;
            if (manifestFile.exists() && manifestFile.canRead()) {
                Reader fr = new FileReader(manifestFile);
                manifest = new Manifest(fr);
            } else {
                manifest = new Manifest();
            }
            // Adding resouces directory
            if(updateResources != null && updateResources.exists() && updateResources.isDirectory()) {
                MyUtils.copyDirectory(updateResources, tempDir);
            }
            // Add date to the manifest file
            manifest.addConfiguredAttribute(new Manifest.Attribute("Build-Date", MyUtils.getDateTime()));
            // Make jar archive
            final JarArchiver jarArchiver = new JarArchiver();
            jarArchiver.setCompress(true);
            jarArchiver.addConfiguredManifest(manifest);
            final DefaultFileSet fileSet = new DefaultFileSet();
            fileSet.setDirectory(tempDir);
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

    private List<String> getExcludeList() {
        List<String> excludePatterns = new ArrayList<String>();
        if (patternsets != null && patternsets.size() > 0) {
            for (int v = 0, size = patternsets.size(); v < size; v++) {
                PatternSet p = patternsets.elementAt(v);
                String[] excls = p.getExcludePatterns(getProject());
                if (excls != null) {
                    for (String excl : excls) {
                        String pattern = excl.replace('\\', File.separatorChar);
                        if (pattern.endsWith("/") || pattern.endsWith("\\") || pattern.endsWith(File.separator)) {
                            pattern += ".*";
                        }
                        excludePatterns.add(pattern);
                    }
                }
            }
        }
        return excludePatterns;
    }

    private void unpackUpdateRunner(File outputDir) {
        ClassLoader cl = this.getClass().getClassLoader();
        ZipInputStream updateRunnerJar = new ZipInputStream(cl.getResourceAsStream(updateRunnerJarLocation));
        ZipEntry entry;
        try {
            while ((entry = updateRunnerJar.getNextEntry()) != null) {
                String entryName = entry.getName();
                if (!entryName.startsWith(this.getClass().getPackage().getName())) {
                    // If it is directory, create it
                    if (entry.isDirectory()) {
                        MyUtils.createDir(new File(outputDir + File.separator + fixFileSeparator(entryName)));
                    } else {
                        File outputFile = new File(outputDir + File.separator + fixFileSeparator(entry.getName()));
                        if (!outputFile.getParentFile().exists()) {
                            MyUtils.createDir(outputFile.getParentFile());
                        }
                        // Open the output file
                        String outFilename = fixFileSeparator(outputDir + File.separator + entry);
                        OutputStream out = new FileOutputStream(fixFileSeparator(outFilename));
                        try {
                            // Transfer bytes from the ZIP file to the output file
                            byte[] buf = new byte[defaultChunkSize];
                            int len;
                            while ((len = updateRunnerJar.read(buf)) > 0) {
                                out.write(buf, 0, len);
                            }
                        } finally {
                            // Close the streams
                            out.close();
                            updateRunnerJar.closeEntry();
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String fixFileSeparator(String pathStr) {
        return pathStr.replace('/', File.separatorChar).replace('\\', File.separatorChar);
    }

    private static void cleanTempDir(File dir) {
        if (dir.exists()) {
            System.out.println("Tempdir already exist: Cleaning up!");
            MyUtils.deleteTempDir(dir);
        } else {
            System.out.println("Tempdir does not exist: Creating it!");
        }
        MyUtils.createDir(dir);
    }

    /**
     * Add a patternset.
     *
     * @param set a pattern set
     */
    public void addPatternset(PatternSet set) {
        patternsets.addElement(set);
    }

    public void setOldFile(File oldFile) {
        this.oldFile = oldFile;
    }

    public void setNewFile(File newFile) {
        this.newFile = newFile;
    }

    public void setUpdateName(String updateName) {
        this.updateName = updateName;
    }

    public void setTempDir(File tempDir) {
        this.tempDir = new File(tempDir, updateName);
    }

    public void setUpdateResources(File updateResources) {
        this.updateResources = updateResources;
    }
}
