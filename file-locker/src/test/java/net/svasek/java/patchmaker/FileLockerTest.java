package net.svasek.java.patchmaker;

import org.junit.Ignore;
import org.junit.Test;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: svasek@gmail.com
 * Date: 14.1.11
 */
public class FileLockerTest {
    @Ignore("for manual running only")
    @Test
    public void simpleTest() throws IOException, InterruptedException {
        String[] params = {".", "files.lst"};
        new FileLocker();
        FileLocker.main(params);
    }
}

