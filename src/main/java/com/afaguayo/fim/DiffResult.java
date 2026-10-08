package com.afaguayo.fim;

import java.util.List;

/** The result of comparing a current directory snapshot against a stored baseline. */
public record DiffResult(
        List<String> added,
        List<String> removed,
        List<Modified> modified
) {
    public record Modified(String path, String oldHash, String newHash) {
    }

    public boolean isClean() {
        return added.isEmpty() && removed.isEmpty() && modified.isEmpty();
    }

    public int totalChanges() {
        return added.size() + removed.size() + modified.size();
    }
}
