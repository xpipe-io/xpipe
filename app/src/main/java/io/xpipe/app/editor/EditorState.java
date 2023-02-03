package io.xpipe.app.editor;

import io.xpipe.app.core.FileWatchManager;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.core.charsetter.Charsetter;
import io.xpipe.extension.event.ErrorEvent;
import io.xpipe.extension.event.TrackEvent;
import io.xpipe.extension.util.ThreadHelper;
import lombok.Getter;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;

public class EditorState {

    private static final Path TEMP =
            FileUtils.getTempDirectory().toPath().resolve("xpipe").resolve("editor");
    private static EditorState INSTANCE;
    private final Set<Entry> openEntries = new CopyOnWriteArraySet<>();

    public static EditorState get() {
        return INSTANCE;
    }

    private static TrackEvent.TrackEventBuilder event() {
        return TrackEvent.builder().category("editor").type("debug");
    }

    private static void event(String msg) {
        TrackEvent.builder().category("editor").type("debug").message(msg).handle();
    }

    public static void init() {
        INSTANCE = new EditorState();
        try {
            FileUtils.forceMkdir(TEMP.toFile());

            try {
                // Remove old editor files in dir
                FileUtils.cleanDirectory(TEMP.toFile());
            } catch (IOException ignored) {
            }

            FileWatchManager.getInstance().startWatchersInDirectories(List.of(TEMP), (changed, kind) -> {
                if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                    event("Editor entry file " + changed.toString() + " has been removed");
                    INSTANCE.removeForFile(changed);
                } else {
                    INSTANCE.getForFile(changed).ifPresent(e -> {
                        // Wait for edit to finish in case external editor has write lock
                        if (!Files.exists(changed)) {
                            event("File " + TEMP.relativize(e.file) + " is probably still writing ...");
                            ThreadHelper.sleep(
                                    AppPrefs.get().editorReloadTimeout().getValue());

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
                                event("Registering change for file " + TEMP.relativize(e.file) + " for editor node "
                                        + e.getName());
                                boolean valid =
                                        get().openEntries.stream().anyMatch(entry -> entry.file.equals(changed));
                                event("Editor node " + e.getName() + " validity: " + valid);
                                if (valid) {
                                    e.registerChange();
                                    try (var in = Files.newInputStream(e.file)) {
                                        e.writer.accept(in);
                                    }
                                }
                            }
                        } catch (Exception ex) {
                            ErrorEvent.fromThrowable(ex).omit().handle();
                        }
                    });
                }
            });
        } catch (IOException e) {
            ErrorEvent.fromThrowable(e).handle();
        }
    }

    private void removeForFile(Path file) {
        openEntries.removeIf(es -> es.file.equals(file));
    }

    private Optional<Entry> getForKey(Object node) {
        for (var es : openEntries) {
            if (es.key.equals(node)) {
                return Optional.of(es);
            }
        }
        return Optional.empty();
    }

    private Optional<Entry> getForFile(Path file) {
        for (var es : openEntries) {
            if (es.file.equals(file)) {
                return Optional.of(es);
            }
        }
        event("No editor entry found for change file " + file.toString());
        return Optional.empty();
    }

    public void startEditing(
            String keyName,
            Object key,
            String input,
            Consumer<String> output) {
        if (input == null) {
            input = "";
        }

        String s = input;
        startEditing(keyName, key, () -> new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8)), () -> new ByteArrayOutputStream(s.length()) {
            @Override
            public void close() throws IOException {
                super.close();
                output.accept(new String(toByteArray(), StandardCharsets.UTF_8));
            }
        });
    }

    public void startEditing(
            String keyName,
            Object key,
            Charsetter.FailableSupplier<InputStream, Exception> input,
            Charsetter.FailableSupplier<OutputStream, Exception> output) {
        var ext = getForKey(key);
        if (ext.isPresent()) {
            openInEditor(ext.get().file.toString());
            return;
        }

        var name = keyName + " - " + UUID.randomUUID().toString().substring(0, 6) + ".txt";
        Path file = TEMP.resolve(name);
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

        var entry = new Entry(file, key, keyName, in -> {
            try (var out = output.get()) {
                in.transferTo(out);
            } catch (Exception ex) {
                ErrorEvent.fromThrowable(ex).handle();
            }
        });
        entry.registerChange();
        openEntries.add(entry);

        ext = getForKey(key);
        openInEditor(ext.orElseThrow().file.toString());
    }

    public void openInEditor(String file) {
        var editor = AppPrefs.get().externalEditor().getValue();
        if (editor == null || !editor.isSelectable()) {
            return;
        }

        try {
            editor.launch(Path.of(file).toRealPath());
        } catch (Exception e) {
            ErrorEvent.fromThrowable(e).handle();
        }
    }

    @Getter
    public static class Entry {
        private final Path file;
        private final Object key;
        private final String name;
        private final Consumer<InputStream> writer;
        private Instant lastModified;

        public Entry(Path file, Object key, String name, Consumer<InputStream> writer) {
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
