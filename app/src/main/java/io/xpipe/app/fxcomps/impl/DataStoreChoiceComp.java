package io.xpipe.app.fxcomps.impl;

import atlantafx.base.controls.Popover;
import atlantafx.base.theme.Styles;
import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.comp.storage.store.*;
import io.xpipe.app.core.AppFont;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.DataStoreProviders;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.util.DataStoreCategoryChoiceComp;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.ShellStore;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

@RequiredArgsConstructor
public class DataStoreChoiceComp<T extends DataStore> extends SimpleComp {

    public static <T extends DataStore> DataStoreChoiceComp<T> other(
            Property<T> selected, Class<T> clazz, Predicate<T> filter) {
        return new DataStoreChoiceComp<>(Mode.OTHER, null, selected, clazz, filter);
    }

    public static DataStoreChoiceComp<ShellStore> proxy(Property<ShellStore> selected) {
        return new DataStoreChoiceComp<>(Mode.PROXY, null, selected, ShellStore.class, shellStore -> true);
    }

    public static DataStoreChoiceComp<ShellStore> host(Property<ShellStore> selected) {
        return new DataStoreChoiceComp<>(Mode.HOST, null, selected, ShellStore.class, shellStore -> true);
    }

    public static DataStoreChoiceComp<ShellStore> environment(ShellStore self, Property<ShellStore> selected) {
        return new DataStoreChoiceComp<>(Mode.ENVIRONMENT, self, selected, ShellStore.class, shellStore -> true);
    }

    public static DataStoreChoiceComp<ShellStore> proxy(ShellStore self, Property<ShellStore> selected) {
        return new DataStoreChoiceComp<>(Mode.PROXY, self, selected, ShellStore.class, shellStore -> true);
    }

    public static DataStoreChoiceComp<ShellStore> host(ShellStore self, Property<ShellStore> selected) {
        return new DataStoreChoiceComp<>(Mode.HOST, self, selected, ShellStore.class, shellStore -> true);
    }

    public enum Mode {
        HOST,
        ENVIRONMENT,
        OTHER,
        PROXY
    }

    private final Mode mode;
    private final T self;
    private final Property<T> selected;
    private final Class<T> storeClass;
    private final Predicate<T> applicableCheck;

    private Popover popover;

    private Popover getPopover() {
        if (popover == null) {
            var selectedCategory = new SimpleObjectProperty<>(
                    StoreViewState.get().getActiveCategory().getValue());
            var filterText = new SimpleStringProperty();
            popover = new Popover();
            Predicate<StoreEntryWrapper> applicable = storeEntryWrapper -> {
                        var e = storeEntryWrapper.getEntry();

                if (e.getStore() == self) {
                    return false;
                }

                var store = e.getStore();
                if (!(mode == Mode.ENVIRONMENT)
                        && e.getProvider() != null
                        && !e.getProvider().canHaveSubShells()) {
                    return false;
                }

                // Check if load failed
                if (e.getStore() == null) {
                    return false;
                }
                        return storeClass.isAssignableFrom(e.getStore().getClass()) && e.getState().isUsable() && applicableCheck.test(e.getStore().asNeeded());
                    };
            var section = StoreSectionMiniComp.createList(
                    StoreSection.createTopLevel(
                            StoreViewState.get().getAllEntries(), applicable, filterText, selectedCategory),
                    (s, comp) -> {
                        comp.apply(struc -> struc.get().setOnAction(event -> {
                            selected.setValue(
                                    s.getWrapper().getEntry().getStore().asNeeded());
                            popover.hide();
                            event.consume();
                        }));

                        if (!applicable.test(s.getWrapper())) {
                            comp.disable(new SimpleBooleanProperty(true));
                        }
                    });
            var category = new DataStoreCategoryChoiceComp(selectedCategory).styleClass(Styles.LEFT_PILL);
            var filter = new FilterComp(filterText)
                    .styleClass(Styles.CENTER_PILL)
                    .hgrow()
                    .apply(struc -> {
                        popover.setOnShowing(event -> {
                            Platform.runLater(() -> {
                                Platform.runLater(() -> {
                                    Platform.runLater(() -> {
                                    struc.getText().requestFocus();
                                    });
                                });
                            });
                        });
                    });

            var addButton = Comp.of(() -> {
                MenuButton m = new MenuButton(null, new FontIcon("mdi2p-plus-box-outline"));
                StoreCreationMenu.addButtons(m);
                return m;
            }).padding(new Insets(-2)).styleClass(Styles.RIGHT_PILL).grow(false, true);

            var top = new HorizontalComp(List.of(category, filter.hgrow(), addButton))
                    .styleClass("top")
                    .apply(struc -> struc.get().setFillHeight(true))
                    .createRegion();
            var r = section.vgrow().createRegion();
            var content = new VBox(top, r);
            content.setFillWidth(true);
            content.getStyleClass().add("choice-comp-content");
            content.setPrefWidth(500);
            content.setMaxHeight(550);

            popover.setContentNode(content);
            popover.setCloseButtonEnabled(true);
            popover.setArrowLocation(Popover.ArrowLocation.TOP_CENTER);
            popover.setHeaderAlwaysVisible(true);
            popover.setDetachable(true);
            popover.setTitle(AppI18n.get("selectConnection"));
            AppFont.small(popover.getContentNode());
        }

        return popover;
    }

