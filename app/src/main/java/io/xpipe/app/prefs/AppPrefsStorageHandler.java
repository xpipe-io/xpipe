package io.xpipe.app.prefs;

import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.util.JacksonMapper;

import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.StringNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static io.xpipe.app.prefs.PrefsChoiceValue.getAll;
import static io.xpipe.app.prefs.PrefsChoiceValue.getSupported;

public class AppPrefsStorageHandler {

    private final Path file;
    private ObjectNode content = JsonNodeFactory.instance.objectNode();

    public AppPrefsStorageHandler(Path file) {
        this.file = file;
    }

    private JsonNode getContent(String key) {
        return content.get(key);
    }

    public void load() {
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
                ErrorEventFactory.fromThrowable(e)
                        .expected()
                        .description("Settings file " + file + " is corrupt")
                        .handle();
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
            ErrorEventFactory.fromThrowable(e).expected().handle();
        }
    }

    @SneakyThrows
    public void updateObject(String key, Object object, JavaType type) {
        if (object instanceof PrefsChoiceValue prefsChoiceValue) {
            setContent(key, new StringNode(prefsChoiceValue.getId()));
            return;
        }

        if (object == null) {
            setContent(key, JsonNodeFactory.instance.nullNode());
            return;
        }

        var mapper = JacksonMapper.getDefault();
        setContent(key, mapper.valueToTree(object));
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

        if (tree.isNull()) {
            return null;
        }

        if (PrefsChoiceValue.class.isAssignableFrom(type.getRawClass())) {
            List<T> all = (List<T>) getAll(type.getRawClass());
            if (all != null) {
                Class<PrefsChoiceValue> cast = (Class<PrefsChoiceValue>) type.getRawClass();
                var in = tree.asString();
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
}
