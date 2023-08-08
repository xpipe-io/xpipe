package io.xpipe.app.comp.storage.store;

import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.augment.GrowAugment;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.*;

public class DenseStoreEntryComp extends StoreEntryComp {

    private final boolean showIcon;

    public DenseStoreEntryComp(StoreEntryWrapper entry, boolean showIcon, Comp<?> content) {
        super(entry, content);
        this.showIcon = showIcon;
    }

    protected Region createContent() {
        var name = createName().createRegion();

        var grid = new GridPane();
        grid.setHgap(8);

        if (showIcon) {
            var storeIcon = createIcon(26, 21);
            grid.getColumnConstraints().add(new ColumnConstraints(26));
            grid.add(storeIcon, 0, 0);
            GridPane.setHalignment(storeIcon, HPos.CENTER);
        }

        var customSize = content != null ? 300 : 0;
        var custom = new ColumnConstraints(0, customSize, customSize);
        custom.setHalignment(HPos.RIGHT);

        var info = new ColumnConstraints();
        info.prefWidthProperty().bind(content != null ? INFO_WITH_CONTENT_WIDTH : INFO_NO_CONTENT_WIDTH);
        info.setHalignment(HPos.LEFT);

        var nameCC = new ColumnConstraints();
        nameCC.setMinWidth(100);
        nameCC.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(nameCC);
        grid.addRow(0, name);

        grid.addRow(0, createInformation());
        grid.getColumnConstraints().addAll(info, custom);

        var cr = content != null ? content.createRegion() : new Region();
        var bb = createButtonBar().createRegion();
        var controls = new HBox(cr, bb);
        controls.setFillHeight(true);
        controls.setAlignment(Pos.CENTER_RIGHT);
        controls.setSpacing(10);
        controls.setPadding(new Insets(0, 0, 0, 10));
        HBox.setHgrow(cr, Priority.ALWAYS);
        grid.addRow(0, controls);

        GrowAugment.create(true, false).augment(grid);

        grid.getStyleClass().add("store-entry-grid");
        grid.getStyleClass().add("dense");

        applyState(grid);
        return grid;
    }
}