    protected Region createGraphic(T s) {
        var provider = DataStoreProviders.byStore(s);
        var imgView = PrettyImageHelper.ofFixedSquare(provider.getDisplayIconFileName(s), 16)
                .createRegion();

        var name = DataStorage.get().getUsableStores().stream()
                .filter(e -> e.equals(s))
                .findAny()
                .flatMap(store -> {
                    if (mode == Mode.PROXY && ShellStore.isLocal(store.asNeeded())) {
                        return Optional.of(AppI18n.get("none"));
                    }

                    return DataStorage.get().getStoreDisplayName(store);
                })
                .orElse(AppI18n.get("unknown"));

        return new Label(name, imgView);
    }

    private String toName(DataStore store) {
        if (store == null) {
            return null;
        }

        if (mode == Mode.PROXY && store instanceof ShellStore && ShellStore.isLocal(store.asNeeded())) {
            return AppI18n.get("none");
        }

        return DataStorage.get().getStoreDisplayName(store).orElse("?");
    }

    @Override
    protected Region createSimple() {
        var button = new ButtonComp(
                Bindings.createStringBinding(
                        () -> {
                            return toName(selected.getValue());
                        },
                        selected),
                () -> {});
        button.apply(struc -> {
                    struc.get().setMaxWidth(2000);
                    struc.get().setAlignment(Pos.CENTER_LEFT);
                    struc.get()
                            .setGraphic(PrettyImageHelper.ofSvg(Bindings.createStringBinding(
                                                    () -> {
                                                        if (selected.getValue() == null) {
                                                            return null;
                                                        }

                                                        return DataStorage.get()
                                                                .getStoreEntryIfPresent(selected.getValue())
                                                                .map(entry -> entry.getProvider()
                                                                        .getDisplayIconFileName(selected.getValue()))
                                                                .orElse(null);
                                                    },
                                                    selected),
                                                                16,
                                                                16)
                                    .createRegion());
                    struc.get().setOnAction(event -> {
                        getPopover().show(struc.get());
                        event.consume();
                    });
                })
                .styleClass("choice-comp");

        var r = button.grow(true, false).createRegion();
        var icon = new FontIcon("mdal-keyboard_arrow_down");
        icon.setDisable(true);
        icon.setPickOnBounds(false);
        AppFont.header(icon);
        var pane = new StackPane(r, icon);
        StackPane.setMargin(icon, new Insets(10));
        pane.setPickOnBounds(false);
        StackPane.setAlignment(icon, Pos.CENTER_RIGHT);
        pane.setMaxWidth(2000);
        r.prefWidthProperty().bind(pane.widthProperty());
        r.maxWidthProperty().bind(pane.widthProperty());
        return pane;
    }
}
