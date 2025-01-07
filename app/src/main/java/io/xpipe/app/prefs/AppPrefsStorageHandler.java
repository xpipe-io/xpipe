package io.xpipe.app.prefs;

import com.fasterxml.jackson.databind.JavaType;
import io.xpipe.app.ext.PrefsChoiceValue;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.core.util.JacksonMapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

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
                    ObjectMapper o = JacksonMapper.getDefault();
                    var read = o.readTree(Files.readAllBytes(file));
                    content = read.isObject() ? (ObjectNode) read : null;
                } catch (IOException e) {
                    ErrorEvent.fromThrowable(e).handle();
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

    public void updateObject(String key, Object object) {
        var tree = object instanceof PrefsChoiceValue prefsChoiceValue
                ? new TextNode(prefsChoiceValue.getId())
                : (object != null ? JacksonMapper.getDefault().valueToTree(object) : NullNode.getInstance());
        setContent(key, tree);
    }

    @SuppressWarnings("unchecked")
    public <T> T loadObject(String id, JavaType type, T defaultObject) {
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
                    TrackEvent.withWarn("Invalid prefs value found")
                            .tag("key", id)
                            .tag("value", in)
                            .handle();
                    return defaultObject;
                }

                var supported = getSupported(cast);
                if (!supported.contains(found.get())) {
                    TrackEvent.withWarn("Unsupported prefs value found")
                            .tag("key", id)
                            .tag("value", in)
                            .handle();
                    return defaultObject;
                }

                TrackEvent.debug("Loading preferences value for key " + id + " from value " + found.get());
                return found.get();
            }
        }

        try {
            TrackEvent.debug("Loading preferences value for key " + id + " from value " + tree);
            return JacksonMapper.getDefault().treeToValue(tree, type);
        } catch (Exception ex) {
            ErrorEvent.fromThrowable(ex).expected().omit().handle();
            return defaultObject;
        }
    }

    public boolean clear() {
        return FileUtils.deleteQuietly(file.toFile());
    }
}
