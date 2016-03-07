package net.svasek.java.patchmaker.core;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: svasek
 * Date: 18.11.2009
 */
public final class DiffResult {
    private final List<ArchPath> addedEntries;

    private final List<ArchPath> removedEntries;

    private final List<ArchPath> changedEntries;

    /**
     * @param addedEntries   Contains List of Added entries
     * @param removedEntries Contains List of Removed entries
     * @param changedEntries Contains List of Changed entries
     */
    public DiffResult(List<ArchPath> addedEntries, List<ArchPath> removedEntries, List<ArchPath> changedEntries) {
        this.addedEntries = addedEntries;
        this.removedEntries = removedEntries;
        this.changedEntries = changedEntries;
    }

    public List<ArchPath> getAddedEntries() {
        return addedEntries;
    }

    public List<ArchPath> getRemovedEntries() {
        return removedEntries;
    }

    public List<ArchPath> getChangedEntries() {
        return changedEntries;
    }
}
