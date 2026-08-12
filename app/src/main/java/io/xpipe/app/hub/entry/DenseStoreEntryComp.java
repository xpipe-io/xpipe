package io.xpipe.app.hub.entry;

import io.xpipe.app.comp.BaseRegionBuilder;
import io.xpipe.app.comp.RegionBuilder;
import io.xpipe.app.core.AppFontSizes;
import io.xpipe.app.core.AppSizeBreakpoints;
import io.xpipe.app.hub.list.StoreViewState;
import io.xpipe.app.hub.section.StoreSection;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.geometry.HPos;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.*;

import atlantafx.base.controls.Spacer;

import java.util.ArrayList;
import java.util.List;

public class DenseStoreEntryComp extends StoreEntryComp {

    public DenseStoreEntryComp(StoreSection section, BaseRegionBuilder<?, ?> content) {
        super(section, content);
    }

    @Override
    public boolean isFullSize() {
        return false;
    }

    @Override
    public int getHeight() {
        return 37;
    }

    protected Region createContent() {
        var grid = new GridPane();
        grid.hgapProperty()
                .bind(Bindings.createDoubleBinding(
                        () -> {
                            return AppSizeBreakpoints.portraitMode().get() ? 2.0 : 6.0;
                        },
                        AppSizeBreakpoints.portraitMode()));

        var tags = createTags().build();
        var index = createOrderIndex().build();
        var name = createName().build();
        name.maxWidthProperty()
                .bind(Bindings.createDoubleBinding(
                        () -> {
                            return grid.getWidth() / 2.5;
                        },
                        grid.widthProperty()));
        var notes = new StoreNotesComp(getWrapper()).build();
        var templateIcon = createTemplateIcon().build();
        var userIcon = createScopeIcon().build();
        var pinIcon = createPinIcon().build();
        var info = createInformation().build();
        var active = new StoreActiveComp(getWrapper()).build();

        var selection = createBatchSelection().build();
        grid.add(selection, 0, 0, 1, 2);
        grid.getColumnConstraints().add(new ColumnConstraints(25));
        StoreViewState.get().getBatchMode().subscribe(batch -> {
            if (batch) {
                grid.getColumnConstraints().set(0, new ColumnConstraints(25));
            } else {
                grid.getColumnConstraints().set(0, new ColumnConstraints(-8));
            }
        });

        var storeIcon = createIcon(28, 24, AppFontSizes::xxxl).build();
        GridPane.setHalignment(storeIcon, HPos.CENTER);
        grid.add(storeIcon, 1, 0);
        grid.getColumnConstraints().add(new ColumnConstraints(34));

        var contentRegion = content != null ? content.build() : null;
        var cr = contentRegion != null
                ? contentRegion
                : RegionBuilder.empty().hide(new ReadOnlyBooleanWrapper(true)).build();
        cr.getStyleClass().add("custom-content");
        var bb = createButtonBar(name);
        var controls = new HBox(cr, bb);
        controls.setFillHeight(true);
        HBox.setHgrow(cr, Priority.ALWAYS);
        controls.setAlignment(Pos.CENTER_RIGHT);
        controls.setSpacing(10);

        var bbButton = (Region) bb.getChildrenUnmodifiable().getFirst();
        info.prefHeightProperty().bind(bbButton.heightProperty());
        info.maxHeightProperty().bind(bbButton.heightProperty());

        var rightWidth = Bindings.createDoubleBinding(
                () -> {
                    return controls.getWidth();
                },
                controls.widthProperty());
        var infoWidth = Bindings.createDoubleBinding(
                () -> {
                    if (getWrapper().getShownInformation().getValue() == null) {
                        return 0.0;
                    }

                    return INFO_WIDTH.get() - (rightWidth.get());
                },
                INFO_WIDTH,
                rightWidth,
                getWrapper().getShownInformation());

        rightWidth.addListener((observable, oldValue, newValue) -> {
            if (newValue.doubleValue() > 0.0) {
                Platform.runLater(() -> {
                    info.setVisible(true);
                });
            }
        });
        info.setVisible(false);

        var rightCC = new ColumnConstraints();
        rightCC.setMinWidth(0);
        rightCC.setHalignment(HPos.RIGHT);

        var infoCC = new ColumnConstraints();
        infoCC.prefWidthProperty().bind(infoWidth);
        infoCC.setHalignment(HPos.LEFT);

        var nameCC = new ColumnConstraints();
        nameCC.setMinWidth(100);
        nameCC.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(nameCC);

        var nameBoxEntries = new ArrayList<Node>();
        nameBoxEntries.add(name);
        nameBoxEntries.add(new Spacer(2, Orientation.HORIZONTAL));
        nameBoxEntries.add(tags);
        nameBoxEntries.addAll(List.of(index, active, templateIcon, userIcon, pinIcon, notes));
        var nameBox = new HBox(nameBoxEntries.toArray(Node[]::new));
        nameBox.setSpacing(4);
        nameBox.setAlignment(Pos.CENTER_LEFT);
        grid.addRow(0, nameBox);

        grid.addRow(0, info);
        grid.getColumnConstraints().addAll(infoCC, rightCC);

        grid.addRow(0, controls);

        grid.getStyleClass().add("store-entry-grid");
        grid.getStyleClass().add("dense");

        applyState(grid);
        return grid;
    }
}
