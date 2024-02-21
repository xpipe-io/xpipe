package io.xpipe.app.util;

import io.xpipe.app.core.AppFileWatcher;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.core.process.OsType;
import io.xpipe.core.util.FailableFunction;
import io.xpipe.core.util.FailableSupplier;
import lombok.Getter;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.time.Instant;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class FileBridge {

    private static final Path TEMP =
            FileUtils.getTempDirectory().toPath().resolve("xpipe").resolve("bridge");
    private static FileBridge INSTANCE;
    private final Set<Entry> openEntries = new HashSet<>();

    public static FileBridge get() {
        return INSTANCE;
    }

    private static void event(String msg) {
        TrackEvent.builder().type("debug").message(msg).handle();
    }

    private static String getFileSystemCompatibleName(String name) {
        if (OsType.getLocal() != OsType.WINDOWS) {
            return name;
        }

        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    public static void init() {
        INSTANCE = new FileBridge();
        try {
            FileUtils.forceMkdir(TEMP.toFile());

            try {
                // Remove old editor files in dir
                FileUtils.cleanDirectory(TEMP.toFile());
            } catch (IOException ignored) {
            }

            AppFileWatcher.getInstance().startWatchersInDirectories(List.of(TEMP), (changed, kind) -> {
                INSTANCE.handleWatchEvent(changed, kind);
            });
        } catch (IOException e) {
            ErrorEvent.fromThrowable(e).handle();
        }
    }

    private synchronized void handleWatchEvent(Path changed, WatchEvent.Kind<Path> kind) {
        if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
            event("Editor entry file " + changed.toString() + " has been removed");
            removeForFile(changed);
            return;
        }

        var entry = getForFile(changed);
        if (entry.isEmpty()) {
            return;
        }

        var e = entry.get();
        // Wait for edit to finish in case external editor has write lock
        if (!Files.exists(changed)) {
            event("File " + TEMP.relativize(e.file) + " is probably still writing ...");
            ThreadHelper.sleep(AppPrefs.get().editorReloadTimeout().getValue());

            // If still no read lock after 500ms, just don't parse it
            if (!Files.exists(changed)) {
                event("Could not obtain read lock even after timeout. Ignoring change ...");
                return;
            }
        }

        try {
            event("Registering modification for file " + TEMP.relativize(e.file));
            event("Last modification for file: " + e.lastModified.toString() + " vs current one: "
                    + e.getLastModified());
            if (e.hasChanged()) {
                event("Registering change for file " + TEMP.relativize(e.file) + " for editor entry " + e.getName());
                e.registerChange();
                try (var in = Files.newInputStream(e.file)) {
                    e.writer.accept(in, (long) in.available());
                }
            }
        } catch (Exception ex) {
            ErrorEvent.fromThrowable(ex).omit().handle();
        }
    }

    private synchronized void removeForFile(Path file) {
        openEntries.removeIf(es -> es.file.equals(file));
    }

    private synchronized Optional<Entry> getForKey(Object node) {
        for (var es : openEntries) {
            if (es.key.equals(node)) {
                return Optional.of(es);
            }
        }
        return Optional.empty();
    }

    private synchronized Optional<Entry> getForFile(Path file) {
        for (var es : openEntries) {
            if (es.file.equals(file)) {
                return Optional.of(es);
            }
        }
        event("No editor entry found for change file " + file.toString());
        return Optional.empty();
    }

    public void openReadOnlyString(String input, Consumer<String> fileConsumer) {
        if (input == null) {
            input = "";
        }

        var id = UUID.randomUUID();
        String s = input;
        openIO(
                id.toString(),
                id,
                () -> new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8)),
                null,
                fileConsumer);
    }

    public void openString(
            String keyName, Object key, String input, Consumer<String> output, Consumer<String> fileConsumer) {
        if (input == null) {
            input = "";
        }

        String s = input;
        openIO(
                keyName,
                key,
                () -> new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8)),
                (size) -> new ByteArrayOutputStream(s.length()) {
                    @Override
                    public void close() throws IOException {
                        super.close();
                        output.accept(new String(toByteArray(), StandardCharsets.UTF_8));
                    }
                },
                fileConsumer);
    }

    public synchronized void openIO(
            String keyName,
            Object key,
            FailableSupplier<InputStream> input,
            FailableFunction<Long, OutputStream, Exception> output,
            Consumer<String> consumer) {
        var ext = getForKey(key);
        if (ext.isPresent()) {
            var existingFile = ext.get().file;
            try {
                try (var out = Files.newOutputStream(existingFile);
                        var in = input.get()) {
                    in.transferTo(out);
                }
            } catch (Exception ex) {
                ErrorEvent.fromThrowable(ex).handle();
                return;
            }
            ext.get().registerChange();
            consumer.accept(existingFile.toString());
            return;
        }

        Path file = TEMP.resolve(UUID.randomUUID().toString().substring(0, 6))
                .resolve(getFileSystemCompatibleName(keyName));
        try {
            FileUtils.forceMkdirParent(file.toFile());
            try (var out = Files.newOutputStream(file);
                    var in = input.get()) {
                in.transferTo(out);
            }
        } catch (Exception ex) {
            ErrorEvent.fromThrowable(ex).handle();
            return;
        }

        var entry = new Entry(file, key, keyName, (in, size) -> {
            if (output != null) {
                try (var out = output.apply(size)) {
                    in.transferTo(out);
                } catch (Exception ex) {
                    ErrorEvent.fromThrowable(ex).handle();
                }
            }
        });
        entry.registerChange();
        openEntries.add(entry);

        ext = getForKey(key);
        consumer.accept(ext.orElseThrow().file.toString());
    }

    @Getter
    public static class Entry {
        private final Path file;
        private final Object key;
        private final String name;
        private final BiConsumer<InputStream, Long> writer;
        private Instant lastModified;

        public Entry(Path file, Object key, String name, BiConsumer<InputStream, Long> writer) {
            this.file = file;
            this.key = key;
            this.name = name;
            this.writer = writer;
        }

        public boolean hasChanged() {
            try {
                var newDate = Files.getLastModifiedTime(file).toInstant();
                return !newDate.equals(lastModified);
            } catch (IOException e) {
                return false;
            }
        }

        public Instant getLastModified() {
            try {
                return Files.getLastModifiedTime(file).toInstant();
            } catch (IOException e) {
                return Instant.EPOCH;
            }
        }

        public void registerChange() {
            lastModified = getLastModified();
        }
    }
}
