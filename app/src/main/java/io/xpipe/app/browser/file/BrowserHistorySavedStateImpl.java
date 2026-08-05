package io.xpipe.app.browser.file;

import io.xpipe.app.core.AppCache;
import io.xpipe.app.util.JacksonMapper;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import lombok.SneakyThrows;
import lombok.Value;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;

@Value
@JsonDeserialize(using = BrowserHistorySavedStateImpl.Deserializer.class)
public class BrowserHistorySavedStateImpl implements BrowserHistorySavedState {

    private static BrowserHistorySavedStateImpl INSTANCE;

    @JsonSerialize(as = List.class)
    ObservableList<Entry> lastSystems;

    public BrowserHistorySavedStateImpl(List<Entry> lastSystems) {
        this.lastSystems = FXCollections.synchronizedObservableList(FXCollections.observableArrayList(lastSystems));
    }

    public static BrowserHistorySavedState get() {
        if (INSTANCE == null) {
            INSTANCE = load();
        }
        return INSTANCE;
    }

    private static BrowserHistorySavedStateImpl load() {
        return AppCache.getNonNull("browser-state", BrowserHistorySavedStateImpl.class, () -> {
            return new BrowserHistorySavedStateImpl(
                    FXCollections.synchronizedObservableList(FXCollections.observableArrayList()));
        });
    }

    @Override
    public synchronized void add(BrowserHistorySavedState.Entry entry) {
        synchronized (lastSystems) {
            lastSystems.removeIf(e -> e == null || e.getUuid().equals(entry.getUuid()));
            lastSystems.addFirst(entry);
            if (lastSystems.size() > 15) {
                lastSystems.removeLast();
            }
        }
    }

    @Override
    public synchronized void save() {
        AppCache.update("browser-state", this);
    }

    @Override
    public ObservableList<Entry> getEntries() {
        return lastSystems;
    }

    public static class Deserializer extends StdDeserializer<BrowserHistorySavedStateImpl> {

        protected Deserializer() {
            super(BrowserHistorySavedStateImpl.class);
        }

        @Override
        @SneakyThrows
        public BrowserHistorySavedStateImpl deserialize(JsonParser p, DeserializationContext ctxt) {
            var tree = (ObjectNode) JacksonMapper.getDefault().readTree(p);
            JavaType javaType =
                    JacksonMapper.getDefault().getTypeFactory().constructCollectionLikeType(List.class, Entry.class);
            List<Entry> ls = JacksonMapper.getDefault().treeToValue(tree.remove("lastSystems"), javaType);
            if (ls == null) {
                ls = List.of();
            }
            var valid = ls.stream()
                    .filter(entry -> entry.getUuid() != null && entry.getPath() != null)
                    .toList();
            return new BrowserHistorySavedStateImpl(valid);
        }
    }
}
