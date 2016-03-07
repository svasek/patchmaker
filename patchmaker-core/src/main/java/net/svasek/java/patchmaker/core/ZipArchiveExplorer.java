package net.svasek.java.patchmaker.core;

import java.io.*;
import java.util.*;
import java.util.jar.Manifest;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.Checksum;
import java.util.zip.CRC32;

public class ZipArchiveExplorer {

    private static final int defaultChunkSize = 8196;

    /**
     * Read distribution archive into collection (recursively)
     *
     * @param inputArchive File name of the archive to read
     * @param distEarPath  Full path to the ear
     * @return Returns collection containing information about files in this archive
     * @throws IOException IOException
     */
    public static Collection<ArchEntry> readArchive(File inputArchive, String distEarPath) throws IOException {
        // Open input file as ZipInputStream
        ZipInputStream inputArchiveStream = new ZipInputStream(new FileInputStream(inputArchive));
        try {
            // Calling recursion
            return readArchiveRecursive(inputArchiveStream, distEarPath, 0);
        } finally {
            inputArchiveStream.close();
        }
    }

    private static Collection<ArchEntry> readArchiveRecursive(ZipInputStream externalInputStream, String distEarPath, int depth) throws IOException {
        final String distributionDir = MyUtils.parseEarPath(distEarPath)[0];
        ZipEntry entry;
        Collection<ArchEntry> subEntries = new ArrayList<ArchEntry>();
        while ((entry = externalInputStream.getNextEntry()) != null) {
            String entryName = entry.getName();
            if ((distributionDir.length() > 0) && (!entryName.startsWith(distributionDir + "/")) && (depth == 0)) {
                continue;
            }

            // Need to update CRC and size, if archive is corupted!
            byte[] myByteArray = null;
            if ((entryName.toUpperCase().endsWith("MANIFEST.MF")) || (entry.getCrc() == -1) || (entry.getSize() == -1)) {
                myByteArray = updateZipEntryCrcAndSize(externalInputStream, entry);
            }

            long entryCrc = entry.getCrc();
            long entrySize = entry.getSize();

            ArchEntry myArchEntry = new ArchEntry(entryName, entryCrc, entrySize);

            if (entry.isDirectory()) {
                myArchEntry.setEntryType(ArchEntryType.DIRECTORY);
            } else if (MyUtils.isArchive(entryName)) {
                myArchEntry.setEntryType(ArchEntryType.ZIP);
            } else if (entryName.toUpperCase().endsWith("MANIFEST.MF")) {
                myArchEntry.setEntryType(ArchEntryType.MANIFEST);
            } else {
                myArchEntry.setEntryType(ArchEntryType.REGULAR);
            }
            subEntries.add(myArchEntry);

            if (myArchEntry.getEntryType() == ArchEntryType.MANIFEST) {
                Manifest manifest = new Manifest(new ByteArrayInputStream(myByteArray));
                myArchEntry.setManifest(manifest);
                MyUtils.cleanManifestAttributes(manifest);
            }

            if (myArchEntry.getEntryType() == ArchEntryType.ZIP) {
                ZipInputStream childInputStream;
                if (myByteArray != null) {
                    InputStream myIs = new ByteArrayInputStream(myByteArray);
                    childInputStream = new ZipInputStream(myIs);
                } else {
                    childInputStream = new ZipInputStream(externalInputStream);
                }
                myArchEntry.getNestedEntries().addAll(readArchiveRecursive(childInputStream, distributionDir, depth + 1));
            }
            externalInputStream.closeEntry();
        }
        return subEntries;
    }

    private static byte[] updateZipEntryCrcAndSize(ZipInputStream in, ZipEntry entry) throws IOException {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        int count;
        byte[] b = new byte[defaultChunkSize];
        while ((count = in.read(b, 0, defaultChunkSize)) > 0) {
            bo.write(b, 0, count);
        }
        byte[] theBytes = bo.toByteArray();
        Checksum checksum = new CRC32();
        checksum.update(theBytes, 0, theBytes.length);
        entry.setCrc(checksum.getValue());
        bo.close();

        long size = theBytes.length;
        if (size > -1) entry.setSize(size);
        return theBytes;
    }

