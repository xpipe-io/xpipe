package io.xpipe.app.comp.store;

import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.comp.base.DialogComp;
import io.xpipe.app.comp.base.MarkdownEditorComp;
import io.xpipe.app.core.AppFont;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.CompStructure;
import io.xpipe.app.fxcomps.impl.IconButtonComp;
import io.xpipe.app.fxcomps.util.BindingsHelper;
import io.xpipe.app.storage.DataStorage;

import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.paint.Color;

import atlantafx.base.controls.Popover;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class StoreNotesComp extends Comp<StoreNotesComp.Structure> {

    private final StoreEntryWrapper wrapper;

    public StoreNotesComp(StoreEntryWrapper wrapper) {
        this.wrapper = wrapper;
    }

    @Override
    public Structure createBase() {
        var n = wrapper.getNotes();
        var button = new IconButtonComp("mdi2n-note-text")
                .apply(struc -> AppFont.small(struc.get()))
                .focusTraversableForAccessibility()
                .tooltipKey("notes")
                .styleClass("notes-button")
                .hide(BindingsHelper.map(n, s -> s.getCommited() == null && s.getCurrent() == null))
                .createStructure()
                .get();
        button.prefWidthProperty().bind(button.heightProperty());

        var prop = new SimpleStringProperty(n.getValue().getCurrent());

        var popover = new AtomicReference<Popover>();
        button.setOnAction(e -> {
            if (n.getValue().getCurrent() == null) {
                return;
            }

            if (popover.get() != null && popover.get().isShowing()) {
                e.consume();
                return;
            }

            popover.set(createPopover(popover, prop));
            popover.get().show(button);
            e.consume();
        });
        prop.addListener((observable, oldValue, newValue) -> {
            n.setValue(new StoreNotes(n.getValue().getCommited(), newValue));
        });
        n.addListener((observable, oldValue, s) -> {
            prop.set(s.getCurrent());
            // Check for scene existence. If we exited the platform immediately after adding notes, this might throw an
            // exception
            if (s.getCurrent() != null
                    && oldValue.getCommited() == null
                    && oldValue.isCommited()
                    && button.getScene() != null) {
                Platform.runLater(() -> {
                    popover.set(createPopover(popover, prop));
                    popover.get().show(button);
                });
            }
        });
        return new Structure(popover.get(), button);
    }

    private Popover createPopover(AtomicReference<Popover> ref, Property<String> prop) {
        var n = wrapper.getNotes();
        var md = new MarkdownEditorComp(prop, "notes-" + wrapper.getName().getValue())
                .prefWidth(600)
                .prefHeight(600)
                .createStructure();
        var dialog = new DialogComp() {

            @Override
            protected void finish() {
                n.setValue(
                        new StoreNotes(n.getValue().getCurrent(), n.getValue().getCurrent()));
                ref.get().hide();
            }

            @Override
            protected String finishKey() {
                return "apply";
            }

            @Override
            public Comp<?> bottom() {
                return new ButtonComp(AppI18n.observable("delete"), () -> {
                            n.setValue(new StoreNotes(null, null));
                        })
                        .hide(BindingsHelper.map(n, v -> v.getCommited() == null));
            }

            @Override
            protected List<Comp<?>> customButtons() {
                return List.of(new ButtonComp(AppI18n.observable("cancel"), () -> {
                    ref.get().hide();
                }));
            }

            @Override
            public Comp<?> content() {
                return Comp.of(() -> md.get());
            }
        }.createRegion();

        var popover = new Popover(dialog);
        popover.getScene().setFill(Color.TRANSPARENT);
        popover.setCloseButtonEnabled(true);
        popover.setHeaderAlwaysVisible(true);
        popover.setDetachable(true);
        popover.setTitle(wrapper.getName().getValue());
        popover.showingProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                n.setValue(
                        new StoreNotes(n.getValue().getCommited(), n.getValue().getCommited()));
                DataStorage.get().saveAsync();
                ref.set(null);
            }
        });
        AppFont.small(popover.getContentNode());

        md.getEditButton().addEventFilter(ActionEvent.ANY, event -> {
            if (!popover.isDetached()) {
                popover.setDetached(true);
                event.consume();
                Platform.runLater(() -> {
                    Platform.runLater(() -> {
                        md.getEditButton().fire();
                    });
                });
            }
        });

        return popover;
    }

    public record Structure(Popover popover, Button button) implements CompStructure<Button> {

        @Override
        public Button get() {
            return button;
        }
    }
}
