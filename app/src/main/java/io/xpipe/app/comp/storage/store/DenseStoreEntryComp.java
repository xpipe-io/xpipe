package io.xpipe.app.comp.storage.store;

import atlantafx.base.controls.Spacer;
import com.jfoenix.controls.JFXButton;
import io.xpipe.app.comp.base.LoadingOverlayComp;
import io.xpipe.app.core.AppFont;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.SimpleCompStructure;
import io.xpipe.app.fxcomps.augment.ContextMenuAugment;
import io.xpipe.app.fxcomps.augment.GrowAugment;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.util.ThreadHelper;
import javafx.beans.binding.Bindings;
import javafx.geometry.HPos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import lombok.SneakyThrows;

public class DenseStoreEntryComp extends StoreEntryComp {

    private final boolean showIcon;
    private final Comp<?> content;

    public DenseStoreEntryComp(StoreEntryWrapper entry, boolean showIcon, Comp<?> content) {
        super(entry);
        this.showIcon = showIcon;
        this.content = content;
    }

    protected Region createContent() {
        var name = createName().createRegion();

        var size = createInformation();

        var date = new Label();
        date.textProperty().bind(AppI18n.readableDuration("usedDate", PlatformThread.sync(entry.lastAccessProperty())));
        AppFont.small(date);
        date.getStyleClass().add("date");

        var grid = new GridPane();

        if (showIcon) {
            var storeIcon = createIcon(30, 25);
            grid.getColumnConstraints().add(new ColumnConstraints(45));
            grid.add(storeIcon, 0, 0);
            GridPane.setHalignment(storeIcon, HPos.CENTER);
        } else {
            grid.add(new Region(), 0, 0);
            grid.getColumnConstraints().add(new ColumnConstraints(5));
        }

        var fill = new ColumnConstraints();
        fill.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(new ColumnConstraints(450), fill);

        grid.add(name, 1, 0);

        var c = content != null ? content.createRegion() : new Region();
        grid.add(c, 2, 0);
        GridPane.setHalignment(c, HPos.CENTER);

        grid.add(createButtonBar().createRegion(), 3, 0, 1, 1);
        GrowAugment.create(true, false).augment(grid);

        AppFont.small(size);
        AppFont.small(date);

        grid.getStyleClass().add("store-entry-grid");

        applyState(grid);

        var button = new JFXButton();
        button.setGraphic(grid);
        GrowAugment.create(true, false).augment(new SimpleCompStructure<>(grid));
        button.getStyleClass().add("store-entry-comp");
        button.getStyleClass().add("condensed-store-entry-comp");
        button.setMaxWidth(2000);
        button.setFocusTraversable(true);
        button.accessibleTextProperty()
                .bind(Bindings.createStringBinding(
                        () -> {
                            return entry.getName();
                        },
                        entry.nameProperty()));
        button.accessibleHelpProperty().bind(entry.getInformation());
        button.setOnAction(event -> {
            event.consume();
            ThreadHelper.runFailableAsync(() -> {
                entry.refreshIfNeeded();
                entry.executeDefaultAction();
            });
        });
        HBox.setHgrow(button, Priority.ALWAYS);

        new ContextMenuAugment<>(() -> DenseStoreEntryComp.this.createContextMenu())
                .augment(new SimpleCompStructure<>(button));

        return new HBox(button, new Spacer(25));
    }

    @SneakyThrows
    @Override
    protected Region createSimple() {
        var loading = new LoadingOverlayComp(Comp.of(() -> createContent()), entry.getLoading());
        var region = loading.createRegion();
        return region;
    }
}