    /**
     * Finding changes betwen two distribution archives (jars).
     *
     * @param origContent Collection containing information about files in original (old) distribution archive.
     * @param newContent  Collection containing information about files in updated (new) distribution archive.
     * @param distEarPath Full path to th ear
     * @param excludeList List of excluded files (regexp)
     * @return DiffResult Results of diffing archives
     */
    public static DiffResult findChanges(Collection<ArchEntry> origContent, Collection<ArchEntry> newContent, String distEarPath, List<String> excludeList) {
        List<ArchPath> addedEntries = new ArrayList<ArchPath>();
        List<ArchPath> removedEntries = new ArrayList<ArchPath>();
        List<ArchPath> changedEntries = new ArrayList<ArchPath>();
        ArchPath prefix = new ArchPath(Collections.<String>emptyList());

        // Going to recursivity
        findChangesRecursive(origContent, newContent, prefix, addedEntries, removedEntries, changedEntries, distEarPath, excludeList);

        return new DiffResult(addedEntries, removedEntries, changedEntries);
    }

    private static void findChangesRecursive(Collection<ArchEntry> origColl, Collection<ArchEntry> newColl, ArchPath prefix, List<ArchPath> addedEntries, List<ArchPath> removedEntries, List<ArchPath> changedEntries, String distEarPath, List<String> excludeList) {
        // List of Added entries
        final Map<String, ArchEntry> right = new LinkedHashMap<String, ArchEntry>();
        for (ArchEntry entry : newColl) {
            right.put(entry.getName(), entry);
        }

        // Changed and removed entries
        for (ArchEntry e : origColl) {
            ArchPath currentPath = prefix.extend(e.getName());
            ArchEntry rightEntry = right.get(e.getName());

            boolean isIgnored = (isIgnored(currentPath.toString(), excludeList, distEarPath));

            if (rightEntry != null) {
                // There are changed entries only
                if (!isIgnored && ((e.getChecksum() != rightEntry.getChecksum()) || (e.getSize() != rightEntry.getSize()))) {
                    if (e.getEntryType() == ArchEntryType.MANIFEST) {
                        if (!e.getManifest().equals(rightEntry.getManifest())) {
                            changedEntries.add(currentPath);
                        }
                    } else {
                        changedEntries.add(currentPath);
                    }
                    // Going into changed zip archive
                    if (e.getEntryType() == ArchEntryType.ZIP) {
                        int sizesBefore = addedEntries.size() + removedEntries.size() + changedEntries.size();
                        findChangesRecursive(e.getNestedEntries(), rightEntry.getNestedEntries(), currentPath, addedEntries, removedEntries, changedEntries, distEarPath, excludeList);
                        int sizesAfter = addedEntries.size() + removedEntries.size() + changedEntries.size();
                        if (sizesBefore == sizesAfter) {
                            changedEntries.remove(currentPath);
                        }
                    }
                }
                // Remove changed or unchanged entries from the list.
                // Remains only the new entries (see below).
                right.remove(e.getName());
            } else if (e.getEntryType() != ArchEntryType.MANIFEST) {
                //Removed entries
                if (!isIgnored) removedEntries.add(currentPath);
            }
        }

        // Added entries
        for (ArchEntry ae : right.values()) {
            ArchPath currentPath = prefix.extend(ae.getName());
            if ((ae.getEntryType() != ArchEntryType.MANIFEST) && (!isIgnored(currentPath.toString(), excludeList, distEarPath))) {
                addedEntries.add(currentPath);
            }
        }

    }

