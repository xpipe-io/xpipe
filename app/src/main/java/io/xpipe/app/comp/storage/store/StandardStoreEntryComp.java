package io.xpipe.app.comp.storage.store;

import io.xpipe.app.fxcomps.Comp;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.*;

public class StandardStoreEntryComp extends StoreEntryComp {

    public StandardStoreEntryComp(StoreEntryWrapper entry, Comp<?> content) {
        super(entry, content);
    }


    protected Region createContent() {
        var name = createName().createRegion();

        var grid = new GridPane();
        grid.setHgap(7);
        grid.setVgap(0);

        var storeIcon = createIcon(50, 39);
        grid.add(storeIcon, 0, 0, 1, 2);
        grid.getColumnConstraints().add(new ColumnConstraints(50));

        grid.add(name, 1, 0);
        grid.add(createSummary(), 1, 1);
        var nameCC = new ColumnConstraints();
        nameCC.setMinWidth(100);
        nameCC.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(nameCC);

        grid.add(createInformation(), 2, 0, 1, 2);
        var info = new ColumnConstraints();
        info.prefWidthProperty().bind(content != null ? INFO_WITH_CONTENT_WIDTH : INFO_NO_CONTENT_WIDTH);
        info.setHalignment(HPos.LEFT);
        grid.getColumnConstraints().add(info);

        var customSize = content != null ? 300 : 0;
        var custom = new ColumnConstraints(0, customSize, customSize);
        custom.setHalignment(HPos.RIGHT);
        var cr = content != null ? content.createRegion() : new Region();
        var bb = createButtonBar().createRegion();
        var controls = new HBox(cr, bb);
        controls.setFillHeight(true);
        HBox.setHgrow(cr, Priority.ALWAYS);
        controls.setAlignment(Pos.CENTER_RIGHT);
        controls.setSpacing(10);
        controls.setPadding(new Insets(0, 0, 0, 10));
        grid.add(controls, 3, 0, 1, 2);
        grid.getColumnConstraints().add(custom);

        grid.getStyleClass().add("store-entry-grid");

        applyState(grid);

        return grid;
    }
}
