package io.xpipe.app.comp.store;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.augment.GrowAugment;
import io.xpipe.app.util.PlatformThread;
import io.xpipe.app.util.Rect;
import io.xpipe.core.process.OsType;

import javafx.beans.binding.Bindings;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;

public class DenseStoreEntryComp extends StoreEntryComp {

    public DenseStoreEntryComp(StoreSection section, Comp<?> content) {
        super(section, content);
    }

    private Label createInformation(GridPane grid) {
        var information = new Label();
        information.setGraphicTextGap(7);
        information.getStyleClass().add("information");

        var state = getWrapper().getEntry().getProvider() != null
                ? getWrapper().getEntry().getProvider().stateDisplay(getWrapper())
                : Comp.empty();
        information.setGraphic(state.createRegion());

        var summary = getWrapper().getShownSummary();
        if (getWrapper().getEntry().getProvider() != null) {
            var info = getWrapper().getShownInformation();
            information
                    .textProperty()
                    .bind(PlatformThread.sync(Bindings.createStringBinding(
                            () -> {
                                var val = summary.getValue();
                                var p = getWrapper().getEntry().getProvider();
                                if (val != null && grid.isHover() && p.alwaysShowSummary()) {
                                    return val;
                                } else if (info.getValue() == null && p.alwaysShowSummary()) {
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

    @Override
    public boolean isFullSize() {
        return false;
    }

    @Override
    public int getHeight() {
        return OsType.getLocal() == OsType.WINDOWS ? 38 : 37;
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
        var notes = new StoreNotesComp(getWrapper()).createRegion();
        var userIcon = createUserIcon().createRegion();

        var selection = createBatchSelection().createRegion();
        grid.add(selection, 0, 0, 1, 2);
        grid.getColumnConstraints().add(new ColumnConstraints(25));
        StoreViewState.get().getBatchMode().subscribe(batch -> {
            if (batch) {
                grid.getColumnConstraints().set(0, new ColumnConstraints(25));
            } else {
                grid.getColumnConstraints().set(0, new ColumnConstraints(-8));
            }
        });

        var storeIcon = createIcon(28, 24);
        GridPane.setHalignment(storeIcon, HPos.CENTER);
        grid.add(storeIcon, 1, 0);
        grid.getColumnConstraints().add(new ColumnConstraints(34));

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

        var active = new StoreActiveComp(getWrapper()).createRegion();
        var nameBox = new HBox(name, userIcon, notes);
        getWrapper().getSessionActive().subscribe(aBoolean -> {
            if (!aBoolean) {
                nameBox.getChildren().remove(active);
            } else {
                nameBox.getChildren().add(1, active);
            }
        });
        nameBox.setSpacing(6);
        nameBox.setAlignment(Pos.CENTER_LEFT);
        grid.addRow(0, nameBox);

        var info = createInformation(grid);
        grid.addRow(0, info);
        grid.getColumnConstraints().addAll(infoCC, custom);

        var cr = content != null ? content.createRegion() : new Region();
        var bb = createButtonBar();
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
