package io.xpipe.app.browser.file;

import io.xpipe.app.core.AppCache;
import io.xpipe.app.util.GlobalTimer;
import io.xpipe.core.FilePath;
import io.xpipe.core.JacksonMapper;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@AllArgsConstructor
@Getter
@JsonSerialize(using = BrowserFileSystemSavedState.Serializer.class)
@JsonDeserialize(using = BrowserFileSystemSavedState.Deserializer.class)
public class BrowserFileSystemSavedState {

    private static final int STORED = 15;

    @Setter
    private BrowserFileSystemTabModel model;

    private FilePath lastDirectory;

    @NonNull
    private ObservableList<RecentEntry> recentDirectories;

    public BrowserFileSystemSavedState(FilePath lastDirectory, @NonNull ObservableList<RecentEntry> recentDirectories) {
        this.lastDirectory = lastDirectory;
        this.recentDirectories = recentDirectories;
    }

    public BrowserFileSystemSavedState() {
        lastDirectory = null;
        recentDirectories = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());
    }

    static BrowserFileSystemSavedState loadForStore(BrowserFileSystemTabModel model) {
        var state = AppCache.getNonNull(
                "fs-state-" + model.getEntry().get().getUuid(), BrowserFileSystemSavedState.class, () -> {
                    return new BrowserFileSystemSavedState();
                });
        state.setModel(model);
        return state;
    }

    public synchronized void save() {
        if (model == null) {
            return;
        }

        AppCache.update("fs-state-" + model.getEntry().get().getUuid(), this);
    }

    public void cd(FilePath dir, boolean delay) {
        if (dir == null) {
            lastDirectory = null;
            return;
        }

        lastDirectory = dir;

        if (delay) {
            // After 10 seconds
            GlobalTimer.delayAsync(
                    new Runnable() {
                        @Override
                        public void run() {
                            // Synchronize with platform thread
                            Platform.runLater(() -> {
                                if (Objects.equals(lastDirectory, dir)) {
                                    updateRecent(dir);
                                    save();
                                }
                            });
                        }
                    },
                    Duration.ofMillis(10000));
        } else {
            updateRecent(dir);
            save();
        }
    }

    private synchronized void updateRecent(FilePath dir) {
        var without = dir.removeTrailingSlash();
        var with = dir.toDirectory();
        var copy = new ArrayList<>(recentDirectories);
        for (RecentEntry recentEntry : copy) {
            if (Objects.equals(recentEntry.directory, without) || Objects.equals(recentEntry.directory, with)) {
                recentDirectories.remove(recentEntry);
            }
        }

        var o = new RecentEntry(with, Instant.now());
        if (recentDirectories.size() < STORED) {
            recentDirectories.addFirst(o);
        } else {
            recentDirectories.removeLast();
            recentDirectories.addFirst(o);
        }
    }

    public static class Serializer extends StdSerializer<BrowserFileSystemSavedState> {

        protected Serializer() {
            super(BrowserFileSystemSavedState.class);
        }

        @Override
        public void serialize(BrowserFileSystemSavedState value, JsonGenerator gen, SerializerProvider provider)
                throws IOException {
            var node = JsonNodeFactory.instance.objectNode();
            node.set("recentDirectories", JacksonMapper.getDefault().valueToTree(value.getRecentDirectories()));
            gen.writeTree(node);
        }
    }

    public static class Deserializer extends StdDeserializer<BrowserFileSystemSavedState> {

        protected Deserializer() {
            super(BrowserFileSystemSavedState.class);
        }

        private static <T> Predicate<T> distinctBy(Function<? super T, ?> f) {
            Set<Object> objects = new HashSet<>();
            return t -> objects.add(f.apply(t));
        }

        @Override
        @SneakyThrows
        public BrowserFileSystemSavedState deserialize(JsonParser p, DeserializationContext ctxt) {
            var tree = (ObjectNode) JacksonMapper.getDefault().readTree(p);
            JavaType javaType = JacksonMapper.getDefault()
                    .getTypeFactory()
                    .constructCollectionLikeType(List.class, RecentEntry.class);
            List<RecentEntry> recentDirectories =
                    JacksonMapper.getDefault().treeToValue(tree.remove("recentDirectories"), javaType);
            if (recentDirectories == null) {
                recentDirectories = List.of();
            }
            var cleaned = recentDirectories.stream()
                    .map(recentEntry -> new RecentEntry(recentEntry.directory.toDirectory(), recentEntry.time))
                    .filter(distinctBy(recentEntry -> recentEntry.getDirectory()))
                    .collect(Collectors.toCollection(CopyOnWriteArrayList::new));
            return new BrowserFileSystemSavedState(null, FXCollections.observableList(cleaned));
        }
    }

    @Value
    @Jacksonized
    @Builder
    public static class RecentEntry {

        FilePath directory;
        Instant time;
    }
}
