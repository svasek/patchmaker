package net.svasek.java.patchmaker.maven;

import net.svasek.java.patchmaker.core.MyUtils;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;

import net.svasek.java.patchmaker.core.PatchCreator;

/**
 * Created by IntelliJ IDEA.
 * User: svasek@gmail.com
 * Date: 19.3.2010
 */
public class ChecksumTest {
    @Ignore("for manual running only")
    @Test
    public void simpleTest() {
        final File tempDirectory = MyUtils.getTmpDir();
        final File dstJar = new File(System.getProperty("file"));

        PatchCreator myPatchCreator = new PatchCreator(null, null, null, tempDirectory, null, null);
        myPatchCreator.makeJarChecksums(dstJar);
    }
}
