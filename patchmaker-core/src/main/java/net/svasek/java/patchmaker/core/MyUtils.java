package net.svasek.java.patchmaker.core;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Created by IntelliJ IDEA.
 * User: svasek@gmail.com
 * Date: 7.12.2009
 */
public class MyUtils {
    /**
     * Return array of tree items: <br>
     * [0] = distDirectory <br>
     * [1] = distEarLocation <br>
     * [2] = distEarName <br>
     *
     * @param distEarPath Full path to the ear
     * @return Array[distDirectory, distEarLocation, distEarName]
     */
    static String[] parseEarPath(String distEarPath) {
        if (distEarPath != null) {
            if (distEarPath.length() > 0) {
                String[] distEarPathSplit = distEarPath.split("/");
                if (distEarPathSplit.length > 0) {
                    List<String> fullEarPath = Arrays.asList(distEarPathSplit);
                    return new String[]{fullEarPath.get(0), MyUtils.concatenate(fullEarPath.subList(0, (fullEarPath.size() - 1)), "/"), fullEarPath.get(fullEarPath.size() - 1)};
                }
            }
        }
        return new String[]{"", "", ""};
    }

    /**
     * Changing location of distribution ear, because it should be in root of update
     *
     * @param fullEntryPath String containing full entry path
     * @param distEarPath   Full path of ear file
     * @return fullEntryPath String containing fixed entry path for product ear content
     */
    static String fixEarPathInUpdate(String fullEntryPath, String distEarPath) {
        if (fullEntryPath != null && distEarPath != null) {
            if (fullEntryPath.startsWith(distEarPath)) {
                String distEarName = parseEarPath(distEarPath)[2];
                fullEntryPath = fullEntryPath.replaceAll("^" + distEarPath, distEarName);
            }
        }
        return fullEntryPath;
    }

    /**
     * Cleans MANIFEST.MF file from irrelevant attributes.
     *
     * @param manifest Manifest object
     */
    static void cleanManifestAttributes(Manifest manifest) {
        Attributes manifestAttr = manifest.getMainAttributes();
/* Fixing bug #57799:
        manifestAttr.remove(Attributes.Name.IMPLEMENTATION_TITLE);
        manifestAttr.remove(Attributes.Name.IMPLEMENTATION_VENDOR);
        manifestAttr.remove(Attributes.Name.IMPLEMENTATION_VERSION);
        manifestAttr.remove(Attributes.Name.IMPLEMENTATION_VENDOR_ID);
        manifestAttr.remove(Attributes.Name.IMPLEMENTATION_URL);
        manifestAttr.remove(Attributes.Name.SPECIFICATION_VENDOR);
        manifestAttr.remove(Attributes.Name.SPECIFICATION_TITLE);
        manifestAttr.remove(Attributes.Name.SPECIFICATION_VERSION);
*/
        manifestAttr.remove(new Attributes.Name("Archiver-Version"));
        manifestAttr.remove(new Attributes.Name("Created-By"));
        manifestAttr.remove(new Attributes.Name("Built-By"));
        manifestAttr.remove(new Attributes.Name("Build-Id"));
        manifestAttr.remove(new Attributes.Name("Build-Jdk"));
        manifestAttr.remove(new Attributes.Name("Revision"));
        manifestAttr.remove(new Attributes.Name("Ant-Version"));
        manifestAttr.remove(new Attributes.Name("Maven-Version"));
        manifestAttr.remove(new Attributes.Name("Product"));
        manifestAttr.remove(new Attributes.Name("BuildBranchName"));
        manifestAttr.remove(new Attributes.Name("HP-Build-Machine"));
        manifestAttr.remove(new Attributes.Name("HP-Build-Number"));
        manifestAttr.remove(new Attributes.Name("HP-App-Description"));
        manifestAttr.remove(new Attributes.Name("HP-Build-Initiator"));
        manifestAttr.remove(new Attributes.Name("HP-Build-Date"));
        manifestAttr.remove(new Attributes.Name("Sealed"));
    }

    /**
     * Creating directory if not exist.
     *
     * @param dir Name of the new directory
     */
    public static void createDir(File dir) {
        if (dir.exists()) {
            if (!dir.isDirectory()) {
                throw new RuntimeException("Can not create directory \"" + dir + "\"! File with this name already exists!");
            }
        } else {
            if (!dir.mkdirs()) throw new RuntimeException("Can not create dir " + dir);
        }
    }