    /**
     * Unziping all new, or chaged files.
     *
     * @param diffResults Contains collection of changed, added and removed files
     * @param newFile     New distribution file (jar).
     * @param distEarPath Full path to the ear
     * @param tmpDir      Temp directory
     * @throws java.io.IOException Throws I/O errors
     */
    public static void unzipAffectedFiles(DiffResult diffResults, File newFile, String distEarPath, File tmpDir) throws IOException {
        // Recreate temporary directory
        MyUtils.createTempDir(tmpDir);

        List<String> addedEntries = new ArrayList<String>();
        for (ArchPath fullPath : diffResults.getAddedEntries()) {
            addedEntries.add(fullPath.toString());
        }

        List<String> changedEntries = new ArrayList<String>();
        for (ArchPath fullPath : diffResults.getChangedEntries()) {
            changedEntries.add(fullPath.toString());
        }

        // Open original file as ZipInputStrem
        final ZipInputStream rootZip = new ZipInputStream(new FileInputStream(newFile));
        try {
            unzipAllAffectedFiles(rootZip, addedEntries, changedEntries, tmpDir, "", distEarPath);
        } finally {
            rootZip.close();
        }
    }

    private static void unzipAllAffectedFiles(ZipInputStream inStream, List<String> addedEntries, List<String> changedEntries, File outputDir, String path, String distEarPath) throws IOException {
        ZipEntry entry;
        while ((entry = inStream.getNextEntry()) != null) {
            String entryName = entry.getName();
            String entryFullPath = path + entryName;
            String outputPath = (MyUtils.fixEarPathInUpdate(path, distEarPath));
            boolean isAdded = false;
            boolean isChanged = false;

            if (addedEntries.contains(entryFullPath)) {
                isAdded = true;
            } else if (changedEntries.contains(entryFullPath)) {
                isChanged = true;
            }

            // If it is directory, create it
            if (entry.isDirectory() && (isChanged || isAdded)) {
                MyUtils.createDir(new File(outputDir + File.separator + fixFileSeparator(outputPath + entryName)));
            } else {
                if ((MyUtils.isArchive(entryName)) && isChanged) {
                    ZipInputStream is = new ZipInputStream(inStream);
                    unzipAllAffectedFiles(is, addedEntries, changedEntries, outputDir, path + entryName + "/", distEarPath);
                } else if (isChanged || isAdded) {
                    File outputFile = new File(outputDir + File.separator + fixFileSeparator(outputPath), fixFileSeparator(entry.getName()));
                    if (!outputFile.getParentFile().exists()) {
                        MyUtils.createDir(outputFile.getParentFile());
                    }

                    // Open the output file
                    String outFilename = fixFileSeparator(outputDir + File.separator + outputPath + entry);
                    System.out.println("Unpacking: " + fixFileSeparator(outputPath + entry));

                    OutputStream out = new FileOutputStream(fixFileSeparator(outFilename));
                    try {
                        // Transfer bytes from the ZIP file to the output file
                        byte[] buf = new byte[defaultChunkSize];
                        int len;
                        while ((len = inStream.read(buf)) > 0) {
                            out.write(buf, 0, len);
                        }
                    } finally {
                        // Close the streams
                        out.close();
                        inStream.closeEntry();
                    }
                }
            }
        }
    }

    private static String fixFileSeparator(String pathStr) {
        return pathStr.replace('/', File.separatorChar).replace('\\', File.separatorChar);
    }

    private static boolean isIgnored(String toFind, List<String> ignoreList, String distEarPath) {
        boolean isOnIgnoreList = false;
        int lineNumber = 0;
        String mySearch = MyUtils.fixEarPathInUpdate(toFind, distEarPath);
        for (String lineInIgnoreList : ignoreList) {
            lineNumber++;
            Pattern searchPattern = Pattern.compile(lineInIgnoreList);
            Matcher searchMatcher = searchPattern.matcher(mySearch);
            if (searchMatcher.matches()) {
                isOnIgnoreList = true;
                System.out.println("IGNORING: File [" + fixFileSeparator(mySearch) + "] matches pattern [" + fixFileSeparator(lineInIgnoreList) + "] found on line number " + lineNumber + "!");
            }
        }
        return isOnIgnoreList;
    }
}
