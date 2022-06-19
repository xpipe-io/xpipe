package io.xpipe.extension;

import javafx.beans.value.ObservableValue;
import javafx.scene.layout.Region;

import java.util.function.Supplier;

public interface SupportedApplicationProvider {

    enum Category {
        PROGRAMMING_LANGUAGE,
        APPLICATION
    }

    Region createRetrieveInstructions(DataSourceProvider<?> provider, ObservableValue<String> id);

    String getId();

    Supplier<String> getName();

    Category getCategory();

    String getSetupGuideURL();

    default String getGraphicIcon() {
        return null;
    }
}
