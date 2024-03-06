package io.xpipe.app.fxcomps.impl;

import atlantafx.base.controls.Popover;
import atlantafx.base.theme.Styles;
import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.comp.store.*;
import io.xpipe.app.core.AppFont;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.DataStoreCategoryChoiceComp;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.LocalStore;
import io.xpipe.core.store.ShellStore;
import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.MenuButton;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;
import java.util.function.Predicate;

@RequiredArgsConstructor
public class DataStoreChoiceComp<T extends DataStore> extends SimpleComp {

    private final Mode mode;
    private final DataStoreEntry self;
    private final Property<DataStoreEntryRef<T>> selected;
    private final Class<T> storeClass;
    private final Predicate<DataStoreEntryRef<T>> applicableCheck;
    private final StoreCategoryWrapper initialCategory;
    private Popover popover;

    public static <T extends DataStore> DataStoreChoiceComp<T> other(
            Property<DataStoreEntryRef<T>> selected,
            Class<T> clazz,
            Predicate<DataStoreEntryRef<T>> filter,
            StoreCategoryWrapper initialCategory) {
        return new DataStoreChoiceComp<>(Mode.OTHER, null, selected, clazz, filter, initialCategory);
    }

    public static DataStoreChoiceComp<ShellStore> proxy(
            Property<DataStoreEntryRef<ShellStore>> selected, StoreCategoryWrapper initialCategory) {
        return new DataStoreChoiceComp<>(Mode.PROXY, null, selected, ShellStore.class, null, initialCategory);
    }

    public static DataStoreChoiceComp<ShellStore> host(
            Property<DataStoreEntryRef<ShellStore>> selected, StoreCategoryWrapper initialCategory) {
        return new DataStoreChoiceComp<>(Mode.HOST, null, selected, ShellStore.class, null, initialCategory);
    }

    private Popover getPopover() {
        // Rebuild popover if we have a non-null condition to allow for the content to be updated in case the condition
        // changed
        if (popover == null || applicableCheck != null) {
            var cur = StoreViewState.get().getActiveCategory().getValue();
            var selectedCategory = new SimpleObjectProperty<>(
                    initialCategory != null
                            ? (initialCategory.getRoot().equals(cur.getRoot()) ? cur : initialCategory)
                            : cur);
            var filterText = new SimpleStringProperty();
            popover = new Popover();
            Predicate<StoreEntryWrapper> applicable = storeEntryWrapper -> {
                var e = storeEntryWrapper.getEntry();

                if (e.equals(self)) {
                    return false;
                }

                // Check if load failed
                if (e.getStore() == null) {
                    return false;
                }

                return storeClass.isAssignableFrom(e.getStore().getClass())
                        && e.getValidity().isUsable()
                        && (applicableCheck == null || applicableCheck.test(e.ref()));
            };
            var section = new StoreSectionMiniComp(
                    StoreSection.createTopLevel(
                            StoreViewState.get().getAllEntries(), applicable, filterText, selectedCategory),
                    (s, comp) -> {
                        if (!applicable.test(s.getWrapper())) {
                            comp.disable(new SimpleBooleanProperty(true));
                        }
                    },
                    storeEntryWrapper -> {
                        if (applicable.test(storeEntryWrapper)) {
                            selected.setValue(storeEntryWrapper.getEntry().ref());
                            popover.hide();
                        }
                    },
                    false);
            var category = new DataStoreCategoryChoiceComp(
                            initialCategory != null ? initialCategory.getRoot() : null,
                            StoreViewState.get().getActiveCategory(),
                            selectedCategory)
                    .styleClass(Styles.LEFT_PILL);
            var filter =
                    new FilterComp(filterText).styleClass(Styles.CENTER_PILL).hgrow();

            var addButton = Comp.of(() -> {
                        MenuButton m = new MenuButton(null, new FontIcon("mdi2p-plus-box-outline"));
                        StoreCreationMenu.addButtons(m);
                        return m;
                    })
                    .accessibleTextKey("addConnection")
                    .padding(new Insets(-2))
                    .styleClass(Styles.RIGHT_PILL)
                    .grow(false, true);

            var top = new HorizontalComp(List.of(category, filter.hgrow(), addButton))
                    .styleClass("top")
                    .apply(struc -> struc.get().setFillHeight(true))
                    .apply(struc -> {
                        // Ugly solution to focus the text field
                        // Somehow this does not work through the normal on shown listeners
                        struc.get()
                                .getChildren()
                                .get(0)
                                .focusedProperty()
                                .addListener((observable, oldValue, newValue) -> {
                                    if (newValue) {
                                        ((StackPane) struc.get().getChildren().get(1))
                                                .getChildren()
                                                .get(1)
                                                .requestFocus();
                                    }
                                });
                    })
                    .createStructure()
                    .get();
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

    private String toName(DataStoreEntry entry) {
        if (entry == null) {
            return null;
        }

        if (mode == Mode.PROXY && entry.getStore() instanceof LocalStore) {
            return AppI18n.get("none");
        }

        return entry.getName();
    }

    @Override
    protected Region createSimple() {
        var button = new ButtonComp(
                Bindings.createStringBinding(
                        () -> {
                            return selected.getValue() != null
                                    ? toName(selected.getValue().getEntry())
                                    : null;
                        },
                        selected),
                () -> {});
        button.apply(struc -> {
                    struc.get().setMaxWidth(2000);
                    struc.get().setAlignment(Pos.CENTER_LEFT);
                    Comp<?> graphic = PrettyImageHelper.ofSvg(Bindings.createStringBinding(() -> {
                        if (selected.getValue() == null) {
                            return null;
                        }

                        return selected.getValue().get().getProvider().getDisplayIconFileName(selected.getValue().getStore());
                    }, selected), 16, 16);
                    struc.get()
                            .setGraphic(graphic
                                    .createRegion());
                    struc.get().setOnAction(event -> {
                        getPopover().show(struc.get());
                        event.consume();
                    });
                })
                .styleClass("choice-comp");

        var r = button.grow(true, false).accessibleText("Select connection").createRegion();
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

    public enum Mode {
        HOST,
        OTHER,
        PROXY
    }
}
