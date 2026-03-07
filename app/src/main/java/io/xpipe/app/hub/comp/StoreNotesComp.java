package io.xpipe.app.hub.comp;

import io.xpipe.app.comp.*;
import io.xpipe.app.comp.base.*;
import io.xpipe.app.core.AppFontSizes;
import io.xpipe.app.platform.BindingsHelper;
import io.xpipe.app.storage.DataStorage;

import io.xpipe.app.util.FileOpener;
import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.Button;
import javafx.scene.input.MouseButton;

import java.util.UUID;

public class StoreNotesComp extends RegionBuilder<Button> {

    public static void showDialog(StoreEntryWrapper wrapper, String initial) {
        var prop = new SimpleStringProperty(initial);
        var md = new MarkdownEditorComp(prop, "notes-" + wrapper.getName().getValue())
                .prefWidth(700)
                .prefHeight(800);

        var modal = ModalOverlay.of(new ReadOnlyStringWrapper(wrapper.getName().getValue()), md, null);
        if (wrapper.getNotes().getValue() != null) {
            modal.addButton(new ModalButton("delete", () -> {
                wrapper.getEntry().setNotes(null);
                DataStorage.get().saveAsync();
            }, true, false));
        }
        modal.addButton(new ModalButton("cancel", () -> {}, true, false));
        modal.addButton(new ModalButton("apply", () -> {
            wrapper.getEntry().setNotes(prop.getValue());
            DataStorage.get().saveAsync();
        }, true, true));
        modal.show();
    }

    private final StoreEntryWrapper wrapper;

    public StoreNotesComp(StoreEntryWrapper wrapper) {
        this.wrapper = wrapper;
    }

    @Override
    protected Button createSimple() {
        var n = wrapper.getNotes();
        var button = new IconButtonComp("mdi2n-note-text-outline")
                .apply(struc -> AppFontSizes.xs(struc))
                .describe(d ->
                        d.nameKey("notes").focusTraversal(RegionDescriptor.FocusTraversal.ENABLED_FOR_ACCESSIBILITY))
                .style("notes-button")
                .hide(n.isNull())
                .build();
        button.setOpacity(0.85);
        button.prefWidthProperty().bind(button.heightProperty());

        button.setOnAction(e -> {
            showDialog(wrapper, wrapper.getNotes().getValue());
            e.consume();
        });

        var editKey = UUID.randomUUID().toString();
        button.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.isShiftDown()) {
                FileOpener.openString("notes.md", editKey, wrapper.getNotes().getValue(), s -> wrapper.getEntry().setNotes(s));
                e.consume();
            }
        });
        return button;
    }
}
