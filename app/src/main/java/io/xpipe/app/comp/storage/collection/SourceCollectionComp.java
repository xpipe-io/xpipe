package io.xpipe.app.comp.storage.collection;

import com.jfoenix.controls.JFXTextField;
import io.xpipe.app.comp.base.CountComp;
import io.xpipe.app.comp.base.LazyTextFieldComp;
import io.xpipe.app.comp.storage.source.SourceEntryWrapper;
import io.xpipe.app.core.AppFont;
import io.xpipe.app.core.AppI18n;
import io.xpipe.extension.I18n;
import io.xpipe.extension.event.TrackEvent;
import io.xpipe.extension.fxcomps.Comp;
import io.xpipe.extension.fxcomps.SimpleComp;
import io.xpipe.extension.fxcomps.SimpleCompStructure;
import io.xpipe.extension.fxcomps.impl.FancyTooltipAugment;
import io.xpipe.extension.fxcomps.impl.IconButtonComp;
import io.xpipe.extension.fxcomps.impl.PrettyImageComp;
import io.xpipe.extension.fxcomps.util.PlatformThread;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.effect.Glow;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseButton;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;

public class SourceCollectionComp extends SimpleComp {

    private final SourceCollectionWrapper group;

    public SourceCollectionComp(SourceCollectionWrapper group) {
        this.group = group;
    }

    @Override
    protected Region createSimple() {
        var r = createContent();
        var sp = new StackPane(r);
        sp.setAlignment(Pos.CENTER);
        sp.setOnMouseClicked(e -> {
            if (e.getButton() != MouseButton.PRIMARY) {
                return;
            }

            TrackEvent.withDebug("Storage group clicked")
                    .tag("uuid", group.getCollection().getUuid().toString())
                    .tag("name", group.getName())
                    .build()
                    .handle();
            // StorageViewState.get().selectedGroupProperty().set(group);
            e.consume();
        });
        setupDragAndDrop(sp);
        return sp;
    }

    private void setupDragAndDrop(Region r) {
        r.setOnDragOver(event -> {
            // Moving storage entries
            if (event.getGestureSource() != null
                    && event.getGestureSource() != r
                    && event.getSource() instanceof Node) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                r.setEffect(new Glow(0.5));
            }

            // Files from the outside
            else if (event.getGestureSource() == null && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
                r.setEffect(new Glow(0.5));
            }

            event.consume();
        });

        r.setOnDragExited(event -> {
            r.setEffect(null);
            event.consume();
        });

        r.setOnDragDropped(event -> {
            // Moving storage entries
            if (event.getGestureSource() != null
                    && event.getGestureSource() != r
                    && event.getGestureSource() instanceof Node n) {
                var entry = n.getProperties().get("entry");
                if (entry != null) {
                    var cast = (SourceEntryWrapper) entry;
                    cast.moveTo(this.group);
                }
            }

            // Files from the outside
            else if (event.getGestureSource() == null && event.getDragboard().hasFiles()) {
                event.setDropCompleted(true);
                Dragboard db = event.getDragboard();
                db.getFiles().stream().map(File::toPath).forEach(group::dropFile);
            }

            event.consume();
        });
    }

    private Label createDate() {
        var date = new Label();
        date.textProperty().bind(AppI18n.readableDuration("usedDate", PlatformThread.sync(group.lastAccessProperty())));
        date.getStyleClass().add("date");
        return date;
    }

    private Region createContent() {
        Region nameR;
        if (!group.isRenameable()) {
            Region textFieldR = new JFXTextField(group.getName());
            textFieldR.setDisable(true);

            var tempNote = Comp.of(() -> {
                        var infoIcon = new FontIcon("mdi2i-information-outline");
                        infoIcon.setOpacity(0.75);
                        return new StackPane(infoIcon);
                    })
                    .apply(new FancyTooltipAugment<>(I18n.observable("temporaryCollectionNote")))
                    .createRegion();
            var label = new Label(group.getName(), tempNote);
            label.getStyleClass().add("temp");
            label.setAlignment(Pos.CENTER);
            label.setContentDisplay(ContentDisplay.RIGHT);
            nameR = new HBox(label);
        } else {
            var text = new LazyTextFieldComp(group.nameProperty());
            nameR = text.createRegion();
        }

        var options = new IconButtonComp("mdomz-settings");
        var cm = new SourceCollectionContextMenu<>(true, group, nameR);
        options.apply(new SourceCollectionContextMenu<>(true, group, nameR))
                .apply(r -> {
                    AppFont.setSize(r.get(), -1);
                    r.get().setPadding(new Insets(3, 5, 3, 5));
                })
                .apply(new FancyTooltipAugment<>("collectionOptions"));

        var count = new CountComp<>(
                SourceCollectionViewState.get().getFilteredEntries(this.group), this.group.entriesProperty());
        var spacer = new Region();

        var optionsR = options.createRegion();
        var top = new HBox(nameR, optionsR);
        HBox.setHgrow(nameR, Priority.ALWAYS);
        top.setSpacing(8);
        var countR = count.createRegion();
        countR.prefWidthProperty().bind(optionsR.widthProperty());
        var bottom = new HBox(createDate(), spacer, countR);
        bottom.setAlignment(Pos.CENTER);
        bottom.setSpacing(8);
        HBox.setHgrow(spacer, Priority.ALWAYS);
        var right = new VBox(top, bottom);
        right.setSpacing(8);

        AppFont.header(top);
        AppFont.small(bottom);

        var svgContent = Bindings.createObjectBinding(
                () -> {
                    if (SourceCollectionViewState.get().getSelectedGroup() == group) {
                        return "folder_open.svg";
                    } else {
                        return "folder_closed.svg";
                    }
                },
                SourceCollectionViewState.get().selectedGroupProperty());
        var svg = new PrettyImageComp(svgContent, 55, 55).createRegion();
        svg.getStyleClass().add("icon");
        if (group.isInternal()) {
            svg.setOpacity(0.3);
        }

        var hbox = new HBox(svg, right);
        HBox.setHgrow(right, Priority.ALWAYS);
        hbox.setAlignment(Pos.CENTER);
        // svg.prefHeightProperty().bind(right.heightProperty());
        // svg.prefWidthProperty().bind(right.heightProperty());
        hbox.setSpacing(5);
        hbox.getStyleClass().add("storage-group-entry");

        cm = new SourceCollectionContextMenu<>(false, group, nameR);
        cm.augment(new SimpleCompStructure<>(hbox));
        hbox.setMinWidth(0);

        return hbox;
    }
}
