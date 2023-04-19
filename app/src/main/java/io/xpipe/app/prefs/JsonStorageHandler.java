package io.xpipe.app.prefs;

import com.dlsc.preferencesfx.util.StorageHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.type.CollectionType;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.ext.PrefsChoiceValue;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.util.JsonConfigHelper;
import io.xpipe.core.util.JacksonMapper;
import javafx.collections.ObservableList;
import org.apache.commons.io.FileUtils;

import java.nio.file.Path;
import java.util.List;

import static io.xpipe.app.ext.PrefsChoiceValue.getAll;
import static io.xpipe.app.ext.PrefsChoiceValue.getSupported;

public class JsonStorageHandler implements StorageHandler {

    private final Path file =
            AppProperties.get().getDataDir().resolve("settings").resolve("preferences.json");
    private ObjectNode content;

    private String getSaveId(String bc) {
        return bc.split("#")[bc.split("#").length - 1];
    }

    private JsonNode getContent(String key) {
        if (content == null) {
            content = (ObjectNode) JsonConfigHelper.readConfig(file);
        }
        return content.get(key);
    }

    private void setContent(String key, JsonNode value) {
        content.set(key, value);
    }

    void save() {
        JsonConfigHelper.writeConfig(file, content);
    }

    @Override
    public void saveObject(String breadcrumb, Object object) {
        var id = getSaveId(breadcrumb);
        var tree = object instanceof PrefsChoiceValue prefsChoiceValue
                ? new TextNode(prefsChoiceValue.getId())
                : (object != null ? JacksonMapper.newMapper().valueToTree(object) : NullNode.getInstance());
        setContent(id, tree);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object loadObject(String breadcrumb, Object defaultObject) {
        Class<Object> c = (Class<Object>) AppPrefs.get().getSettingType(breadcrumb);
        return loadObject(breadcrumb, c, defaultObject);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T loadObject(String breadcrumb, Class<T> type, T defaultObject) {
        var id = getSaveId(breadcrumb);
        var tree = getContent(id);
        if (tree == null) {
            TrackEvent.debug("Preferences value not found for key: " + breadcrumb);
            return defaultObject;
        }

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

            TrackEvent.debug("Loading preferences value for key " + breadcrumb + " from value " + found.get());
            return found.get();
        }

        try {
            TrackEvent.debug("Loading preferences value for key " + breadcrumb + " from value " + tree);
            return JacksonMapper.newMapper().treeToValue(tree, type);
        } catch (Exception ex) {
            ErrorEvent.fromThrowable(ex).omit().handle();
            return defaultObject;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public ObservableList<?> loadObservableList(String breadcrumb, ObservableList defaultObservableList) {
        return loadObservableList(breadcrumb, defaultObservableList.get(0).getClass(), defaultObservableList);
    }

    @Override
    public <T> ObservableList<T> loadObservableList(
            String breadcrumb, Class<T> type, ObservableList<T> defaultObservableList) {
        var id = getSaveId(breadcrumb);
        var tree = getContent(id);
        if (tree == null) {
            return defaultObservableList;
        }

        try {
            CollectionType javaType =
                    JacksonMapper.newMapper().getTypeFactory().constructCollectionType(List.class, type);
            return JacksonMapper.newMapper().treeToValue(tree, javaType);
        } catch (Exception ex) {
            ErrorEvent.fromThrowable(ex).omit().handle();
            return defaultObservableList;
        }
    }

    @Override
    public boolean clearPreferences() {
        return FileUtils.deleteQuietly(file.toFile());
    }

    // ======
    // UNUSED
    // ======

    @Override
    public void saveSelectedCategory(String breadcrumb) {
        throw new AssertionError();
    }

    @Override
    public String loadSelectedCategory() {
        throw new AssertionError();
    }

    @Override
    public void saveDividerPosition(double dividerPosition) {}

    @Override
    public double loadDividerPosition() {
        return 0;
    }

    @Override
    public void saveWindowWidth(double windowWidth) {}

    @Override
    public double loadWindowWidth() {
        return 0;
    }

    @Override
    public void saveWindowHeight(double windowHeight) {}

    @Override
    public double loadWindowHeight() {
        return 0;
    }

    @Override
    public void saveWindowPosX(double windowPosX) {}

    @Override
    public double loadWindowPosX() {
        return 0;
    }

    @Override
    public void saveWindowPosY(double windowPosY) {}

    @Override
    public double loadWindowPosY() {
        return 0;
    }
}