    /**
     * Checking if is archive.
     *
     * @param name File name to chech if is archive
     * @return Return true is archive
     */
    static boolean isArchive(String name) {
        String s = name.toLowerCase();
        return (s.endsWith(".jar") || s.endsWith(".war") || s.endsWith(".zip") || s.endsWith(".ear"));
    }

    /**
     * Read the text file
     *
     * @param fromFile File to read from
     * @return List of strings
     */
    public static List<String> readLines(File fromFile) {
        InputStream in = null;
        try {
            in = new FileInputStream(fromFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in));
        List<String> lines = new ArrayList<String>();
        String line;
        try {
            while ((line = bufferedReader.readLine()) != null) {
                if (!line.startsWith("#") || !line.equals("")) {
                    lines.add(line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                bufferedReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return lines;
    }

    /**
     * Copy contents from the specified source directory to the specified destination directory. <br>
     * Skiping files and directories with name started with dot (.)
     *
     * @param srcPath source location (directory)
     * @param dstPath destination location (directory)
     * @throws IOException IOException
     */
    public static void copyDirectory(File srcPath, File dstPath) throws IOException {
        // Make filename filter
        FilenameFilter filter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return !name.startsWith(".");
            }
        };

        if (srcPath.isDirectory()) {
            if (!dstPath.exists()) {
                //noinspection ResultOfMethodCallIgnored
                dstPath.mkdir();
            }

            String files[] = srcPath.list(filter);
            for (String file : files) {
                copyDirectory(new File(srcPath, file), new File(dstPath, file));
            }
        } else {
            if (!srcPath.exists()) {
                System.err.println("WARNING: Can not copy file \"" + srcPath + "\" does not exist!");
            } else if (dstPath.exists()) {
                throw new RuntimeException("ERROR: Destination file \"" + dstPath + "\" already exist!");
            } else {
                System.out.println("Copying \"" + srcPath + "\" to \"" + dstPath + "\"");
                InputStream in = new FileInputStream(srcPath);
                OutputStream out = new FileOutputStream(dstPath);
                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                in.close();
                out.close();
            }
        }
    }

    public static boolean deleteTempDir(File tmpDir) {
        return deleteDirectory(tmpDir);
    }

    private static boolean deleteDirectory(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    //noinspection ResultOfMethodCallIgnored
                    file.delete();
                }
            }
        }
        return (path.delete());
    }

    static File createTempDir(File tmpDir) {
        tmpDir.deleteOnExit();
        createDir(tmpDir);
        return tmpDir;
    }

    public static String getDateTime() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();
        return dateFormat.format(date);
    }

    static String concatenate(Collection<String> strings, String delimiter) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String string : strings) {
            if (first) {
                first = false;
            } else {
                sb.append(delimiter);
            }
            sb.append(string);
        }
        return sb.toString();
    }

    public static String getSvnRevisionFromFile(File inputArchive, String distEarPath) throws IOException {
        final String distributionDir = MyUtils.parseEarPath(distEarPath)[0];
        String filePath = distributionDir + "/conf/buildnumber";
        return readOneLineOfTextFileFromZip(inputArchive, filePath);
    }

    private static String readOneLineOfTextFileFromZip(File inputArchive, String filePath) throws IOException {
        String myRevision = null;
        final ZipInputStream inputArchiveStream = new ZipInputStream(new FileInputStream(inputArchive));
        try {
            ZipEntry entry;
            while ((entry = inputArchiveStream.getNextEntry()) != null) {
                String entryName = entry.getName();
                if (entryName.equals(filePath)) {
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputArchiveStream));
                    myRevision = bufferedReader.readLine();
                }
            }
            return myRevision;
        } finally {
            inputArchiveStream.close();
        }
    }

    /**
     * Extracting numbers of fixed bugs from SVN log message.
     * SVN message must be in format "fixing defect #12345: message", or "fixing defects #12345, #54321: message"
     *
     * @param toFind SVN message to search in
     * @return List<String> List of bug numbers
     */
    public static Set<String> exctractBugNumbers(String toFind) {
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

    public static File getTmpDir() {
        String sysTempDir = System.getProperty("java.io.tmpdir");
        if (!(sysTempDir.endsWith("/") || sysTempDir.endsWith("\\"))) {
            sysTempDir = sysTempDir + File.separator;
        }
        return new File(sysTempDir + "_patchmaker_tmp" + File.separator);
    }
}
