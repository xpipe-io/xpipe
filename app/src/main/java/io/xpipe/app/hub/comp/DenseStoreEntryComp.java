package io.xpipe.app.hub.comp;

import io.xpipe.app.comp.BaseRegionBuilder;
import io.xpipe.app.comp.RegionBuilder;
import io.xpipe.app.core.AppFontSizes;

import io.xpipe.app.core.AppSizeBreakpoints;
import javafx.beans.binding.Bindings;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.layout.*;

public class DenseStoreEntryComp extends StoreEntryComp {

    public DenseStoreEntryComp(StoreSection section, BaseRegionBuilder<?, ?> content) {
        super(section, content);
    }

    private Label createInformation(GridPane grid) {
        var information = new Label();
        information.setGraphicTextGap(7);
        information.getStyleClass().add("information");

        var state = getWrapper().getEntry().getProvider() != null
                ? getWrapper().getEntry().getProvider().stateDisplay(section)
                : RegionBuilder.empty();
        information.setGraphic(state.build());

        var summary = getWrapper().getShownSummary();
        if (getWrapper().getEntry().getProvider() != null) {
            var info = getWrapper().getShownInformation();
            information
                    .textProperty()
                    .bind(Bindings.createStringBinding(
                            () -> {
                                var summaryValue = summary.getValue();
                                var infoValue = info.getValue();
                                if (summaryValue != null && infoValue != null && grid.isHover()) {
                                    return summaryValue;
                                } else if (summaryValue != null && infoValue != null) {
                                    return infoValue;
                                } else if (infoValue == null && summaryValue != null) {
                                    return summaryValue;
                                } else if (summaryValue == null && infoValue != null) {
                                    return infoValue;
                                } else {
                                    return null;
                                }
                            },
                            grid.hoverProperty(),
                            info,
                            summary));
        }

        information.visibleProperty().bind(information.widthProperty().greaterThan(40));
        information.setTextOverrun(OverrunStyle.CLIP);

        return information;
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
        grid.hgapProperty().bind(Bindings.createDoubleBinding(() -> {
            return AppSizeBreakpoints.portraitMode().get() ? 2.0 : 6.0;
        }, AppSizeBreakpoints.portraitMode()));

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
        var userIcon = createUserIcon().build();
        var pinIcon = createPinIcon().build();
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
        var customWidth = Bindings.createDoubleBinding(() -> {
            if (AppSizeBreakpoints.compactMode().get()) {
                return contentRegion != null && contentRegion.isVisible() ? 120.0 : 40.0;
            }

            return contentRegion != null && contentRegion.isVisible() ? 140.0 : 70.0;
        }, INFO_WIDTH, AppSizeBreakpoints.compactMode());
        var infoWidth = Bindings.createDoubleBinding(() -> {
            return INFO_WIDTH.get() - (customWidth.get());
        }, INFO_WIDTH, customWidth);

        var custom = new ColumnConstraints();
        custom.setMinWidth(0);
        custom.prefWidthProperty().bind(customWidth);
        custom.maxWidthProperty().bind(custom.prefWidthProperty());
        custom.setHalignment(HPos.RIGHT);

        var infoCC = new ColumnConstraints();
        infoCC.prefWidthProperty().bind(infoWidth);
        infoCC.setHalignment(HPos.LEFT);

        var nameCC = new ColumnConstraints();
        nameCC.setMinWidth(100);
        nameCC.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(nameCC);

        var nameBox = new HBox(name, tags, index, active, templateIcon, userIcon, pinIcon, notes);
        nameBox.setSpacing(4);
        nameBox.setAlignment(Pos.CENTER_LEFT);
        grid.addRow(0, nameBox);

        var info = createInformation(grid);
        grid.addRow(0, info);
        grid.getColumnConstraints().addAll(infoCC, custom);

        var cr = contentRegion != null ? contentRegion : new Region();
        cr.getStyleClass().add("custom-content");
        var bb = createButtonBar(name);
        var controls = new HBox(cr, bb);
        controls.setFillHeight(true);
        controls.setAlignment(Pos.CENTER_RIGHT);
        controls.setSpacing(10);
        controls.setPadding(new Insets(0, 0, 0, 10));
        HBox.setHgrow(cr, Priority.ALWAYS);
        grid.addRow(0, controls);

        grid.getStyleClass().add("store-entry-grid");
        grid.getStyleClass().add("dense");

        applyState(grid);
        return grid;
    }
}
