package net.svasek.java.patchmaker.core;

public enum ArchEntryType {
    /**
     * For directory entries - they have no content
     */
    DIRECTORY,
    /**
     * For regular files which are not archives
     */
    REGULAR,
    /**
     * For MANIFEST files that contains metadata
     */
    MANIFEST,
    /**
     * For regular files which are nested archives; their entries must be filled in {@link ArchEntry#nestedEntries}
     */
    ZIP
}
