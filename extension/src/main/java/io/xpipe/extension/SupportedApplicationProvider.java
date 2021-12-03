package io.xpipe.extension;

import io.xpipe.core.source.DataSourceId;
import javafx.beans.value.ObservableValue;
import javafx.scene.image.Image;
import javafx.scene.layout.Region;

public interface SupportedApplicationProvider {

    Region createTableRetrieveInstructions(ObservableValue<DataSourceId> id);

    Region createStructureRetrieveInstructions(ObservableValue<DataSourceId> id);

    Region createRawRetrieveInstructions(ObservableValue<DataSourceId> id);

    Image getLogo();

    String getId();

    String getName();
}
