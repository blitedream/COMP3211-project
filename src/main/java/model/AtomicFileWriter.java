package model;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

/** Internal persistence boundary: never truncate the user's destination in place. */
final class AtomicFileWriter {
    private AtomicFileWriter() { }

    @FunctionalInterface
    interface Writer { void write(Path path, byte[] bytes) throws IOException; }

    @FunctionalInterface
    interface Replacer { void replace(Path staged, Path target) throws IOException; }

    static void write(Path target, byte[] bytes) throws IOException {
        write(target, bytes, AtomicFileWriter::writeAndFlush,
                (staged, destination) -> Files.move(staged, destination,
                        StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING));
    }

    // Package-private collaborators permit deterministic disk-failure tests without
    // exhausting disk space or relying on platform-specific permissions/locks.
    static void write(Path target, byte[] bytes, Writer writer, Replacer replacer)
            throws IOException {
        Path destination = target.toAbsolutePath();
        Path staged = Files.createTempFile(destination.getParent(), ".pim-save-", ".tmp");
        try {
            writer.write(staged, bytes);
            replacer.replace(staged, destination);
        } catch (IOException | RuntimeException | Error failure) {
            try {
                Files.deleteIfExists(staged);
            } catch (IOException cleanupFailure) {
                failure.addSuppressed(cleanupFailure);
            }
            throw failure;
        }
    }

    private static void writeAndFlush(Path path, byte[] bytes) throws IOException {
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING)) {
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            while (buffer.hasRemaining()) channel.write(buffer);
            channel.force(true);
        }
    }
}
