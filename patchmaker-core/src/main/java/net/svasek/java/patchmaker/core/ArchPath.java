package net.svasek.java.patchmaker.core;

import java.util.List;
import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: svasek
 * Date: 12.11.2009
 * Time: 10:24:29
 */
public class ArchPath {
    private final List<String> pathItems;

    public ArchPath(List<String> pathItems) {
        this.pathItems = pathItems;
    }

    public ArchPath extend(String newField) {
        final ArrayList<String> pi = new ArrayList<String>(pathItems);
        pi.add(newField);
        return new ArchPath(pi);
    }

    @Override
    public String toString() {
        return MyUtils.concatenate(pathItems, "/");
    }
}
