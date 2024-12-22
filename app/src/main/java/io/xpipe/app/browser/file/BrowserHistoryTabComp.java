package io.xpipe.app.browser.file;

import io.xpipe.app.browser.BrowserFullSessionModel;
import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.comp.base.HorizontalComp;
import io.xpipe.app.comp.base.LabelComp;
import io.xpipe.app.comp.base.ListBoxViewComp;
import io.xpipe.app.comp.base.PrettyImageHelper;
import io.xpipe.app.comp.base.TileButtonComp;
import io.xpipe.app.core.AppFont;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.util.BindingsHelper;
import io.xpipe.app.util.DerivedObservableList;
import io.xpipe.app.util.ThreadHelper;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import atlantafx.base.controls.Spacer;
import atlantafx.base.theme.Styles;

import java.util.List;

public class BrowserHistoryTabComp extends SimpleComp {

    private final BrowserFullSessionModel model;

    public BrowserHistoryTabComp(BrowserFullSessionModel model) {
        this.model = model;
    }

    @Override
    protected Region createSimple() {
        var state = BrowserHistorySavedStateImpl.get();

        var welcome = new BrowserGreetingComp().createSimple();

        var vbox = new VBox(welcome, new Spacer(4, Orientation.VERTICAL));
        vbox.setAlignment(Pos.CENTER_LEFT);

        var img = PrettyImageHelper.ofFixedSize("graphics/Hips.svg", 50, 61)
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
        layout.setPadding(new Insets(25, 40, 40, 40));
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
        AppFont.medium(layout);
        return layout;
    }

    private Comp<?> entryButton(BrowserHistorySavedState.Entry e, BooleanProperty disable) {
        var entry = DataStorage.get().getStoreEntryIfPresent(e.getUuid());
        var graphic = entry.get().getEffectiveIconFile();
        var view = PrettyImageHelper.ofFixedSize(graphic, 22, 16);
        var name = Bindings.createStringBinding(
                () -> {
                    var n = DataStorage.get().getStoreEntryDisplayName(entry.get());
                    return AppPrefs.get().censorMode().get() ? "*".repeat(n.length()) : n;
                },
                AppPrefs.get().censorMode());
        return new ButtonComp(name, view.createRegion(), () -> {
                    ThreadHelper.runAsync(() -> {
                        var storageEntry = DataStorage.get().getStoreEntryIfPresent(e.getUuid());
                        if (storageEntry.isPresent()) {
                            model.openFileSystemAsync(storageEntry.get().ref(), null, disable);
                        }
                    });
                })
                .minWidth(300)
                .accessibleText(DataStorage.get().getStoreEntryDisplayName(entry.get()))
                .disable(disable)
                .styleClass("entry-button")
                .styleClass(Styles.LEFT_PILL)
                .apply(struc -> struc.get().setAlignment(Pos.CENTER_LEFT));
    }

    private Comp<?> dirButton(BrowserHistorySavedState.Entry e, BooleanProperty disable) {
        var name = Bindings.createStringBinding(
                () -> {
                    var n = e.getPath();
                    return AppPrefs.get().censorMode().get() ? "*".repeat(n.length()) : n;
                },
                AppPrefs.get().censorMode());
        return new ButtonComp(name, null, () -> {
                    ThreadHelper.runAsync(() -> {
                        model.restoreStateAsync(e, disable);
                    });
                })
                .accessibleText(e.getPath())
                .disable(disable)
                .styleClass("directory-button")
                .apply(struc -> struc.get().setMaxWidth(2000))
                .styleClass(Styles.RIGHT_PILL)
                .hgrow()
                .apply(struc -> struc.get().setAlignment(Pos.CENTER_LEFT));
    }
}
