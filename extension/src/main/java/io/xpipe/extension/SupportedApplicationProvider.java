package io.xpipe.extension;

import io.xpipe.core.source.DataSourceId;
import javafx.beans.value.ObservableValue;
import javafx.scene.layout.Region;

import java.util.function.Supplier;

public interface SupportedApplicationProvider {

    enum Category {
        PROGRAMMING_LANGUAGE,
        APPLICATION
    }

    Region createTableRetrieveInstructions(ObservableValue<DataSourceId> id);

    Region createStructureRetrieveInstructions(ObservableValue<DataSourceId> id);

    Region createTextRetrieveInstructions(ObservableValue<DataSourceId> id);

    Region createRawRetrieveInstructions(ObservableValue<DataSourceId> id);

    String getId();

    Supplier<String> getName();

    Category getCategory();

    String getSetupGuideURL();

    default String getGraphicIcon() {
        return null;
    }
}
