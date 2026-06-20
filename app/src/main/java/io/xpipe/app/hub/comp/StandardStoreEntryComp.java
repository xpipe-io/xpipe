package io.xpipe.app.hub.comp;

import io.xpipe.app.comp.BaseRegionBuilder;
import io.xpipe.app.comp.RegionBuilder;
import io.xpipe.app.core.AppFontSizes;
import io.xpipe.app.core.AppSizeBreakpoints;
import io.xpipe.app.util.OsType;

import javafx.beans.binding.Bindings;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.layout.*;

public class StandardStoreEntryComp extends StoreEntryComp {

    public StandardStoreEntryComp(StoreSection section, BaseRegionBuilder<?, ?> content) {
        super(section, content);
    }

    @Override
    public boolean isFullSize() {
        return true;
    }

    @Override
    public int getHeight() {
        return 57;
    }

    protected Region createContent() {
        var name = createName().build();
        var tags = createTags().build();
        var index = createOrderIndex().build();
        var notes = new StoreNotesComp(getWrapper()).build();
        var templateIcon = createTemplateIcon().build();
        var userIcon = createUserIcon().build();
        var pinIcon = createPinIcon().build();
        var active = new StoreActiveComp(getWrapper()).build();

        var grid = new GridPane();
        grid.hgapProperty().bind(Bindings.createDoubleBinding(() -> {
            return AppSizeBreakpoints.portraitMode().get() ? 2.0 : 6.0;
        }, AppSizeBreakpoints.portraitMode()));
        grid.setVgap(OsType.ofLocal() == OsType.MACOS ? 2 : 0);

        var selection = createBatchSelection();
        grid.add(selection.build(), 0, 0, 1, 2);
        grid.getColumnConstraints().add(new ColumnConstraints(25));
        StoreViewState.get().getBatchMode().subscribe(batch -> {
            if (batch) {
                grid.getColumnConstraints().set(0, new ColumnConstraints(25));
            } else {
                grid.getColumnConstraints().set(0, new ColumnConstraints(-6));
            }
        });

        var storeIcon = createIcon(46, 40, AppFontSizes::title);
        grid.add(storeIcon.build(), 1, 0, 1, 2);
        grid.getColumnConstraints().add(new ColumnConstraints(52));

        var nameBox = new HBox(name, tags, index, active, templateIcon, userIcon, pinIcon, notes);
        nameBox.setSpacing(4);
        nameBox.setAlignment(Pos.CENTER_LEFT);
        grid.add(nameBox, 2, 0);
        GridPane.setVgrow(nameBox, Priority.ALWAYS);

        var summaryBox = new HBox(createSummary());
        summaryBox.setAlignment(Pos.TOP_LEFT);
        GridPane.setVgrow(summaryBox, Priority.ALWAYS);
        grid.add(summaryBox, 2, 1);

        var nameCC = new ColumnConstraints();
        nameCC.setMinWidth(100);
        nameCC.setHgrow(Priority.ALWAYS);
        nameCC.setPrefWidth(100);
        grid.getColumnConstraints().addAll(nameCC);

        var contentRegion = content != null ? content.build() : null;
        var customWidth = Bindings.createDoubleBinding(() -> {
            if (AppSizeBreakpoints.compactMode().get()) {
                return contentRegion != null && contentRegion.isVisible() ? 125.0 : 55.0;
            }

            return contentRegion != null && contentRegion.isVisible() ? 140.0 : 70.0;
        }, INFO_WIDTH, AppSizeBreakpoints.compactMode());
        var infoWidth = Bindings.createDoubleBinding(() -> {
            return INFO_WIDTH.get() - (customWidth.get());
        }, INFO_WIDTH, customWidth);

        grid.add(createInformation(), 3, 0, 1, 2);
        var info = new ColumnConstraints();
        info.prefWidthProperty().bind(infoWidth);
        info.setHalignment(HPos.LEFT);
        grid.getColumnConstraints().add(info);

        var custom = new ColumnConstraints();
        custom.setMinWidth(0);
        custom.prefWidthProperty().bind(customWidth);
        custom.maxWidthProperty().bind(custom.prefWidthProperty());
        custom.setHalignment(HPos.RIGHT);
        var cr = contentRegion != null ? contentRegion : new Region();
        cr.getStyleClass().add("custom-content");
        var bb = createButtonBar(name);
        var controls = new HBox(cr, bb);
        controls.setFillHeight(true);
        HBox.setHgrow(cr, Priority.ALWAYS);
        controls.setAlignment(Pos.CENTER_RIGHT);
        controls.setSpacing(10);
        controls.setPadding(new Insets(0, 0, 0, 10));
        grid.add(controls, 4, 0, 1, 2);
        grid.getColumnConstraints().add(custom);

        grid.getStyleClass().add("store-entry-grid");

        applyState(grid);

        return grid;
    }

    private Label createSummary() {
        var summary = new Label();
        summary.textProperty().bind(getWrapper().getShownDescription());
        summary.getStyleClass().add("summary");
        AppFontSizes.xs(summary);
        return summary;
    }

    private Label createInformation() {
        var information = new Label();
        information.setGraphicTextGap(7);
        information.textProperty().bind(getWrapper().getShownInformation());
        information.getStyleClass().add("information");

        var state = getWrapper().getEntry().getProvider() != null
                ? getWrapper().getEntry().getProvider().stateDisplay(section)
                : RegionBuilder.empty();
        information.setGraphic(state.build());

        information.visibleProperty().bind(information.widthProperty().greaterThan(40));
        information.setTextOverrun(OverrunStyle.CLIP);

        return information;
    }
}
