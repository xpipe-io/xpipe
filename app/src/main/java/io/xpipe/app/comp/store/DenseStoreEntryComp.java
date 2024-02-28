package io.xpipe.app.comp.store;

import io.xpipe.app.core.AppFont;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.augment.GrowAugment;
import io.xpipe.app.fxcomps.util.PlatformThread;
import javafx.beans.binding.Bindings;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

public class DenseStoreEntryComp extends StoreEntryComp {

    private final boolean showIcon;

    public DenseStoreEntryComp(StoreEntryWrapper entry, boolean showIcon, Comp<?> content) {
        super(entry, content);
        this.showIcon = showIcon;
    }

    private Label createInformation(GridPane grid) {
        var information = new Label();
        information.setGraphicTextGap(7);
        information.getStyleClass().add("information");
        AppFont.header(information);

        var state = wrapper.getEntry().getProvider() != null
                ? wrapper.getEntry().getProvider().stateDisplay(wrapper)
                : Comp.empty();
        information.setGraphic(state.createRegion());

        var info = wrapper.getEntry().getProvider().informationString(wrapper);
        var summary = wrapper.getSummary();
        if (wrapper.getEntry().getProvider() != null) {
            information
                    .textProperty()
                    .bind(PlatformThread.sync(Bindings.createStringBinding(
                            () -> {
                                var val = summary.getValue();
                                if (val != null
                                        && grid.isHover()
                                        && wrapper.getEntry().getProvider().alwaysShowSummary()) {
                                    return val;
                                } else {
                                    return info.getValue();
                                }
                            },
                            grid.hoverProperty(),
                            info,
                            summary)));
        }

        return information;
    }

    protected Region createContent() {
        var grid = new GridPane();
        grid.setHgap(8);

        var name = createName().createRegion();
        name.maxWidthProperty()
                .bind(Bindings.createDoubleBinding(
                        () -> {
                            return grid.getWidth() / 2.5;
                        },
                        grid.widthProperty()));

        if (showIcon) {
            var storeIcon = createIcon(30, 24);
            grid.getColumnConstraints().add(new ColumnConstraints(46));
            grid.add(storeIcon, 0, 0);
            GridPane.setHalignment(storeIcon, HPos.CENTER);
        }

        var customSize = content != null ? 100 : 0;
        var custom = new ColumnConstraints(0, customSize, customSize);
        custom.setHalignment(HPos.RIGHT);

        var infoCC = new ColumnConstraints();
        infoCC.prefWidthProperty().bind(content != null ? INFO_WITH_CONTENT_WIDTH : INFO_NO_CONTENT_WIDTH);
        infoCC.setHalignment(HPos.LEFT);

        var nameCC = new ColumnConstraints();
        nameCC.setMinWidth(100);
        nameCC.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(nameCC);
        var nameBox = new HBox(name);
        nameBox.setAlignment(Pos.CENTER_LEFT);
        grid.addRow(0, nameBox);

        var info = createInformation(grid);
        grid.addRow(0, info);
        grid.getColumnConstraints().addAll(infoCC, custom);

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
