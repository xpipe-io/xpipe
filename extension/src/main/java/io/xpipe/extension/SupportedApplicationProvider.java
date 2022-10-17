package io.xpipe.extension;

import io.xpipe.core.source.DataSource;
import io.xpipe.extension.util.Validator;
import javafx.beans.value.ObservableValue;
import javafx.scene.layout.Region;
import lombok.AllArgsConstructor;
import lombok.Value;

public interface SupportedApplicationProvider {

    default InstructionsDisplay createRetrievalInstructions(DataSource<?> source, ObservableValue<String> id) {
        return null;
    }

    default InstructionsDisplay createUpdateInstructions(DataSource<?> source, ObservableValue<String> id) {
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

    enum Direction {
        RETRIEVE,
        UPDATE
    }

    @Value
    @AllArgsConstructor
    public static class InstructionsDisplay {
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
