package net.svasek.java.patchmaker.core;

import java.security.MessageDigest;
import java.io.InputStream;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;

/**
 * Gradually create several digests for a stream at once.
 *
 * @author <a href="mailto:pkozelka@gmail.com">Petr Kozelka</a> (extended by Milos Svasek <msvasek@gmail.com>)
 */
public class StreamingMultiDigester {

    private final MessageDigest[] digests;

    private static final int BUFFER_SIZE = 32768;

    static final String HEXES = "0123456789ABCDEF";

    private StreamingMultiDigester(MessageDigest... digests) {
        this.digests = digests;
    }

    private void update(InputStream is) throws IOException {
        final byte[] buffer = new byte[BUFFER_SIZE];
        int size = is.read(buffer, 0, BUFFER_SIZE);
        while (size >= 0) {
            for (MessageDigest digest : digests) {
                digest.update(buffer, 0, size);
            }
            size = is.read(buffer, 0, BUFFER_SIZE);
        }
    }

    public static void compute(File file, MessageDigest... digests) throws IOException {
        final InputStream is = new FileInputStream(file);
        try {
            final StreamingMultiDigester smd = new StreamingMultiDigester(digests);
            smd.update(is);
        } finally {
            is.close();
        }
    }

    public static String getHex(byte[] raw) {
        if (raw == null) {
            return null;
        }
        final StringBuilder hex = new StringBuilder(2 * raw.length);
        for (final byte b : raw) {
            hex.append(HEXES.charAt((b & 0xF0) >> 4))
                    .append(HEXES.charAt((b & 0x0F)));
        }
        return hex.toString().toLowerCase();
    }
}
