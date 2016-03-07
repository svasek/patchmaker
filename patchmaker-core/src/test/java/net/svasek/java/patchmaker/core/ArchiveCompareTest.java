package net.svasek.java.patchmaker.core;

import org.junit.Test;
import org.junit.Assert;
import org.junit.Ignore;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.io.File;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: svasek
 * Date: 13.11.2009
 */
public class ArchiveCompareTest {
    @Ignore("for manual running only")
    @Test
    public void simpleCompareTest() throws IOException {
        //final String distEarPath = "distribution/deploy/final-product-distribution.ear";
        final String distEarPath = "";
        final File origFile = new File(System.getProperty("fileOld"));
        final File newFile = new File(System.getProperty("fileNew"));
        final Collection<ArchEntry> coll1 = ZipArchiveExplorer.readArchive(origFile, distEarPath);
        final Collection<ArchEntry> coll2 = ZipArchiveExplorer.readArchive(newFile, distEarPath);

        final List<String> excludeList = new ArrayList<String>();

        System.out.print("Opening 1st file ");
        Assert.assertFalse("Collection 1 is empty !", coll1.isEmpty());
        System.out.println("OK");

        System.out.print("Opening 2nd file ");
        Assert.assertFalse("Collection 2 is empty !", coll1.isEmpty());
        System.out.println("OK");

        System.out.print("Changes: ");

        final DiffResult result = ZipArchiveExplorer.findChanges(coll1, coll2, distEarPath, excludeList);

        //Print Added, Changed and Removed entries.
        System.out.println(" +" + result.getAddedEntries().size() + ", *" + result.getChangedEntries().size() + ", -" + result.getRemovedEntries().size());

        //Print Added entries.
        System.out.println("Added entries:");
        for (ArchPath a : result.getAddedEntries()) {
            System.out.println("\t\"" + a + "\"");
        }

        //Print Changed entries.
        System.out.println("Changed entries:");
        for (ArchPath c : result.getChangedEntries()) {
            System.out.println("\t\"" + c + "\"");
        }

        //Print Removed entries.
        System.out.println("Removed entries:");
        for (ArchPath r : result.getRemovedEntries()) {
            System.out.println("\t\"" + r + "\"");
        }
    }
}
