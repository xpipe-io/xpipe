package io.xpipe.app.comp.store;

import io.xpipe.app.core.AppFont;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.core.process.OsType;

import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

public class StandardStoreEntryComp extends StoreEntryComp {

    public StandardStoreEntryComp(StoreSection section, Comp<?> content) {
        super(section, content);
    }

    @Override
    public boolean isFullSize() {
        return true;
    }

    private Label createSummary() {
        var summary = new Label();
        summary.textProperty().bind(getWrapper().getSummary());
        summary.getStyleClass().add("summary");
        AppFont.small(summary);
        return summary;
    }

    protected Region createContent() {
        var name = createName().createRegion();
        var notes = new StoreNotesComp(getWrapper()).createRegion();

        var grid = new GridPane();
        grid.setHgap(6);
        grid.setVgap(OsType.getLocal() == OsType.MACOS ? 2 : 0);

        var storeIcon = createIcon(46, 40);
        grid.add(storeIcon, 0, 0, 1, 2);
        grid.getColumnConstraints().add(new ColumnConstraints(56));

        var nameAndNotes = new HBox(name, notes);
        nameAndNotes.setSpacing(6);
        nameAndNotes.setAlignment(Pos.CENTER_LEFT);
        grid.add(nameAndNotes, 1, 0);
        GridPane.setVgrow(nameAndNotes, Priority.ALWAYS);

        var summaryBox = new HBox(createSummary());
        summaryBox.setAlignment(Pos.TOP_LEFT);
        GridPane.setVgrow(summaryBox, Priority.ALWAYS);
        grid.add(summaryBox, 1, 1);

        var nameCC = new ColumnConstraints();
        nameCC.setMinWidth(100);
        nameCC.setHgrow(Priority.ALWAYS);
        nameCC.setPrefWidth(100);
        grid.getColumnConstraints().addAll(nameCC);

        grid.add(createInformation(), 2, 0, 1, 2);
        var info = new ColumnConstraints();
        info.prefWidthProperty().bind(content != null ? INFO_WITH_CONTENT_WIDTH : INFO_NO_CONTENT_WIDTH);
        info.setHalignment(HPos.LEFT);
        grid.getColumnConstraints().add(info);

        var customSize = content != null ? 100 : 0;
        var custom = new ColumnConstraints(0, customSize, customSize);
        custom.setHalignment(HPos.RIGHT);
        var cr = content != null ? content.createRegion() : new Region();
        var bb = createButtonBar();
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
