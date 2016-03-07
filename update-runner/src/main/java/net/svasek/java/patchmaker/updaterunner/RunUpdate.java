package net.svasek.java.patchmaker.updaterunner;

import java.io.*;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;
import java.util.List;
import java.util.Arrays;
import java.net.URISyntaxException;

/**
 * Created by IntelliJ IDEA.
 * User: svasek@gmail.com
 * Date: 2.6.2010
 */
public class RunUpdate {
    private static final int defaultChunkSize = 8196;

    public static void main(String[] args) throws IOException, URISyntaxException {
        String outDirName;
        if (args.length < 1) {
            throw new RuntimeException("Missing parameter: You must specify XYZX installation directory!");
        } else {
            outDirName = args[0];
        }
        RunUpdate myRun = new RunUpdate();
        File installDir = new File(outDirName);
        File jarFile = new File(RunUpdate.class.getProtectionDomain().getCodeSource().getLocation().toURI());

        if (installDir.isDirectory() && installDir.canWrite() && isXyzxDist(installDir)) {
            String updateName = jarFile.getName().substring(0, jarFile.getName().indexOf(".jar"));
            File outDir = new File(installDir, "updates" + File.separator + updateName);
            createDir(outDir);

            System.out.println("Unpacking to directory: \"" + outDir + "\"");
            myRun.unpack(jarFile, outDir);

            System.out.print("Executing Ant ... ");
            String line;
            Process p = Runtime.getRuntime().exec("java -classpath META-INF/lib/ant-launcher.jar org.apache.tools.ant.launch.Launcher -noclasspath -nouserlib -lib META-INF/lib -Ddont.ask=\"true\"", null, outDir);
            BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
            while ((line = input.readLine()) != null) {
                System.out.println(line);
            }
            input.close();
        } else {
            throw new RuntimeException("Specified directory is not valid XYZX installation directory!");
        }
    }

    RunUpdate() {
    }

    private static boolean isXyzxDist(File dir) {
        List<String> lst = Arrays.asList(dir.list());
        if (!lst.isEmpty()) {
            if (lst.contains("bin") && lst.contains("conf") && lst.contains("lib") && lst.contains("webapps")) {
                File f1 = new File(dir, "conf/mipServer.xml");
                File f2 = new File(dir, "lib/wsm-broker.jar");
                File f3 = new File(dir, "webapps/bse.war");
                return f1.isFile() && f2.isFile() && f3.isFile();
            }
        }
        return false;
    }

    private void unpack(File jarFile, File outputDir) throws IOException {
        final ZipInputStream rootZipStream = new ZipInputStream(new FileInputStream(jarFile));
        ZipEntry entry;

        while ((entry = rootZipStream.getNextEntry()) != null) {
            String entryName = entry.getName();
            if (!entryName.startsWith(this.getClass().getPackage().getName())) {
                // If it is directory, create it
                if (entry.isDirectory()) {
                    createDir(new File(outputDir + File.separator + fixFileSeparator(entryName)));
                } else {
                    File outputFile = new File(outputDir + File.separator + fixFileSeparator(entry.getName()));
                    if (!outputFile.getParentFile().exists()) {
                        createDir(outputFile.getParentFile());
                    }

                    // Open the output file
                    String outFilename = fixFileSeparator(outputDir + File.separator + entry);
                    OutputStream out = new FileOutputStream(fixFileSeparator(outFilename));
                    try {
                        // Transfer bytes from the ZIP file to the output file
                        byte[] buf = new byte[defaultChunkSize];
                        int len;
                        while ((len = rootZipStream.read(buf)) > 0) {
                            out.write(buf, 0, len);
                        }
                    } finally {
                        // Close the streams
                        out.close();
                        rootZipStream.closeEntry();
                    }
                }
            }
        }
    }

    static void createDir(File dir) {
        if (dir.exists()) {
            if (!dir.isDirectory()) {
                throw new RuntimeException("Can not create directory \"" + dir + "\"! File with this name already exists!");
            }
        } else {
            if (!dir.mkdirs()) throw new RuntimeException("Can not create dir " + dir);
        }
    }

    private static String fixFileSeparator(String pathStr) {
        return pathStr.replace('/', File.separatorChar).replace('\\', File.separatorChar);
    }
}