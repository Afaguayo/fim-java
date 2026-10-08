package com.afaguayo.fim;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HasherTest {

    @Test
    void knownStringProducesKnownSha256(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("hello.txt");
        Files.writeString(file, "hello world");
        // Precomputed: sha256("hello world")
        String expected = "b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9";
        assertEquals(expected, Hasher.sha256(file));
    }

    @Test
    void differentContentProducesDifferentHash(@TempDir Path tempDir) throws Exception {
        Path a = tempDir.resolve("a.txt");
        Path b = tempDir.resolve("b.txt");
        Files.writeString(a, "content A");
        Files.writeString(b, "content B");
        assertEquals(false, Hasher.sha256(a).equals(Hasher.sha256(b)));
    }
}
