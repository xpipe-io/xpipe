package io.xpipe.app.comp.store;

import atlantafx.base.controls.Popover;
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
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.Region;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class StoreNotesComp extends Comp<StoreNotesComp.Structure> {

    private final StoreEntryWrapper wrapper;

    public StoreNotesComp(StoreEntryWrapper wrapper) {this.wrapper = wrapper;}

    @Override
    public Structure createBase() {
        var n = wrapper.getNotes();
        var button = new IconButtonComp("mdi2n-note-text")
                .apply(struc -> AppFont.small(struc.get()))
                .focusTraversableForAccessibility()
                .tooltipKey("notes")
                .styleClass("notes-button")
                .grow(false, true)
                .hide(BindingsHelper.map(n, s -> s.getCommited() == null && s.getCurrent() == null))
                .padding(new Insets(5))
                .createStructure().get();
        button.prefWidthProperty().bind(button.heightProperty());

        var prop = new SimpleStringProperty(n.getValue().getCurrent());
        var md = new MarkdownEditorComp(prop,"notes-" + wrapper.getName().getValue()).createStructure();

        var popover = new AtomicReference<Popover>();
        var dialog = new DialogComp() {

            @Override
            protected void finish() {
                n.setValue(new StoreNotes(n.getValue().getCurrent(), n.getValue().getCurrent()));
                popover.get().hide();
            }

            @Override
            protected String finishKey() {
                return "apply";
            }

            @Override
            public Comp<?> bottom() {
                return new ButtonComp(AppI18n.observable("delete"), () -> {
                    n.setValue(new StoreNotes(null, null));
                }).hide(BindingsHelper.map(n, v -> v.getCommited() == null));
            }

            @Override
            protected List<Comp<?>> customButtons() {
                return List.of(new ButtonComp(AppI18n.observable("cancel"), () -> {
                    popover.get().hide();
                }));
            }

            @Override
            public Comp<?> content() {
                return Comp.of(() -> md.get());
            }
        }.createRegion();

        popover.set(createPopover(dialog));
        button.setOnAction(e -> {
            if (n.getValue().getCurrent() == null) {
                return;
            }

            if (popover.get().isShowing()) {
                e.consume();
                return;
            }

            popover.get().show(button);
            e.consume();
        });
        md.getEditButton().addEventFilter(ActionEvent.ANY, event -> {
            if (!popover.get().isDetached()) {
                popover.get().setDetached(true);
                event.consume();
                Platform.runLater(() -> {
                    Platform.runLater(() -> {
                        md.getEditButton().fire();
                    });
                });
            }
        });
        popover.get().showingProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                n.setValue(new StoreNotes(n.getValue().getCommited(), n.getValue().getCommited()));
                DataStorage.get().saveAsync();
            }
        });
        prop.addListener((observable, oldValue, newValue) -> {
            n.setValue(new StoreNotes(n.getValue().getCommited(), newValue));
        });
        n.addListener((observable, oldValue, s) -> {
            prop.set(s.getCurrent());
            if (s.getCurrent() != null && oldValue.getCommited() == null && oldValue.isCommited()) {
                Platform.runLater(() -> {
                    popover.get().show(button);
                });
            }
        });
        return new Structure(popover.get(), button);
    }

    private Popover createPopover(Region content) {
        var popover = new Popover(content);
        popover.setCloseButtonEnabled(true);
        popover.setHeaderAlwaysVisible(true);
        popover.setDetachable(true);
        popover.setTitle(wrapper.getName().getValue());
        popover.setMaxWidth(400);
        popover.setHeight(600);
        AppFont.small(popover.getContentNode());
        return popover;
    }


    public record Structure(Popover popover, Button button) implements CompStructure<Button> {

        @Override
        public Button get() {
            return button;
        }
    }
}
