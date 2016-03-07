package net.svasek.java.patchmaker.core;

import java.util.Collection;
import java.util.ArrayList;
import java.util.jar.Manifest;

/**
 * Contains information about each archive entry.
 */
public class ArchEntry {
    private final String name;
    private final long size;
    private ArchEntryType entryType;
    private Manifest manifest;
    private final long checksum;
    private final Collection<ArchEntry> nestedEntries = new ArrayList<ArchEntry>();

    public ArchEntry(String name, long checksum, long size) {
        this.name = name;
        this.checksum = checksum;
        this.size = size;
    }

    public String getName() {
        return name;
    }

    public long getSize() {
        return size;
    }

    public ArchEntryType getEntryType() {
        return entryType;
    }

    public void setEntryType(ArchEntryType entryType) {
        this.entryType = entryType;
    }

    public long getChecksum() {
        return checksum;
    }

    public Collection<ArchEntry> getNestedEntries() {
        return nestedEntries;
    }

    public Manifest getManifest() {
        return manifest;
    }

    public void setManifest(Manifest manifest) {
        this.manifest = manifest;
    }
}
