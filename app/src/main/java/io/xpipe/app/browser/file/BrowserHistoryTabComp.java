package io.xpipe.app.browser.file;

import io.xpipe.app.browser.BrowserFullSessionModel;
import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.comp.base.*;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.util.DerivedObservableList;
import io.xpipe.app.util.DocumentationLink;
import io.xpipe.app.util.LabelGraphic;
import io.xpipe.app.util.ThreadHelper;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.*;

import atlantafx.base.theme.Styles;

import java.util.LinkedHashMap;
import java.util.List;

public class BrowserHistoryTabComp extends SimpleComp {

    private final BrowserFullSessionModel model;

    public BrowserHistoryTabComp(BrowserFullSessionModel model) {
        this.model = model;
    }

    @Override
    protected Region createSimple() {
        var state = BrowserHistorySavedStateImpl.get();
        var list = DerivedObservableList.wrap(state.getEntries(), true)
                .filtered(e -> {
                    if (DataStorage.get() == null) {
                        return false;
                    }

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
        var contentDisplay = createListDisplay(list);
        var emptyDisplay = createEmptyDisplay();
        var map = new LinkedHashMap<Comp<?>, ObservableValue<Boolean>>();
        map.put(emptyDisplay, empty);
        map.put(contentDisplay, empty.not());
        var stack = new MultiContentComp(map, false);
        return stack.createRegion();
    }

    private Comp<?> createListDisplay(ObservableList<BrowserHistorySavedState.Entry> list) {
        var state = BrowserHistorySavedStateImpl.get();

        var welcome = new BrowserGreetingComp();
        var header = new LabelComp(AppI18n.observable("browserWelcomeSystems"));
        var vbox = new VerticalComp(List.of(welcome, Comp.vspacer(4), header));
        vbox.apply(struc -> struc.get().setAlignment(Pos.CENTER_LEFT));

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
                });

        var tile = new TileButtonComp("restore", "restoreAllSessions", "mdmz-restore", actionEvent -> {
                    model.restoreState(state);
                    actionEvent.consume();
                })
                .grow(true, false)
                .accessibleTextKey("restoreAllSessions");

        var layout = new VerticalComp(List.of(vbox, Comp.vspacer(5), listBox, Comp.hseparator(), tile));
        layout.styleClass("welcome");
        layout.spacing(14);
        layout.maxWidth(1000);
        layout.padding(new Insets(45, 40, 40, 50));
        layout.apply(struc -> {
            struc.get().setMaxWidth(1000);
        });
        return layout;
    }

    private Comp<?> createEmptyDisplay() {
        var docs = new IntroComp("browserWelcomeDocs", new LabelGraphic.IconGraphic("mdi2b-book-open-variant"));
        docs.setButtonAction(() -> {
            DocumentationLink.GETTING_STARTED.open();
        });
        docs.setButtonDefault(true);

        var open = new IntroComp(
                "browserWelcomeEmpty",
                new LabelGraphic.CompGraphic(PrettyImageHelper.ofSpecificFixedSize("graphics/Hips.svg", 100, 122)));
        open.setButtonAction(() -> {
            BrowserFullSessionModel.DEFAULT.openFileSystemAsync(
                    DataStorage.get().local().ref(), null, null);
        });

        var v = new VerticalComp(List.of(docs, open));
        v.spacing(70);
        v.apply(struc -> struc.get().setAlignment(Pos.CENTER));
        return v;
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
                    return AppPrefs.get().censorMode().get()
                            ? "*".repeat(n.toString().length())
                            : n.toString();
                },
                AppPrefs.get().censorMode());
        return new ButtonComp(name, () -> {
                    ThreadHelper.runAsync(() -> {
                        model.restoreStateAsync(e, disable);
                    });
                })
                .accessibleText(e.getPath().toString())
                .disable(disable)
                .styleClass("directory-button")
                .apply(struc -> struc.get().setMaxWidth(20000))
                .styleClass(Styles.RIGHT_PILL)
                .hgrow()
                .apply(struc -> struc.get().setAlignment(Pos.CENTER_LEFT));
    }
}
