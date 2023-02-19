package io.xpipe.app.comp.storage;

import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.core.source.DataSource;
import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.kordamp.ikonli.javafx.FontIcon;

@Value
@EqualsAndHashCode(callSuper = true)
public class DataStoreTypeComp extends SimpleComp {

    private final DataSource<?> source;

    @Override
    protected Region createSimple() {
        var icon = new FontIcon("mdoal-insert_drive_file");
        var sp = new StackPane(icon);
        sp.setAlignment(Pos.CENTER);
        icon.iconSizeProperty().bind(Bindings.divide(sp.heightProperty(), 1));
        sp.getStyleClass().add("data-store-type-comp");
        return sp;
    }
}
