package io.xpipe.app.hub.comp;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.comp.base.*;
import io.xpipe.app.core.AppFontSizes;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.window.AppDialog;
import io.xpipe.app.ext.DataStore;
import io.xpipe.app.ext.LocalStore;
import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.BindingsHelper;
import io.xpipe.app.util.DataStoreCategoryChoiceComp;
import io.xpipe.app.util.LabelGraphic;

import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.MenuButton;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import atlantafx.base.controls.Popover;
import atlantafx.base.theme.Styles;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

@RequiredArgsConstructor
public class StoreChoiceComp<T extends DataStore> extends SimpleComp {

    private final Mode mode;
    private final DataStoreEntry self;
    private final Property<DataStoreEntryRef<T>> selected;
    private final Class<T> storeClass;
    private final Predicate<DataStoreEntryRef<T>> applicableCheck;
    private final StoreCategoryWrapper initialCategory;
    private Popover popover;

    public static <T extends DataStore> StoreChoiceComp<T> other(
            Property<DataStoreEntryRef<T>> selected,
            Class<T> clazz,
            Predicate<DataStoreEntryRef<T>> filter,
            StoreCategoryWrapper initialCategory) {
        return new StoreChoiceComp<>(Mode.OTHER, null, selected, clazz, filter, initialCategory);
    }

    public static StoreChoiceComp<ShellStore> proxy(
            Property<DataStoreEntryRef<ShellStore>> selected, StoreCategoryWrapper initialCategory) {
        return new StoreChoiceComp<>(Mode.PROXY, null, selected, ShellStore.class, null, initialCategory);
    }

