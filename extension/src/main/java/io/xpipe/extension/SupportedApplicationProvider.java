package io.xpipe.extension;

import io.xpipe.core.source.DataSourceId;
import javafx.beans.value.ObservableValue;
import javafx.scene.image.Image;
import javafx.scene.layout.Region;

public interface SupportedApplicationProvider {

    Region createRetrieveInstructions(ObservableValue<DataSourceId> id);

    Image getLogo();

    String getId();

    String getName();
}
