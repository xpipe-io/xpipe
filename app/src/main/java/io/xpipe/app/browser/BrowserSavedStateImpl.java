package io.xpipe.app.browser;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.xpipe.app.core.AppCache;
import io.xpipe.core.util.JacksonMapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.SneakyThrows;
import lombok.Value;

import java.util.List;

@Value
@JsonDeserialize(using = BrowserSavedStateImpl.Deserializer.class)
public class BrowserSavedStateImpl implements BrowserSavedState {

    static BrowserSavedStateImpl load() {
        return AppCache.get("browser-state", BrowserSavedStateImpl.class, () -> {
            return new BrowserSavedStateImpl(FXCollections.observableArrayList());
        });
    }

    @JsonSerialize(as = List.class)
    ObservableList<Entry> lastSystems;

    public BrowserSavedStateImpl(List<Entry> lastSystems) {
        this.lastSystems = FXCollections.observableArrayList(lastSystems);
    }

    public static class Deserializer extends StdDeserializer<BrowserSavedStateImpl> {

        protected Deserializer() {
            super(BrowserSavedStateImpl.class);
        }

        @Override
        @SneakyThrows
        public BrowserSavedStateImpl deserialize(JsonParser p, DeserializationContext ctxt) {
            var tree = (ObjectNode) JacksonMapper.getDefault().readTree(p);
            JavaType javaType = JacksonMapper.getDefault()
                    .getTypeFactory()
                    .constructCollectionLikeType(List.class, Entry.class);
            List<Entry> ls = JacksonMapper.getDefault().treeToValue(tree.remove("lastSystems"), javaType);
            if (ls == null) {
                ls = List.of();
            }
            return new BrowserSavedStateImpl(ls);
        }
    }

    @Override
    public synchronized void add(BrowserSavedState.Entry entry) {
        lastSystems.removeIf(s -> s.getUuid().equals(entry.getUuid()));
        lastSystems.addFirst(entry);
        if (lastSystems.size() > 10) {
            lastSystems.removeLast();
        }
    }

    @Override
    public void save() {
        AppCache.update("browser-state", this);
    }

    @Override
    public ObservableList<Entry> getEntries() {
        return lastSystems;
    }
}
