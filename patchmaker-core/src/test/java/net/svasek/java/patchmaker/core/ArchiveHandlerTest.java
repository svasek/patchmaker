package net.svasek.java.patchmaker.core;

import org.junit.Test;
import org.junit.Assert;
import org.junit.Ignore;

import java.util.Collection;
import java.util.Set;
import java.util.jar.Manifest;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.FileWriter;

public class ArchiveHandlerTest {
    private static final String FILENAME = System.getProperty("file");

    @Ignore("for manual running only")
    @Test
    public void simpleTest() throws IOException {
        System.out.print("Reading file " + FILENAME);
        final Collection<ArchEntry> coll = ZipArchiveExplorer.readArchive(new File(FILENAME), "");
        Assert.assertFalse("Collection is empty !", coll.isEmpty());
        System.out.println("\t[done]");

        System.out.print("Writing content to file " + FILENAME + "-list.txt");
        PrintWriter pw = new PrintWriter(new FileWriter(FILENAME + "-list.txt"));
        try {
            printZipArch(coll, pw, "");
        } finally {
            pw.close();
        }
        System.out.println("\t[done]");
    }

    void printZipArch(Collection<ArchEntry> myCollection, PrintWriter pw, String printPrefix) throws IOException {
        for (ArchEntry e : myCollection) {
            pw.print(printPrefix + e.getName() + "\t[" + e.getSize() + "B]\t[" + Long.toHexString(e.getChecksum()) + "]\t" + e.getEntryType());

            if (e.getEntryType() == ArchEntryType.MANIFEST) {
                Manifest manifest = e.getManifest();
                if (manifest != null) {
                    Set manifestAttributies = manifest.getMainAttributes().keySet();
                    //Collection manifestValues = manifest.getMainAttributes().values();
                    pw.print("\tManifest contains: { ");
                    for (Object key : manifestAttributies) {
                        pw.print("\"" + key.toString() + ": " + manifest.getMainAttributes().getValue(key.toString()) + "\", ");
                    }
                    pw.println(" }");
                }
            } else {
                pw.println("");
            }

            if (e.getEntryType() == ArchEntryType.ZIP) {
                printZipArch(e.getNestedEntries(), pw, "  " + printPrefix + e.getName() + "/");
            }
        }
    }
}
