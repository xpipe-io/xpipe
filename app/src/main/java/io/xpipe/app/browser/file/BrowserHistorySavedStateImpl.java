package io.xpipe.app.browser.file;

import io.xpipe.app.core.AppCache;
import io.xpipe.core.util.JacksonMapper;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.SneakyThrows;
import lombok.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Value
@JsonDeserialize(using = BrowserHistorySavedStateImpl.Deserializer.class)
public class BrowserHistorySavedStateImpl implements BrowserHistorySavedState {

    @JsonSerialize(as = List.class)
    ObservableList<Entry> lastSystems;

    public BrowserHistorySavedStateImpl(List<Entry> lastSystems) {
        this.lastSystems = FXCollections.synchronizedObservableList(FXCollections.observableArrayList(lastSystems));
    }

    private static BrowserHistorySavedStateImpl INSTANCE;

    public static BrowserHistorySavedState get() {
        if (INSTANCE == null) {
            INSTANCE = load();
        }
        return INSTANCE;
    }

    private static BrowserHistorySavedStateImpl load() {
        return AppCache.getNonNull("browser-state", BrowserHistorySavedStateImpl.class, () -> {
            return new BrowserHistorySavedStateImpl(FXCollections.synchronizedObservableList(FXCollections.observableArrayList()));
        });
    }

    @Override
    public synchronized void add(BrowserHistorySavedState.Entry entry) {
        var copy = new ArrayList<>(lastSystems);
        for (Entry e : copy) {
            if (e.getUuid().equals(entry.getUuid())) {
                lastSystems.remove(e);
            }
        }
        lastSystems.addFirst(entry);
        if (lastSystems.size() > 15) {
            lastSystems.removeLast();
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
            return new BrowserHistorySavedStateImpl(ls);
        }
    }
}
