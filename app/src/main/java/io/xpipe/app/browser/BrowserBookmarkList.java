package io.xpipe.app.browser;

import atlantafx.base.theme.Styles;
import io.xpipe.app.comp.storage.store.StoreEntryWrapper;
import io.xpipe.app.comp.storage.store.StoreSection;
import io.xpipe.app.comp.storage.store.StoreSectionMiniComp;
import io.xpipe.app.comp.storage.store.StoreViewState;
import io.xpipe.app.core.AppFont;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.impl.FilterComp;
import io.xpipe.app.fxcomps.impl.HorizontalComp;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.util.BooleanScope;
import io.xpipe.app.util.DataStoreCategoryChoiceComp;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.FixedHierarchyStore;
import io.xpipe.core.store.ShellStore;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.css.PseudoClass;
import javafx.geometry.Point2D;
import javafx.scene.input.DragEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Predicate;

final class BrowserBookmarkList extends SimpleComp {

    private static final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");

    public static final Timer DROP_TIMER = new Timer("dnd", true);
    private Point2D lastOver = new Point2D(-1, -1);
    private TimerTask activeTask;

    private final BrowserModel model;

    BrowserBookmarkList(BrowserModel model) {
        this.model = model;
    }

    @Override
    protected Region createSimple() {
        var filterText = new SimpleStringProperty();
        var open = PlatformThread.sync(model.getSelected());
        Predicate<StoreEntryWrapper> applicable = storeEntryWrapper -> {
                    return (storeEntryWrapper.getEntry().getStore() instanceof ShellStore
                                    || storeEntryWrapper.getEntry().getStore() instanceof FixedHierarchyStore)
                            && storeEntryWrapper.getEntry().getState().isUsable();
                };
        var section = StoreSectionMiniComp.createList(
                StoreSection.createTopLevel(
                        StoreViewState.get().getAllEntries(), applicable, filterText, StoreViewState.get().getActiveCategory()),
                (s, comp) -> {
                    BooleanProperty busy = new SimpleBooleanProperty(false);
                    comp.disable(Bindings.createBooleanBinding(() -> {
                        return busy.get() || !applicable.test(s.getWrapper());
                    }, busy)).apply(struc -> {
                        open.addListener((observable, oldValue, newValue) -> {
                            struc.get()
                                    .pseudoClassStateChanged(
                                            SELECTED,
                                            newValue != null
                                                    && newValue.getStore()
                                                            .equals(s.getWrapper()
                                                                    .getEntry()
                                                                    .getStore()));
                        });
                        struc.get().setOnAction(event -> {
                            ThreadHelper.runFailableAsync(() -> {
                                var entry = s.getWrapper().getEntry();
                                if (entry.getStore() instanceof ShellStore fileSystem) {
                                    BooleanScope.execute(busy, () -> {
                                        s.getWrapper().refreshIfNeeded();
                                    });
                                    model.openFileSystemAsync(null, fileSystem, null, busy);
                                } else if (entry.getStore() instanceof FixedHierarchyStore) {
                                    BooleanScope.execute(busy, () -> {
                                        s.getWrapper().refreshWithChildren();
                                    });
                                }
                            });
                            event.consume();
                        });
                    });
                });
        var category = new DataStoreCategoryChoiceComp(StoreViewState.get().getActiveCategory())
                .styleClass(Styles.LEFT_PILL)
                .grow(false, true);
        var filter =
                new FilterComp(filterText).styleClass(Styles.RIGHT_PILL).hgrow().apply(struc -> {});

        var top = new HorizontalComp(List.of(category, filter.hgrow()))
                .styleClass("categories")
                .apply(struc -> {
                    AppFont.medium(struc.get());
                    struc.get().setFillHeight(true);
                })
                .createRegion();
        var r = section.vgrow().createRegion();
        var content = new VBox(top, r);
        content.setFillWidth(true);

        content.getStyleClass().add("bookmark-list");
        return content;
    }

    private void handleHoverTimer(DataStore store, DragEvent event) {
        if (lastOver.getX() == event.getX() && lastOver.getY() == event.getY()) {
            return;
        }

        lastOver = (new Point2D(event.getX(), event.getY()));
        activeTask = new TimerTask() {
            @Override
            public void run() {
                if (activeTask != this) {
                    return;
                }

                Platform.runLater(() -> model.openExistingFileSystemIfPresent(null, store.asNeeded()));
            }
        };
        DROP_TIMER.schedule(activeTask, 500);
    }
}
