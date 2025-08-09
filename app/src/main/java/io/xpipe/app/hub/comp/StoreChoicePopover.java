package io.xpipe.app.hub.comp;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.*;
import io.xpipe.app.core.AppFontSizes;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.window.AppDialog;
import io.xpipe.app.ext.DataStore;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.BindingsHelper;
import io.xpipe.app.util.DataStoreCategoryChoiceComp;
import io.xpipe.app.util.LabelGraphic;

import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.MenuButton;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import atlantafx.base.controls.Popover;
import atlantafx.base.theme.Styles;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

@RequiredArgsConstructor
public class StoreChoicePopover<T extends DataStore> {

    private final DataStoreEntry self;
    private final Property<DataStoreEntryRef<T>> selected;
    private final Class<?> storeClass;
    private final Predicate<DataStoreEntryRef<T>> applicableCheck;
    private final StoreCategoryWrapper initialCategory;
    private final String titleKey;
    private Popover popover;

    public void show(Node node) {
        var p = getPopover();
        if (!p.isShowing()) {
            p.show(node);
        } else {
            p.hide();
        }
    }

    public void hide() {
        if (popover != null) {
            popover.hide();
        }
    }

    public Popover getPopover() {
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

                if (self != null
                        && (e.equals(self)
                                || DataStorage.get().getStoreParentHierarchy(e).contains(self))) {
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

            var enabled = popover.showingProperty();
            var section = new StoreSectionMiniComp(
                    StoreSection.createTopLevel(
                            StoreViewState.get().getAllEntries(),
                            Set.of(),
                            applicable,
                            filterText,
                            selectedCategory,
                            StoreViewState.get().getEntriesListVisibilityObservable(),
                            StoreViewState.get().getEntriesListUpdateObservable(),
                            enabled),
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
            popover.setTitle(AppI18n.get(titleKey));
            AppFontSizes.xs(popover.getContentNode());

            // Hide on connection creation dialog
            AppDialog.getModalOverlays().addListener((ListChangeListener<? super ModalOverlay>) c -> {
                popover.hide();
            });
        }

        return popover;
    }
}