    public static StoreChoiceComp<ShellStore> host(
            Property<DataStoreEntryRef<ShellStore>> selected, StoreCategoryWrapper initialCategory) {
        return new StoreChoiceComp<>(Mode.HOST, null, selected, ShellStore.class, null, initialCategory);
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

                if (e.equals(self)
                        || DataStorage.get().getStoreParentHierarchy(e).contains(self)) {
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

            var applicableMatch =
                    StoreViewState.get().getCurrentTopLevelSection().anyMatches(applicable);
            if (!applicableMatch) {
                selectedCategory.set(initialCategory);
            }

            var applicableCount = StoreViewState.get().getAllEntries().getList().stream()
                    .filter(applicable)
                    .count();
            var initialExpanded = applicableCount < 20;

            var section = new StoreSectionMiniComp(
                    StoreSection.createTopLevel(
                            StoreViewState.get().getAllEntries(),
                            Set.of(),
                            applicable,
                            filterText,
                            selectedCategory,
                            StoreViewState.get().getEntriesListVisibilityObservable(),
                            StoreViewState.get().getEntriesListUpdateObservable(),
                            popover.showingProperty()),
                    (s, comp) -> {
                        if (!applicable.test(s.getWrapper())) {
                            comp.disable(new SimpleBooleanProperty(true));
                        }
                    },
                    sec -> {
                        if (applicable.test(sec.getWrapper())) {
                            this.selected.setValue(sec.getWrapper().getEntry().ref());
                            popover.hide();
                        }
                    },
                    initialExpanded);

            var category = new DataStoreCategoryChoiceComp(
                            initialCategory != null ? initialCategory.getRoot() : null,
                            StoreViewState.get().getActiveCategory(),
                            selectedCategory)
                    .styleClass(Styles.LEFT_PILL);
            var filter =
                    new FilterComp(filterText).styleClass(Styles.CENTER_PILL).hgrow();

            var addButton = Comp.of(() -> {
                        MenuButton m = new MenuButton(null, new FontIcon("mdi2p-plus-box-outline"));
                        m.setMaxHeight(100);
                        m.setMinHeight(0);
                        StoreCreationMenu.addButtons(m, false);
                        return m;
                    })
                    .accessibleTextKey("addConnection")
                    .padding(new Insets(-5))
                    .styleClass(Styles.RIGHT_PILL);

            var top = new HorizontalComp(List.of(category, filter, addButton))
                    .styleClass("top")
                    .apply(struc -> struc.get().setFillHeight(true))
                    .apply(struc -> {
                        var first = ((Region) struc.get().getChildren().get(0));
                        var second = ((Region) struc.get().getChildren().get(1));
                        var third = ((Region) struc.get().getChildren().get(1));
                        second.prefHeightProperty().bind(first.heightProperty());
                        second.minHeightProperty().bind(first.heightProperty());
                        second.maxHeightProperty().bind(first.heightProperty());
                        third.prefHeightProperty().bind(first.heightProperty());
                    })
                    .apply(struc -> {
                        // Ugly solution to focus the text field
                        // Somehow this does not work through the normal on shown listeners
                        struc.get()
                                .getChildren()
                                .get(0)
                                .focusedProperty()
                                .addListener((observable, oldValue, newValue) -> {
                                    if (newValue) {
                                        struc.get().getChildren().get(1).requestFocus();
                                    }
                                });
                    })
                    .createStructure()
                    .get();

            var emptyText = Bindings.createStringBinding(
                    () -> {
                        var count = StoreViewState.get().getAllEntries().getList().stream()
                                .filter(applicable)
                                .count();
                        return count == 0 ? AppI18n.get("noCompatibleConnection") : null;
                    },
                    StoreViewState.get().getAllEntries().getList());
            var emptyLabel =
                    new LabelComp(emptyText, new SimpleObjectProperty<>(new LabelGraphic.IconGraphic("mdi2f-filter")));
            emptyLabel.apply(struc -> AppFontSizes.sm(struc.get()));
            emptyLabel.hide(BindingsHelper.map(emptyText, s -> s == null));
            emptyLabel.minHeight(80);

            var listStack = new StackComp(List.of(emptyLabel, section));
            listStack.vgrow();

            var r = listStack.createRegion();
            var content = new VBox(top, r);
            content.setFillWidth(true);
            content.getStyleClass().add("choice-comp-content");
            content.setPrefWidth(480);
            content.setMaxHeight(550);

            popover.setContentNode(content);
            popover.setCloseButtonEnabled(true);
            popover.setArrowLocation(Popover.ArrowLocation.TOP_CENTER);
            popover.setHeaderAlwaysVisible(true);
            popover.setDetachable(true);
            popover.setTitle(AppI18n.get("selectConnection"));
            AppFontSizes.xs(popover.getContentNode());

            // Hide on connection creation dialog
            AppDialog.getModalOverlays().addListener((ListChangeListener<? super ModalOverlay>) c -> {
                popover.hide();
            });
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
                            var val = selected.getValue();
                            return val != null ? toName(val.get()) : null;
                        },
                        selected),
                () -> {});
        button.apply(struc -> {
                    struc.get().setMaxWidth(20000);
                    struc.get().setAlignment(Pos.CENTER_LEFT);
                    Comp<?> graphic = PrettyImageHelper.ofFixedSize(
                            Bindings.createStringBinding(
                                    () -> {
                                        var val = selected.getValue();
                                        if (val == null) {
                                            return null;
                                        }

                                        return val.get().getEffectiveIconFile();
                                    },
                                    selected),
                            16,
                            16);
                    struc.get().setGraphic(graphic.createRegion());
                    struc.get().setOnAction(event -> {
                        if (popover == null || !popover.isShowing()) {
                            var p = getPopover();
                            p.show(struc.get());
                        } else {
                            popover.hide();
                        }
                        event.consume();
                    });
                    struc.get().setOnMouseClicked(event -> {
                        if (event.getButton() != MouseButton.SECONDARY) {
                            return;
                        }

                        selected.setValue(
                                mode == Mode.PROXY ? DataStorage.get().local().ref() : null);
                        event.consume();
                    });
                })
                .styleClass("choice-comp");

        var r = button.grow(true, false).accessibleText("Select connection").createRegion();
        var icon = new FontIcon("mdal-keyboard_arrow_down");
        icon.setDisable(true);
        icon.setPickOnBounds(false);
        icon.visibleProperty().bind(r.disabledProperty().not());
        AppFontSizes.xl(icon);
        var pane = new StackPane(r, icon);
        pane.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                r.requestFocus();
            }
        });
        StackPane.setMargin(icon, new Insets(10));
        pane.setPickOnBounds(false);
        StackPane.setAlignment(icon, Pos.CENTER_RIGHT);
        pane.setMaxWidth(20000);
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
