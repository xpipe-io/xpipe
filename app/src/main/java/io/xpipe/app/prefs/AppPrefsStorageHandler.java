package io.xpipe.app.prefs;

import io.xpipe.app.ext.PrefsChoiceValue;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.core.JacksonMapper;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static io.xpipe.app.ext.PrefsChoiceValue.getAll;
import static io.xpipe.app.ext.PrefsChoiceValue.getSupported;

public class AppPrefsStorageHandler {

    private final Path file;
    private ObjectNode content;

    public AppPrefsStorageHandler(Path file) {
        this.file = file;
    }

    boolean isInitialized() {
        return content != null;
    }

    private JsonNode getContent(String key) {
        loadIfNeeded();
        return content.get(key);
    }

    private void loadIfNeeded() {
        if (content == null) {
            if (Files.exists(file)) {
                try {
                    var s = Files.readString(file);
                    if (!s.isEmpty()) {
                        var read = JacksonMapper.getDefault().readTree(s);
                        if (read.isObject()) {
                            content = (ObjectNode) read;
                        }
                    }
                } catch (IOException e) {
                    ErrorEventFactory.fromThrowable(e).handle();
                }
            }

            if (content == null) {
                content = JsonNodeFactory.instance.objectNode();
            }
        }
    }

    private void setContent(String key, JsonNode value) {
        content.set(key, value);
    }

    void save() {
        try {
            FileUtils.forceMkdir(file.getParent().toFile());
            JacksonMapper.getDefault().writeValue(file.toFile(), content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SneakyThrows
    public void updateObject(String key, Object object, JavaType type) {
        if (object instanceof PrefsChoiceValue prefsChoiceValue) {
            setContent(key, new TextNode(prefsChoiceValue.getId()));
            return;
        }

        if (object == null) {
            setContent(key, JsonNodeFactory.instance.nullNode());
            return;
        }

        var mapper = JacksonMapper.getDefault();
        TokenBuffer buf = new TokenBuffer(mapper, false);
        mapper.writerFor(type).writeValue(buf, object);
        var tree = mapper.readTree(buf.asParser());
        setContent(key, (JsonNode) tree);
    }

    @SuppressWarnings("unchecked")
    @SneakyThrows
    public <T> T loadObject(String id, JavaType type, T defaultObject, boolean log) {
        var tree = getContent(id);
        if (tree == null) {
            TrackEvent.withDebug("Preferences value not found")
                    .tag("id", id)
                    .tag("default", defaultObject)
                    .handle();
            return defaultObject;
        }

        if (PrefsChoiceValue.class.isAssignableFrom(type.getRawClass())) {
            List<T> all = (List<T>) getAll(type.getRawClass());
            if (all != null) {
                Class<PrefsChoiceValue> cast = (Class<PrefsChoiceValue>) type.getRawClass();
                var in = tree.asText();
                var found = all.stream()
                        .filter(t -> ((PrefsChoiceValue) t).getId().equalsIgnoreCase(in))
                        .findAny();
                if (found.isEmpty()) {
                    if (log) {
                        TrackEvent.withWarn("Invalid prefs value found")
                                .tag("key", id)
                                .tag("value", in)
                                .handle();
                    }
                    return defaultObject;
                }

                var supported = getSupported(cast);
                if (!supported.contains(found.get())) {
                    if (log) {
                        TrackEvent.withWarn("Unsupported prefs value found")
                                .tag("key", id)
                                .tag("value", in)
                                .handle();
                    }
                    return defaultObject;
                }

                if (log) {
                    TrackEvent.debug("Loading preferences value for key " + id + " from value " + found.get());
                }
                return found.get();
            }
        }

        try {
            if (log) {
                TrackEvent.debug("Loading preferences value for key " + id + " from value " + tree);
            }
            T value = JacksonMapper.getDefault().treeToValue(tree, type);
            if (value instanceof List<?> l) {
                var mod = l.stream().filter(v -> v != null).collect(Collectors.toCollection(ArrayList::new));
                return (T) mod;
            }
            return value;
        } catch (Exception ex) {
            ErrorEventFactory.fromThrowable(ex).expected().omit().handle();
            return defaultObject;
        }
    }

    public boolean clear() {
        return FileUtils.deleteQuietly(file.toFile());
    }
}
