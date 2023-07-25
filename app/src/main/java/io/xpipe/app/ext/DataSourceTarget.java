package io.xpipe.app.ext;

import io.xpipe.app.util.Validator;
import io.xpipe.core.source.DataSource;
import io.xpipe.core.source.DataStoreId;
import io.xpipe.core.util.ModuleLayerLoader;
import javafx.beans.value.ObservableValue;
import javafx.scene.layout.Region;
import lombok.AllArgsConstructor;
import lombok.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;

public interface DataSourceTarget {

    List<DataSourceTarget> ALL = new ArrayList<>();

    class Loader implements ModuleLayerLoader {

        @Override
        public void init(ModuleLayer layer) {
            ALL.clear();
            ALL.addAll(ServiceLoader.load(layer, DataSourceTarget.class).stream()
                    .map(ServiceLoader.Provider::get)
                    .toList());
        }

        @Override
        public boolean requiresFullDaemon() {
            return true;
        }

        @Override
        public boolean prioritizeLoading() {
            return false;
        }
    }

    static Optional<DataSourceTarget> byId(String id) {
        return ALL.stream().filter(d -> d.getId().equals(id)).findAny();
    }

    static List<DataSourceTarget> getAll() {
        return ALL;
    }

    default InstructionsDisplay createRetrievalInstructions(DataSource<?> source, ObservableValue<DataStoreId> id) {
        return null;
    }

    default InstructionsDisplay createUpdateInstructions(DataSource<?> source, ObservableValue<DataStoreId> id) {
        return null;
    }

    String getId();

    ObservableValue<String> getName();

    Category getCategory();

    AccessType getAccessType();

    String getSetupGuideURL();

    default String getGraphicIcon() {
        return null;
    }

    default boolean isApplicable(DataSource<?> source) {
        return true;
    }

    enum Category {
        PROGRAMMING_LANGUAGE,
        APPLICATION,
        OTHER
    }

    enum AccessType {
        ACTIVE,
        PASSIVE
    }

    @Value
    @AllArgsConstructor
    class InstructionsDisplay {
        Region region;
        Runnable onFinish;
        Validator validator;

        public InstructionsDisplay(Region region) {
            this.region = region;
            onFinish = null;
            validator = null;
        }
    }
}
