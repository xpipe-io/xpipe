package io.xpipe.app.hub.entry;

import io.xpipe.app.comp.BaseRegionBuilder;
import io.xpipe.app.comp.RegionBuilder;
import io.xpipe.app.core.AppFontSizes;
import io.xpipe.app.core.AppSizeBreakpoints;
import io.xpipe.app.hub.list.StoreViewState;
import io.xpipe.app.hub.section.StoreSection;
import io.xpipe.app.util.OsType;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.geometry.HPos;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

import atlantafx.base.controls.Spacer;

import java.util.ArrayList;
import java.util.List;

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
        var userIcon = createScopeIcon().build();
        var pinIcon = createPinIcon().build();
        var active = new StoreActiveComp(getWrapper()).build();
        var info = createInformation().build();

        var grid = new GridPane();
        grid.hgapProperty()
                .bind(Bindings.createDoubleBinding(
                        () -> {
                            return AppSizeBreakpoints.portraitMode().get() ? 2.0 : 6.0;
                        },
                        AppSizeBreakpoints.portraitMode()));
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

        var nameBoxEntries = new ArrayList<Node>();
        nameBoxEntries.add(name);
        nameBoxEntries.add(new Spacer(2, Orientation.HORIZONTAL));
        nameBoxEntries.add(tags);
        nameBoxEntries.addAll(List.of(index, active, templateIcon, userIcon, pinIcon, notes));
        var nameBox = new HBox(nameBoxEntries.toArray(Node[]::new));
        nameBox.setSpacing(4);
        nameBox.setAlignment(Pos.CENTER_LEFT);
        grid.add(nameBox, 2, 0);
        GridPane.setVgrow(nameBox, Priority.ALWAYS);

        var summaryBox = new HBox(createSummary());
        summaryBox.setAlignment(Pos.TOP_LEFT);
        GridPane.setVgrow(summaryBox, Priority.ALWAYS);
        grid.add(summaryBox, 2, 1);

        var nameCC = new ColumnConstraints();
        nameCC.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(nameCC);

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

        grid.add(info, 3, 0, 1, 2);
        var infoCC = new ColumnConstraints();
        infoCC.prefWidthProperty().bind(infoWidth);
        infoCC.setHalignment(HPos.LEFT);
        grid.getColumnConstraints().add(infoCC);

        var rightCC = new ColumnConstraints();
        rightCC.setMinWidth(0);
        rightCC.setHalignment(HPos.RIGHT);

        grid.add(controls, 4, 0, 1, 2);
        grid.getColumnConstraints().add(rightCC);

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
}
