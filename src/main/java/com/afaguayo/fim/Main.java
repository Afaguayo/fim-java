package com.afaguayo.fim;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;

/**
 * CLI entry point.
 *
 * <pre>
 *   java -jar fim-java.jar baseline --dir ./app --out baseline.json
 *   java -jar fim-java.jar check    --dir ./app --baseline baseline.json [--json]
 *   java -jar fim-java.jar watch    --dir ./app --baseline baseline.json [--interval-seconds 30]
 * </pre>
 */
public final class Main {

    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length == 0) {
            printHelp();
            System.exit(1);
            return;
        }

        String command = args[0];
        String dir = null, baselinePath = null, out = null;
        boolean json = false;
        int intervalSeconds = 30;

        for (int i = 1; i < args.length; i++) {
            switch (args[i]) {
                case "--dir" -> dir = args[++i];
                case "--baseline" -> baselinePath = args[++i];
                case "--out" -> out = args[++i];
                case "--json" -> json = true;
                case "--interval-seconds" -> intervalSeconds = Integer.parseInt(args[++i]);
                case "--help", "-h" -> {
                    printHelp();
                    return;
                }
                default -> {
                    System.err.println("Unknown argument: " + args[i]);
                    System.exit(1);
                }
            }
        }

        switch (command) {
            case "baseline" -> {
                requireNonNull(dir, "--dir");
                Path outPath = Path.of(out != null ? out : "baseline.json");
                Baseline baseline = Baseline.buildFrom(Path.of(dir));
                baseline.save(outPath);
                System.out.printf("Baseline captured: %d files -> %s%n", baseline.hashes().size(), outPath);
            }
            case "check" -> {
                requireNonNull(dir, "--dir");
                requireNonNull(baselinePath, "--baseline");
                Baseline previous = Baseline.load(Path.of(baselinePath));
                Baseline current = Baseline.buildFrom(Path.of(dir));
                DiffResult diff = new IntegrityChecker().diff(previous, current);
                printDiff(diff, json);
                System.exit(diff.isClean() ? 0 : 2);
            }
            case "watch" -> {
                requireNonNull(dir, "--dir");
                requireNonNull(baselinePath, "--baseline");
                watch(Path.of(dir), Path.of(baselinePath), intervalSeconds);
            }
            default -> {
                System.err.println("Unknown command: " + command);
                printHelp();
                System.exit(1);
            }
        }
    }

    private static void watch(Path dir, Path baselinePath, int intervalSeconds) throws IOException, InterruptedException {
        Baseline baseline = Baseline.load(baselinePath);
        System.out.println("Watching " + dir + " every " + intervalSeconds + "s (Ctrl+C to stop)...");
        IntegrityChecker checker = new IntegrityChecker();
        while (true) {
            Thread.sleep(intervalSeconds * 1000L);
            Baseline current = Baseline.buildFrom(dir);
            DiffResult diff = checker.diff(baseline, current);
            if (!diff.isClean()) {
                System.out.println("\n[" + LocalDateTime.now() + "] Change detected:");
                printDiff(diff, false);
            }
        }
    }

    private static void printDiff(DiffResult diff, boolean json) {
        if (json) {
            StringBuilder sb = new StringBuilder("{\n");
            sb.append("  \"added\": ").append(jsonArray(diff.added())).append(",\n");
            sb.append("  \"removed\": ").append(jsonArray(diff.removed())).append(",\n");
            sb.append("  \"modified\": [\n");
            for (int i = 0; i < diff.modified().size(); i++) {
                var m = diff.modified().get(i);
                sb.append("    {\"path\": \"").append(m.path())
                        .append("\", \"oldHash\": \"").append(m.oldHash())
                        .append("\", \"newHash\": \"").append(m.newHash()).append("\"}");
                sb.append(i < diff.modified().size() - 1 ? ",\n" : "\n");
            }
            sb.append("  ]\n}");
            System.out.println(sb);
            return;
        }

        if (diff.isClean()) {
            System.out.println("No changes detected. Integrity OK.");
            return;
        }
        System.out.println("Integrity check: " + diff.totalChanges() + " change(s) detected");
        diff.added().forEach(p -> System.out.println("  + added:    " + p));
        diff.removed().forEach(p -> System.out.println("  - removed:  " + p));
        diff.modified().forEach(m -> System.out.println("  ~ modified: " + m.path()
                + " (" + m.oldHash().substring(0, 12) + "... -> " + m.newHash().substring(0, 12) + "...)"));
    }

    private static String jsonArray(java.util.List<String> items) {
        return "[" + String.join(", ", items.stream().map(s -> "\"" + s + "\"").toArray(String[]::new)) + "]";
    }

    private static void requireNonNull(String value, String flag) {
        if (value == null) {
            System.err.println("Error: " + flag + " is required for this command.");
            System.exit(1);
        }
    }

    private static void printHelp() {
        System.out.println("""
                fim-java - file integrity monitor (SHA-256 baseline + diff)

                Usage:
                  java -jar fim-java.jar baseline --dir <path> [--out baseline.json]
                  java -jar fim-java.jar check    --dir <path> --baseline <file> [--json]
                  java -jar fim-java.jar watch    --dir <path> --baseline <file> [--interval-seconds 30]

                Exit codes (check): 0 clean, 2 changes detected, 1 usage error.
                """);
    }
}
