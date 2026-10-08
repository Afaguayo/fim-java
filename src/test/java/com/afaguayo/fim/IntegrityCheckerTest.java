package com.afaguayo.fim;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class IntegrityCheckerTest {

    @Test
    void cleanWhenNothingChanged(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("a.txt"), "hello");
        Baseline before = Baseline.buildFrom(dir);
        Baseline after = Baseline.buildFrom(dir);

        DiffResult diff = new IntegrityChecker().diff(before, after);
        assertTrue(diff.isClean());
    }

    @Test
    void detectsAddedFile(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("a.txt"), "hello");
        Baseline before = Baseline.buildFrom(dir);

        Files.writeString(dir.resolve("b.txt"), "new file");
        Baseline after = Baseline.buildFrom(dir);

        DiffResult diff = new IntegrityChecker().diff(before, after);
        assertEquals(1, diff.added().size());
        assertEquals("b.txt", diff.added().get(0));
        assertTrue(diff.removed().isEmpty());
        assertTrue(diff.modified().isEmpty());
    }

    @Test
    void detectsRemovedFile(@TempDir Path dir) throws Exception {
        Path a = dir.resolve("a.txt");
        Files.writeString(a, "hello");
        Baseline before = Baseline.buildFrom(dir);

        Files.delete(a);
        Baseline after = Baseline.buildFrom(dir);

        DiffResult diff = new IntegrityChecker().diff(before, after);
        assertEquals(1, diff.removed().size());
        assertEquals("a.txt", diff.removed().get(0));
    }

    @Test
    void detectsModifiedFile(@TempDir Path dir) throws Exception {
        Path a = dir.resolve("a.txt");
        Files.writeString(a, "original content");
        Baseline before = Baseline.buildFrom(dir);

        Files.writeString(a, "tampered content");
        Baseline after = Baseline.buildFrom(dir);

        DiffResult diff = new IntegrityChecker().diff(before, after);
        assertEquals(1, diff.modified().size());
        assertEquals("a.txt", diff.modified().get(0).path());
        assertNotEquals(diff.modified().get(0).oldHash(), diff.modified().get(0).newHash());
    }

    @Test
    void baselineRoundTripsThroughJson(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("a.txt"), "hello");
        Baseline original = Baseline.buildFrom(dir);

        Path jsonFile = dir.resolve("baseline.json");
        original.save(jsonFile);
        Baseline loaded = Baseline.load(jsonFile);

        assertEquals(original.hashes().get("a.txt"), loaded.hashes().get("a.txt"));
    }
}
