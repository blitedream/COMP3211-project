package model;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Stream;

/** FR-11: disk failures must not destroy the previous saved collection. */
public final class AtomicSaveTest {

    /** A real overwrite commits the new complete collection and removes staging files. */
    @Test
    public void testSuccessfulReplacement() throws Exception {
        withDestination((dir, file) -> {
            PIMModel model = new PIMModel();
            model.createNote("new complete data");
            PIMFileCodec.save(model, file);
            check(PIMFileCodec.load(file).listRecords().get(0).toDetailString()
                    .equals(model.listRecords().get(0).toDetailString()), "new complete record");
            onlyDestination(dir);
        });
    }

    /** A partial staged write fails before replacement; old bytes remain identical. */
    @Test
    public void testPartialWriteFailurePreservesOriginal() throws Exception {
        withDestination((dir, file) -> {
            byte[] old = Files.readAllBytes(file);
            expectIOException(() -> AtomicFileWriter.write(file, new byte[]{1, 2, 3},
                    (staged, bytes) -> { Files.write(staged, new byte[]{1}); throw new IOException("disk full"); },
                    (staged, target) -> { throw new AssertionError("replacement must not run"); }));
            check(Arrays.equals(old, Files.readAllBytes(file)), "old bytes survived partial write");
            onlyDestination(dir);
        });
    }

    /** Failure to commit a completed stage leaves the original bytes intact. */
    @Test
    public void testReplacementFailurePreservesOriginal() throws Exception {
        withDestination((dir, file) -> {
            byte[] old = Files.readAllBytes(file);
            expectIOException(() -> AtomicFileWriter.write(file, new byte[]{1, 2},
                    (staged, bytes) -> Files.write(staged, bytes),
                    (staged, target) -> { throw new IOException("destination unavailable"); }));
            check(Arrays.equals(old, Files.readAllBytes(file)), "old bytes survived failed replacement");
            onlyDestination(dir);
        });
    }

    /** Unsupported atomic moves are reported; there is no destructive fallback. */
    @Test
    public void testUnsupportedAtomicMovePreservesOriginal() throws Exception {
        withDestination((dir, file) -> {
            byte[] old = Files.readAllBytes(file);
            expectIOException(() -> AtomicFileWriter.write(file, new byte[]{1},
                    (staged, bytes) -> Files.write(staged, bytes),
                    (staged, target) -> { throw new AtomicMoveNotSupportedException(
                            staged.toString(), target.toString(), "not supported"); }));
            check(Arrays.equals(old, Files.readAllBytes(file)), "unsupported provider preserves original");
            onlyDestination(dir);
        });
    }

    /** Saving an empty collection replaces the previous file with a valid empty file. */
    @Test
    public void testSaveEmptyCollection() throws Exception {
        withDestination((dir, file) -> {
            PIMFileCodec.save(new PIMModel(), file);
            check(Files.size(file) == 0 && PIMFileCodec.load(file).listRecords().isEmpty(), "empty round trip");
            onlyDestination(dir);
        });
    }

    /** A nonempty directory cannot be replaced by a saved file; its child survives. */
    @Test
    public void testDirectoryDestinationIsPreserved() throws Exception {
        Path dir = Files.createTempDirectory("pim-directory-test");
        Path target = Files.createDirectory(dir.resolve("data.pim"));
        Path child = target.resolve("keep.txt");
        Files.write(child, new byte[]{7});
        try {
            expectIOException(() -> PIMFileCodec.save(new PIMModel(), target));
            check(Arrays.equals(Files.readAllBytes(child), new byte[]{7}), "child unchanged");
            onlyDestination(dir);
        } finally { Files.delete(child); Files.delete(target); Files.delete(dir); }
    }

    private interface Action { void run() throws Exception; }
    private interface FileCase { void run(Path dir, Path file) throws Exception; }
    private static void withDestination(FileCase test) throws Exception {
        Path dir = Files.createTempDirectory("pim-atomic-test");
        Path file = dir.resolve("data.pim");
        Files.write(file, "type=NOTE\nid=original\ntext=b2xkIGRhdGE=\n".getBytes(StandardCharsets.UTF_8));
        try { test.run(dir, file); }
        finally {
            try (Stream<Path> paths = Files.list(dir)) {
                for (Path path : (Iterable<Path>) paths::iterator) Files.deleteIfExists(path);
            }
            Files.delete(dir);
        }
    }
    private static void onlyDestination(Path dir) throws IOException {
        try (Stream<Path> paths = Files.list(dir)) { check(paths.count() == 1, "no abandoned stage"); }
    }
    private static void expectIOException(Action action) throws Exception {
        try { action.run(); } catch (IOException expected) { return; }
        throw new AssertionError("Expected IOException");
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
