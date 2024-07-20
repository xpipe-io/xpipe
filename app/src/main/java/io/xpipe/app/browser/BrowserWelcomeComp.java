package io.xpipe.app.browser;

import atlantafx.base.controls.Spacer;
import atlantafx.base.theme.Styles;
import io.xpipe.app.browser.session.BrowserSessionModel;
import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.comp.base.ListBoxViewComp;
import io.xpipe.app.comp.base.TileButtonComp;
import io.xpipe.app.core.AppFont;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.impl.HorizontalComp;
import io.xpipe.app.fxcomps.impl.LabelComp;
import io.xpipe.app.fxcomps.impl.PrettyImageHelper;
import io.xpipe.app.fxcomps.impl.PrettySvgComp;
import io.xpipe.app.fxcomps.util.BindingsHelper;
import io.xpipe.app.fxcomps.util.DerivedObservableList;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.util.ThreadHelper;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;

public class BrowserWelcomeComp extends SimpleComp {

    private final BrowserSessionModel model;

    public BrowserWelcomeComp(BrowserSessionModel model) {
        this.model = model;
    }

    @Override
    protected Region createSimple() {
        var state = model.getSavedState();

        var welcome = new BrowserGreetingComp().createSimple();

        var vbox = new VBox(welcome, new Spacer(4, Orientation.VERTICAL));
        vbox.setAlignment(Pos.CENTER_LEFT);

        var img = new PrettySvgComp(new SimpleStringProperty("Hips.svg"), 50, 75)
                .padding(new Insets(5, 0, 0, 0))
                .createRegion();

        var hbox = new HBox(img, vbox);
        hbox.setAlignment(Pos.CENTER_LEFT);
        hbox.setSpacing(15);

        if (state == null) {
            var header = new Label();
            header.textProperty().bind(AppI18n.observable("browserWelcomeEmpty"));
            vbox.getChildren().add(header);
            hbox.setPadding(new Insets(40, 40, 40, 50));
            return new VBox(hbox);
        }

        var list = new DerivedObservableList<>(state.getEntries(), true)
                .filtered(e -> {
                    var entry = DataStorage.get().getStoreEntryIfPresent(e.getUuid());
                    if (entry.isEmpty()) {
                        return false;
                    }

                    if (!entry.get().getValidity().isUsable()) {
                        return false;
                    }

                    return true;
                })
                .getList();
        var empty = Bindings.createBooleanBinding(() -> list.isEmpty(), list);

        var headerBinding = BindingsHelper.flatMap(empty, b -> {
            if (b) {
                return AppI18n.observable("browserWelcomeEmpty");
            } else {
                return AppI18n.observable("browserWelcomeSystems");
            }
        });
        var header = new LabelComp(headerBinding).createRegion();
        AppFont.setSize(header, 1);
        vbox.getChildren().add(header);

        var storeList = new VBox();
        storeList.setSpacing(8);

        var listBox = new ListBoxViewComp<>(
                        list,
                        list,
                        e -> {
                            var disable = new SimpleBooleanProperty();
                            var entryButton = entryButton(e, disable);
                            var dirButton = dirButton(e, disable);
                            return new HorizontalComp(List.of(entryButton, dirButton)).apply(struc -> {
                                ((Region) struc.get().getChildren().get(0))
                                        .prefHeightProperty()
                                        .bind(struc.get().heightProperty());
                                ((Region) struc.get().getChildren().get(1))
                                        .prefHeightProperty()
                                        .bind(struc.get().heightProperty());
                            });
                        },
                        true)
                .apply(struc -> {
                    VBox vBox = (VBox) struc.get().getContent();
                    vBox.setSpacing(10);
                })
                .hide(empty)
                .createRegion();

        var layout = new VBox();
        layout.getStyleClass().add("welcome");
        layout.setPadding(new Insets(60, 40, 40, 50));
        layout.setSpacing(18);
        layout.getChildren().add(hbox);
        layout.getChildren().add(Comp.separator().hide(empty).createRegion());
        layout.getChildren().add(listBox);
        VBox.setVgrow(layout.getChildren().get(2), Priority.NEVER);
        layout.getChildren().add(Comp.separator().hide(empty).createRegion());

        var tile = new TileButtonComp("restore", "restoreAllSessions", "mdmz-restore", actionEvent -> {
                    model.restoreState(state);
                    actionEvent.consume();
                })
                .grow(true, false)
                .hide(empty)
                .accessibleTextKey("restoreAllSessions");
        layout.getChildren().add(tile.createRegion());
        return layout;
    }

    private Comp<?> entryButton(BrowserSavedState.Entry e, BooleanProperty disable) {
        var entry = DataStorage.get().getStoreEntryIfPresent(e.getUuid());
        var graphic =
                entry.get().getProvider().getDisplayIconFileName(entry.get().getStore());
        var view = PrettyImageHelper.ofFixedSize(graphic, 30, 24);
        return new ButtonComp(
                        new SimpleStringProperty(DataStorage.get().getStoreDisplayName(entry.get())),
                        view.createRegion(),
                        () -> {
                            ThreadHelper.runAsync(() -> {
                                var storageEntry = DataStorage.get().getStoreEntryIfPresent(e.getUuid());
                                if (storageEntry.isPresent()) {
                                    model.openFileSystemAsync(storageEntry.get().ref(), null, disable);
                                }
                            });
                        })
                .minWidth(250)
                .accessibleText(DataStorage.get().getStoreDisplayName(entry.get()))
                .disable(disable)
                .styleClass("entry-button")
                .styleClass(Styles.LEFT_PILL)
                .apply(struc -> struc.get().setAlignment(Pos.CENTER_LEFT));
    }

    private Comp<?> dirButton(BrowserSavedState.Entry e, BooleanProperty disable) {
        return new ButtonComp(new SimpleStringProperty(e.getPath()), null, () -> {
                    ThreadHelper.runAsync(() -> {
                        model.restoreStateAsync(e, disable);
                    });
                })
                .accessibleText(e.getPath())
                .disable(disable)
                .styleClass("directory-button")
                .apply(struc -> struc.get().setMaxWidth(2000))
                .styleClass(Styles.RIGHT_PILL)
                .grow(true, false)
                .apply(struc -> struc.get().setAlignment(Pos.CENTER_LEFT));
    }
}
