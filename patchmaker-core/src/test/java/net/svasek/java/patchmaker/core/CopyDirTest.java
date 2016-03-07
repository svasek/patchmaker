package net.svasek.java.patchmaker.core;

import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.io.File;

/**
 * Created by IntelliJ IDEA.
 * User: svasek@gmail.com
 * Date: 3.2.2010
 */
public class CopyDirTest {
    @Ignore("for manual running only")
    @Test
    public void simpleTest() throws IOException {
        File tmpDir = new File("target");
        File srcDir = new File("src/update");

        MyUtils.copyDirectory(srcDir, tmpDir);
    }
}
