package com.afaguayo.fim;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Diffs a freshly computed baseline against a previously stored one. */
public final class IntegrityChecker {

    public DiffResult diff(Baseline previous, Baseline current) {
        List<String> added = new ArrayList<>();
        List<String> removed = new ArrayList<>();
        List<DiffResult.Modified> modified = new ArrayList<>();

        Map<String, String> oldHashes = previous.hashes();
        Map<String, String> newHashes = current.hashes();

        for (var entry : newHashes.entrySet()) {
            String path = entry.getKey();
            String newHash = entry.getValue();
            if (!oldHashes.containsKey(path)) {
                added.add(path);
            } else if (!oldHashes.get(path).equals(newHash)) {
                modified.add(new DiffResult.Modified(path, oldHashes.get(path), newHash));
            }
        }
        for (String path : oldHashes.keySet()) {
            if (!newHashes.containsKey(path)) {
                removed.add(path);
            }
        }

        added.sort(String::compareTo);
        removed.sort(String::compareTo);
        modified.sort((a, b) -> a.path().compareTo(b.path()));

        return new DiffResult(added, removed, modified);
    }
}
