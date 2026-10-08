package com.afaguayo.fim;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * A baseline is a map of relative file path -> SHA-256 hash, persisted as
 * JSON so it can be diffed against on a later run.
 */
public final class Baseline {

    private final Map<String, String> hashes;

    public Baseline(Map<String, String> hashes) {
        this.hashes = hashes;
    }

    public Map<String, String> hashes() {
        return hashes;
    }

    public static Baseline buildFrom(Path root) throws IOException {
        Map<String, String> hashes = new TreeMap<>();
        try (Stream<Path> walk = Files.walk(root)) {
            for (Path path : walk.filter(Files::isRegularFile).toList()) {
                String relative = root.relativize(path).toString();
                hashes.put(relative, Hasher.sha256(path));
            }
        }
        return new Baseline(hashes);
    }

    public void save(Path out) throws IOException {
        ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        mapper.writeValue(out.toFile(), new TreeMap<>(hashes));
    }

    public static Baseline load(Path in) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, String> hashes = mapper.readValue(in.toFile(), Map.class);
        return new Baseline(new LinkedHashMap<>(hashes));
    }
}
