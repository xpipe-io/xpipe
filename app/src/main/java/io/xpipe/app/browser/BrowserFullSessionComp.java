package io.xpipe.app.browser;

import io.xpipe.app.browser.file.BrowserConnectionListComp;
import io.xpipe.app.browser.file.BrowserConnectionListFilterComp;
import io.xpipe.app.browser.file.BrowserTransferComp;
import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.comp.base.*;
import io.xpipe.app.core.AppFontSizes;
import io.xpipe.app.core.AppLayoutModel;
import io.xpipe.app.core.window.AppMainWindow;
import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.hub.comp.StoreEntryWrapper;
import io.xpipe.app.hub.comp.StoreViewState;
import io.xpipe.app.platform.BindingsHelper;
import io.xpipe.app.platform.InputHelper;
import io.xpipe.app.platform.PlatformThread;
import io.xpipe.app.util.ObservableSubscriber;
import io.xpipe.app.util.ThreadHelper;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;

import java.util.HashMap;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class BrowserFullSessionComp extends SimpleComp {

    private final BrowserFullSessionModel model;

    public BrowserFullSessionComp(BrowserFullSessionModel model) {
        this.model = model;
    }

    @Override
    protected Region createSimple() {
        var filterTrigger = new ObservableSubscriber();
        var left = Comp.of(() -> createLeftSide(filterTrigger));

        var leftSplit = new SimpleDoubleProperty();
        var rightSplit = new SimpleDoubleProperty();
        var tabs = new BrowserSessionTabsComp(model, leftSplit, rightSplit);
        tabs.apply(struc -> {
            struc.get().setViewOrder(1);
            struc.get().setPickOnBounds(false);
            AnchorPane.setTopAnchor(struc.get(), 0.0);
            AnchorPane.setBottomAnchor(struc.get(), 0.0);
            AnchorPane.setLeftAnchor(struc.get(), 0.0);
            AnchorPane.setRightAnchor(struc.get(), 0.0);
        });

        left.apply(struc -> {
            struc.get()
                    .paddingProperty()
                    .bind(Bindings.createObjectBinding(
                            () -> new Insets(tabs.getHeaderHeight().get(), 0, 0, 0), tabs.getHeaderHeight()));
        });
        var loadingIndicator = new LoadingIconComp(model.getBusy(), AppFontSizes::xxxl)
                .apply(struc -> {
                    AnchorPane.setTopAnchor(struc.get(), 0.0);
                    AnchorPane.setRightAnchor(struc.get(), 0.0);
                })
                .styleClass("tab-loading-indicator");

        var pinnedStack = createSplitStack(rightSplit, tabs);

        var loadingStack = new AnchorComp(List.of(tabs, pinnedStack, loadingIndicator));
        loadingStack.apply(struc -> struc.get().setPickOnBounds(false));
        var delayedStack = new DelayedInitComp(
                left, () -> StoreViewState.get() != null && StoreViewState.get().isInitialized());
        delayedStack.hide(AppMainWindow.get().getStage().widthProperty().lessThan(1000));
        var splitPane = new LeftSplitPaneComp(delayedStack, loadingStack)
                .withInitialWidth(AppLayoutModel.get().getSavedState().getBrowserConnectionsWidth())
                .withOnDividerChange(d -> {
                    if (d > 0.0) {
                        AppLayoutModel.get().getSavedState().setBrowserConnectionsWidth(d);
                    }
                    leftSplit.set(d);
                });
        splitPane.apply(struc -> {
            struc.getLeft().setMinWidth(200);
            struc.getLeft().setMaxWidth(500);
            struc.get().setPickOnBounds(false);
        });

        splitPane.apply(struc -> {
            struc.get().skinProperty().subscribe(newValue -> {
                if (newValue != null) {
                    Platform.runLater(() -> {
                        struc.get().getChildrenUnmodifiable().forEach(node -> {
                            node.setClip(null);
                            node.setPickOnBounds(false);
                        });
                        struc.get().lookupAll(".split-pane-divider").forEach(node -> node.setViewOrder(-1));
                    });
                }
            });
        });
        splitPane.apply(struc -> {
            InputHelper.onKeyCombination(
                    struc.get(), new KeyCodeCombination(KeyCode.F, KeyCombination.SHORTCUT_DOWN), false, keyEvent -> {
                        filterTrigger.trigger();
                        keyEvent.consume();
                    });
        });
        splitPane.styleClass("browser");
        var r = splitPane.createRegion();
        return r;
    }

    private Region createLeftSide(ObservableSubscriber filterTrigger) {
        Predicate<StoreEntryWrapper> applicable = storeEntryWrapper -> {
            if (!storeEntryWrapper.getEntry().getValidity().isUsable()) {
                return false;
            }

            if (storeEntryWrapper.getEntry().getStore() instanceof ShellStore) {
                return true;
            }

            return storeEntryWrapper.getEntry().getProvider().launchBrowser(model, storeEntryWrapper.getEntry(), null)
                    != null;
        };
        BiConsumer<StoreEntryWrapper, BooleanProperty> action = (w, busy) -> {
            var entry = w.getEntry();
            if (!entry.getValidity().isUsable()) {
                return;
            }

            var a = entry.getProvider().launchBrowser(model, entry, busy);
            if (a != null) {
                ThreadHelper.runFailableAsync(() -> {
                    a.run();
                });
            }
        };

        var category = new SimpleObjectProperty<>(
                StoreViewState.get().getActiveCategory().getValue());
        var filter = new SimpleStringProperty();
        var bookmarkTopBar = new BrowserConnectionListFilterComp(filterTrigger, category, filter);
        var bookmarksList = new BrowserConnectionListComp(
                BindingsHelper.map(
                        model.getSelectedEntry(),
                        v -> v instanceof BrowserStoreSessionTab<?> st
                                ? st.getEntry().get()
                                : null),
                applicable,
                action,
                category,
                filter);
        var bookmarksContainer = new StackComp(List.of(bookmarksList)).styleClass("bookmarks-container");
        bookmarksContainer
                .apply(struc -> {
                    var rec = new Rectangle();
                    rec.widthProperty().bind(struc.get().widthProperty());
                    rec.heightProperty().bind(struc.get().heightProperty());
                    rec.setArcHeight(11);
                    rec.setArcWidth(11);
                    struc.get().getChildren().getFirst().setClip(rec);
                })
                .vgrow();
        var localDownloadStage = new BrowserTransferComp(model.getLocalTransfersStage())
                .hide(Bindings.createBooleanBinding(
                        () -> {
                            if (model.getSessionEntries().size() == 0) {
                                return true;
                            }

                            return false;
                        },
                        model.getSessionEntries(),
                        model.getSelectedEntry()));
        localDownloadStage.prefHeight(200);
        localDownloadStage.maxHeight(200);
        var vertical =
                new VerticalComp(List.of(bookmarkTopBar, bookmarksContainer, localDownloadStage)).styleClass("left");
        return vertical.createRegion();
    }

    private StackComp createSplitStack(SimpleDoubleProperty rightSplit, BrowserSessionTabsComp tabs) {
        var cache = new HashMap<BrowserSessionTab, Region>();
        var splitStack = new StackComp(List.of());
        splitStack.apply(struc -> struc.get().setPickOnBounds(false));
        splitStack.apply(struc -> {
            model.getEffectiveRightTab().subscribe((newValue) -> {
                PlatformThread.runLaterIfNeeded(() -> {
                    var all = model.getAllTabs();
                    cache.keySet().removeIf(browserSessionTab -> !all.contains(browserSessionTab));

                    if (newValue == null) {
                        struc.get().getChildren().clear();
                        return;
                    }

                    var cached = cache.containsKey(newValue);
                    if (!cached) {
                        cache.put(newValue, newValue.comp().createRegion());
                    }
                    var r = cache.get(newValue);
                    struc.get().getChildren().clear();
                    struc.get().getChildren().add(r);

                    struc.get().setMinWidth(rightSplit.get());
                    struc.get().setPrefWidth(rightSplit.get());
                    struc.get().setMaxWidth(rightSplit.get());
                });
            });

            rightSplit.addListener((observable, oldValue, newValue) -> {
                struc.get().setMinWidth(newValue.doubleValue());
                struc.get().setPrefWidth(newValue.doubleValue());
                struc.get().setMaxWidth(newValue.doubleValue());
            });

            var clip = new Rectangle();
            clip.widthProperty().bind(struc.get().widthProperty());
            clip.heightProperty().bind(struc.get().heightProperty());
            struc.get().setClip(clip);

            AnchorPane.setBottomAnchor(struc.get(), 0.0);
            AnchorPane.setRightAnchor(struc.get(), 0.0);
            tabs.getHeaderHeight().subscribe(number -> {
                AnchorPane.setTopAnchor(struc.get(), number.doubleValue());
            });
        });
        return splitStack;
    }
}
