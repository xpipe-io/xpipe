package io.xpipe.app.prefs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.xpipe.app.ext.PrefsChoiceValue;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.util.JsonConfigHelper;
import io.xpipe.core.util.JacksonMapper;
import org.apache.commons.io.FileUtils;

import java.nio.file.Path;

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
            content = JsonConfigHelper.readConfigObject(file);
        }
    }

    private void setContent(String key, JsonNode value) {
        content.set(key, value);
    }

    void save() {
        JsonConfigHelper.writeConfig(file, content);
    }

    public void updateObject(String key, Object object) {
        var tree = object instanceof PrefsChoiceValue prefsChoiceValue
                ? new TextNode(prefsChoiceValue.getId())
                : (object != null ? JacksonMapper.getDefault().valueToTree(object) : NullNode.getInstance());
        setContent(key, tree);
    }

    @SuppressWarnings("unchecked")
    public <T> T loadObject(String id, Class<T> type, T defaultObject) {
        var tree = getContent(id);
        if (tree == null) {
            TrackEvent.withDebug("Preferences value not found").tag("id", id).tag("default", defaultObject).handle();
            return defaultObject;
        }

        if (PrefsChoiceValue.class.isAssignableFrom(type)) {
            var all = getAll(type);
            if (all != null) {
                Class<PrefsChoiceValue> cast = (Class<PrefsChoiceValue>) type;
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
            ErrorEvent.fromThrowable(ex).omit().handle();
            return defaultObject;
        }
    }

    public boolean clear() {
        return FileUtils.deleteQuietly(file.toFile());
    }
}
